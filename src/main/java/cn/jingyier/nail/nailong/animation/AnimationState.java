package cn.jingyier.nail.nailong.animation;

/**
 * Snapshot of the animation player's current state, returned to the frontend.
 */
public class AnimationState {

    private final String action;
    private final String actionDisplayName;
    private final int frameIndex;
    private final int frameCount;
    private final String framePath;
    private final String playMode;
    private final boolean finished;

    public AnimationState(String action, String actionDisplayName, int frameIndex,
                          int frameCount, String framePath, String playMode, boolean finished) {
        this.action = action;
        this.actionDisplayName = actionDisplayName;
        this.frameIndex = frameIndex;
        this.frameCount = frameCount;
        this.framePath = framePath;
        this.playMode = playMode;
        this.finished = finished;
    }

    public String getAction() { return action; }
    public String getActionDisplayName() { return actionDisplayName; }
    public int getFrameIndex() { return frameIndex; }
    public int getFrameCount() { return frameCount; }
    public String getFramePath() { return framePath; }
    public String getPlayMode() { return playMode; }
    public boolean isFinished() { return finished; }
}