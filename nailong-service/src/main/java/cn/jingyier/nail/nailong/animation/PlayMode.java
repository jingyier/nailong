package cn.jingyier.nail.nailong.animation;

public enum PlayMode {
    /** Cycle from frame 0 → N-1 → 0 … */
    LOOP,
    /** Play 0 → N-1 once, stay on last frame */
    PLAY_ONCE,
    /** Cycle 0 → N-1 → N-2 → … → 1 → 0 … */
    PING_PONG
}