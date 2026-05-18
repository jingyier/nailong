package cn.jingyier.nail.nailong.controller;

import cn.jingyier.nail.nailong.animation.AnimationPlayer;
import cn.jingyier.nail.nailong.animation.AnimationState;
import cn.jingyier.nail.nailong.animation.AnimationStateChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class AnimationController {

    private static final Logger log = LoggerFactory.getLogger(AnimationController.class);

    private final AnimationPlayer player;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public AnimationController(AnimationPlayer player) {
        this.player = player;
    }

    /**
     * Legacy polling endpoint — kept for backward compatibility.
     */
    @GetMapping("/api/v1/animation/state")
    public AnimationState getState() {
        return player.getState();
    }

    /**
     * SSE endpoint — server pushes animation state changes.
     * Frontend should prefer this over polling.
     */
    @GetMapping(value = "/api/v1/animation/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(300_000L);
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("state")
                    .data(player.getState()));
        } catch (IOException e) {
            sseEmitters.remove(emitter);
        }

        return emitter;
    }

    @EventListener
    public void onAnimationStateChanged(AnimationStateChangedEvent event) {
        AnimationState state = event.getState();
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("state")
                        .data(state));
            } catch (IOException e) {
                sseEmitters.remove(emitter);
            }
        }
    }
}