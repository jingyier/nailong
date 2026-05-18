package cn.jingyier.nail.nailong.animation;

public class AnimationStateChangedEvent {

    private final AnimationState state;

    public AnimationStateChangedEvent(AnimationState state) {
        this.state = state;
    }

    public AnimationState getState() { return state; }
}
