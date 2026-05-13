package cn.jingyier.nail.nailong.animation;

/**
 * AI semantic state — describes what the chatbot is doing.
 * This is NOT an animation state; it drives animation selection
 * through a mapping table ({@link ChatToAnimationMapping}).
 */
public enum ChatState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    SLEEPING
}