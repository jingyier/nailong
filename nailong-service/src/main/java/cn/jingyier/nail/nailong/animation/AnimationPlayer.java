package cn.jingyier.nail.nailong.animation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authoritative animation player.
 *
 * Responsibilities:
 * - Own the current {@link AnimationAction} and frame index
 * - Advance frames on a fixed clock (independent of frontend polling)
 * - Accept {@link ChatState} changes and switch actions accordingly
 * - Expose a thread-safe snapshot for the controller layer
 *
 * Thread-safety: writes from the chat-agent thread (setChatState) and the
 * scheduler thread (tick) are guarded by atomic references / atomics.
 */
@Service
public class AnimationPlayer {

    private static final Logger log = LoggerFactory.getLogger(AnimationPlayer.class);

    /** Tick rate for the scheduler — fires every 50ms, real frame advance
     *  is gated by per-action {@link AnimationAction#getFrameIntervalMs()}. */
    private static final long TICK_MS = 50;

    private final AtomicReference<AnimationAction> currentAction = new AtomicReference<>(AnimationAction.IDLE);
    private final AtomicInteger frameIndex = new AtomicInteger(0);
    private volatile int pingPongDirection = 1;
    private volatile long lastAdvanceMs = System.currentTimeMillis();
    private volatile Instant lastChatActivity = Instant.now();
    private volatile boolean finished = false;

    /** Timestamp until which state switching is locked (minimum one cycle). */
    private volatile long switchLockedUntilMs = 0;
    /** Pending chat state to process once the current animation cycle completes. */
    private final AtomicReference<ChatState> pendingChatState = new AtomicReference<>(null);

    public AnimationState getState() {
        AnimationAction action = currentAction.get();
        int idx = frameIndex.get();
        return new AnimationState(
                action.name(),
                action.getDisplayName(),
                idx,
                action.getFrameCount(),
                action.getFramePath(idx),
                action.getPlayMode().name(),
                finished
        );
    }

    /**
     * Called by the chat agent whenever the AI changes semantic state.
     * Guarantees that the current animation plays at least one full cycle
     * before switching — unless the state is ERROR (immediate).
     * If the switch is locked, the new state is queued and processed by tick().
     */
    public void setChatState(ChatState state) {
        lastChatActivity = Instant.now();

        // ERROR always interrupts immediately
        if (state == ChatState.ERROR) {
            executeSwitch(state);
            return;
        }

        // Same chat state — no reset, preserves animation continuity
        ChatState currentChatState = inferChatState();
        if (state == currentChatState && currentChatState != ChatState.IDLE) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < switchLockedUntilMs) {
            pendingChatState.set(state);
            log.debug("Animation state {} queued (locked for {}ms more)", state, switchLockedUntilMs - now);
            return;
        }

        executeSwitch(state);
    }

    private void executeSwitch(ChatState state) {
        List<AnimationAction> actions = ChatToAnimationMapping.resolve(state);
        AnimationAction target = actions.isEmpty() ? AnimationAction.IDLE : actions.get(0);
        switchTo(target);
        switchLockedUntilMs = System.currentTimeMillis() + calculateCycleDurationMs(target);
        pendingChatState.set(null);
    }

    private long calculateCycleDurationMs(AnimationAction action) {
        int interval = action.getFrameIntervalMs();
        return switch (action.getPlayMode()) {
            case LOOP -> (long) action.getFrameCount() * interval;
            case PLAY_ONCE -> (long) action.getFrameCount() * interval;
            case PING_PONG -> (long) (2 * (action.getFrameCount() - 1)) * interval;
        };
    }

    private void switchTo(AnimationAction action) {
        AnimationAction old = currentAction.getAndSet(action);
        frameIndex.set(0);
        pingPongDirection = 1;
        finished = false;
        lastAdvanceMs = System.currentTimeMillis();
        if (!action.equals(old)) {
            log.debug("Animation switch: {} → {}", old != null ? old.name() : "null", action.name());
        }
    }

    @Scheduled(fixedDelay = 50)
    public void tick() {
        AnimationAction action = currentAction.get();
        long now = System.currentTimeMillis();
        if (now - lastAdvanceMs < action.getFrameIntervalMs()) {
            processPendingSwitch(now);
            return;
        }
        lastAdvanceMs = now;

        int count = action.getFrameCount();
        int idx = frameIndex.get();

        switch (action.getPlayMode()) {
            case LOOP -> frameIndex.set((idx + 1) % count);
            case PLAY_ONCE -> {
                if (idx < count - 1) {
                    frameIndex.set(idx + 1);
                } else {
                    finished = true;
                }
            }
            case PING_PONG -> {
                int next = idx + pingPongDirection;
                if (next >= count - 1) {
                    pingPongDirection = -1;
                } else if (next <= 0) {
                    pingPongDirection = 1;
                }
                frameIndex.set(Math.clamp(next, 0, count - 1));
            }
        }

        processPendingSwitch(now);
    }

    private void processPendingSwitch(long now) {
        ChatState pending = pendingChatState.getAndSet(null);
        if (pending != null && now >= switchLockedUntilMs) {
            executeSwitch(pending);
        }
    }

    /**
     * Called periodically (by scheduler or external trigger) to check whether
     * the chat has been idle long enough to transition into SLEEPING.
     */
    @Scheduled(fixedDelay = 30000)
    public void checkSleepTimeout() {
        ChatState currentChatState = inferChatState();
        if (currentChatState != ChatState.SLEEPING
                && Instant.now().minusSeconds(300).isAfter(lastChatActivity)) {
            setChatState(ChatState.SLEEPING);
        }
    }

    private ChatState inferChatState() {
        AnimationAction action = currentAction.get();
        // Reverse-lookup: which ChatState maps to this action?
        for (var entry : ChatToAnimationMapping.mappingTable().entrySet()) {
            if (!entry.getValue().isEmpty() && entry.getValue().get(0).equals(action)) {
                return entry.getKey();
            }
        }
        return ChatState.IDLE;
    }
}