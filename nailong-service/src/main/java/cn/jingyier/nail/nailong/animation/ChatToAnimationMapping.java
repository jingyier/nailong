package cn.jingyier.nail.nailong.animation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless mapping table: ChatState → AnimationAction(s).
 * Uses a plain {@link EnumMap} so the compiler enforces coverage of every
 * ChatState — no branching, no default fall-through.
 *
 * A ChatState may map to multiple actions (e.g. a primary loop + a one-shot
 * on entry). The {@link AnimationPlayer} consumes the list in order.
 */
public final class ChatToAnimationMapping {

    private static final Map<ChatState, List<AnimationAction>> MAP = new EnumMap<>(ChatState.class);

    static {
        MAP.put(ChatState.IDLE,      List.of(AnimationAction.WALK));
        MAP.put(ChatState.LISTENING, List.of(AnimationAction.COMBO));
        MAP.put(ChatState.THINKING,  List.of(AnimationAction.JUMP));
        MAP.put(ChatState.SPEAKING,  List.of(AnimationAction.ATTACK));
        MAP.put(ChatState.ERROR,     List.of(AnimationAction.PAIN, AnimationAction.IDLE));
        MAP.put(ChatState.SLEEPING,  List.of(AnimationAction.FALL));
    }

    private ChatToAnimationMapping() {}

    public static List<AnimationAction> resolve(ChatState state) {
        return MAP.getOrDefault(state, List.of(AnimationAction.IDLE));
    }

    public static Map<ChatState, List<AnimationAction>> mappingTable() {
        return Collections.unmodifiableMap(MAP);
    }
}