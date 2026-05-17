/**
 * Animation engine — polls backend for authoritative animation state,
 * renders PNG sequence frames, manages preloading, updates nameplate.
 *
 * Architecture:
 *   PollingLayer (80ms) → StateSync → RenderLayer + NameplateLayer
 *
 * Backend is authoritative for both action AND frame index.
 */
const AnimationEngine = (() => {

    /* ---- mutable state (mirrors backend) ---- */
    let currentAction   = 'IDLE';
    let frameIndex      = 0;
    let frameCount      = 4;
    let playMode        = 'LOOP';
    let finished        = false;
    let lastFramePath   = '';

    /* ---- DOM refs ---- */
    let charImg      = null;
    let nameplateEl  = null;
    let stateTextEl  = null;

    /* ---- polling ---- */
    let pollTimer  = null;
    const POLL_MS  = 80;

    /* ---- preload cache ---- */
    const preloadCache = {};

    /* ---- state display names (Japanese aesthetic) ---- */
    const STATE_LABELS = {
        IDLE:      '待機中',
        WALK:      '歩行',
        COMBO:     '連撃',
        JUMP:      '跳躍',
        FALL:      '落下',
        LAND:      '着地',
        DEATH:     '無念',
        PAIN:      '痛手',
        ATTACK:    '斬撃',
        LISTENING: '傾聴',
        THINKING:  '思案',
        SPEAKING:  '発言',
        ERROR:     '困惑',
        SLEEPING:  '休眠'
    };

    /* ===========================================================
     *  Public API
     * =========================================================== */

    function init() {
        const charEl = document.getElementById('character');
        if (!charEl) return;

        /* Set up <img> render target */
        charEl.innerHTML = '';
        charImg = document.createElement('img');
        charImg.id = 'character-img';
        charImg.alt = 'character';
        charImg.style.width  = '100%';
        charImg.style.height = '100%';
        charImg.style.objectFit = 'contain';
        charImg.style.pointerEvents = 'none';
        charImg.draggable = false;
        charEl.appendChild(charImg);

        /* Nameplate state text element */
        stateTextEl = document.querySelector('.nameplate-state');
        nameplateEl = document.getElementById('character-nameplate');

        /* Fetch initial state immediately, then poll */
        fetchState();
        pollTimer = setInterval(fetchState, POLL_MS);
    }

    /**
     * Called by swordsmanAgent.js when a characterState arrives in an NDJSON event.
     * Provides instant feedback before the next poll cycle.
     */
    function hintAction(actionName) {
        if (!actionName) return;
        const upper = actionName.toUpperCase();
        if (upper !== currentAction) {
            frameIndex = 0;
            currentAction = upper;
            updateNameplate(upper);
        }
    }

    function destroy() {
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    }

    /* ===========================================================
     *  Polling & apply
     * =========================================================== */

    async function fetchState() {
        try {
            const resp = await fetch('/api/v1/animation/state');
            if (!resp.ok) return;
            const s = await resp.json();
            applyState(s);
        } catch (_) {
            /* network glitch — keep showing last frame */
        }
    }

    function applyState(s) {
        const actionChanged = (s.action !== currentAction);

        currentAction = s.action;
        frameIndex    = s.frameIndex;
        frameCount    = s.frameCount;
        playMode      = s.playMode;
        finished      = s.finished;

        const path = s.framePath;
        if (path && path !== lastFramePath) {
            lastFramePath = path;
            if (charImg) {
                /* Crossfade smoothness: pre-set opacity for new frames on action change */
                if (actionChanged) {
                    charImg.style.transition = 'opacity 60ms ease';
                    charImg.style.opacity = '0.85';
                    requestAnimationFrame(() => {
                        charImg.src = path;
                        charImg.style.opacity = '1';
                    });
                } else {
                    charImg.src = path;
                }
            }
        }

        if (actionChanged) {
            updateNameplate(s.action);
            preloadAction(s.action, frameCount, path);
        }
    }

    /* ===========================================================
     *  Nameplate
     * =========================================================== */

    function updateNameplate(action) {
        if (stateTextEl) {
            const label = STATE_LABELS[action] || action;
            stateTextEl.textContent = label;

            /* Flash effect on change */
            stateTextEl.style.color = 'var(--gold)';
            stateTextEl.style.transition = 'none';
            requestAnimationFrame(() => {
                stateTextEl.style.transition = 'color 600ms ease';
                stateTextEl.style.color = 'var(--text-muted)';
            });
        }
    }

    /* ===========================================================
     *  Preloading
     * =========================================================== */

    function preloadAction(actionName, count, samplePath) {
        const match = samplePath.match(/^(.+\/samurai)(\d+)\.png$/);
        if (!match) return;

        const prefix = match[1];
        const currentNum = parseInt(match[2], 10);
        const startNum = currentNum - frameIndex;

        for (let i = 0; i < count; i++) {
            const num = startNum + i;
            const url = prefix + String(num).padStart(3, '0') + '.png';
            if (!preloadCache[url]) {
                const img = new Image();
                img.src = url;
                preloadCache[url] = img;
            }
        }
    }

    /* ===========================================================
     *  Accessors (for external read)
     * =========================================================== */

    function getAction()    { return currentAction; }
    function getFrameIdx()  { return frameIndex; }
    function getFrameCount(){ return frameCount; }
    function isFinished()   { return finished; }

    /* Expose for potential nameplate access */
    function getStateLabel(action) {
        return STATE_LABELS[action] || action;
    }

    return {
        init, destroy, hintAction,
        getAction, getFrameIdx, getFrameCount, isFinished,
        getStateLabel
    };
})();
