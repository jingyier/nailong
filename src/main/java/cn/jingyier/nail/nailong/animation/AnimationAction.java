package cn.jingyier.nail.nailong.animation;

/**
 * Animation action (performance layer).
 * Each constant carries its own frame metadata — frame count, directory, and
 * starting frame index — so different actions with wildly different frame
 * counts (e.g. COMBO 8 vs LAND 4) coexist without special-casing.
 */
public enum AnimationAction {

    IDLE("怠速", "action1", 0, 4, PlayMode.LOOP, 200),
    WALK("步行", "action2", 15, 4, PlayMode.LOOP, 150),
    COMBO("连播", "action3", 30, 8, PlayMode.LOOP, 100),
    JUMP("跳跃", "action4", 45, 9, PlayMode.PING_PONG, 120),
    FALL("秋季", "action5", 60, 4, PlayMode.LOOP, 200),
    LAND("陆地", "action6", 75, 4, PlayMode.LOOP, 250),
    DEATH("死亡", "action7", 90, 9, PlayMode.PLAY_ONCE, 150),
    PAIN("伤痛", "action8", 105, 10, PlayMode.PLAY_ONCE, 120),
    ATTACK("攻击", "action9", 120, 15, PlayMode.LOOP, 80);

    private final String displayName;
    private final String dirName;
    private final int startFrameIndex;
    private final int frameCount;
    private final PlayMode playMode;
    private final int frameIntervalMs;

    AnimationAction(String displayName, String dirName, int startFrameIndex,
                    int frameCount, PlayMode playMode, int frameIntervalMs) {
        this.displayName = displayName;
        this.dirName = dirName;
        this.startFrameIndex = startFrameIndex;
        this.frameCount = frameCount;
        this.playMode = playMode;
        this.frameIntervalMs = frameIntervalMs;
    }

    public String getDisplayName() { return displayName; }
    public String getDirName() { return dirName; }
    public int getStartFrameIndex() { return startFrameIndex; }
    public int getFrameCount() { return frameCount; }
    public PlayMode getPlayMode() { return playMode; }
    public int getFrameIntervalMs() { return frameIntervalMs; }

    public String getFramePath(int index) {
        return "/img/swordman/" + dirName + "/samurai" + String.format("%03d", startFrameIndex + index) + ".png";
    }
}