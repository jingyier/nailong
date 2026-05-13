package cn.jingyier.nail.nailong.controller;

import cn.jingyier.nail.nailong.animation.AnimationPlayer;
import cn.jingyier.nail.nailong.animation.AnimationState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the authoritative animation state so the frontend can poll it.
 *
 * GET /api/v1/animation/state → current action, frame index, frame path, etc.
 */
@RestController
public class AnimationController {

    private final AnimationPlayer player;

    public AnimationController(AnimationPlayer player) {
        this.player = player;
    }

    @GetMapping("/api/v1/animation/state")
    public AnimationState getState() {
        return player.getState();
    }
}