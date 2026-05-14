/**
 * Animation engine — polls backend for authoritative animation state,
 * renders PNG sequence frames, handles action switching and preloading.
 *
 * Architecture:
 *   PollingLayer → StateSync → RenderLayer
 *
 * Backend is authoritative for both action AND frame index.
 * Polling at ~80ms keeps the frontend in lockstep.
 */
const AnimationEngine = (() => {

    /* ---- mutable state (mirrors backend) ---- */
    let currentAction   = 'IDLE';
    let frameIndex      = 0;
    let frameCount      = 4;
    let playMode        = 'LOOP';
    let finished        = false;
    let lastFramePath   = '';

    /* ---- DOM ---- */
    let charImg = null;

    /* ---- polling ---- */
    let pollTimer  = null;
    const POLL_MS  = 80;

    /* ---- preload cache ---- */
    const preloadCache = {};

    /* ===========================================================
     *  Public API
     * =========================================================== */

    function init() {
        const charEl = document.getElementById('character');
        if (!charEl) return;

        // Replace background-image div with <img> for proper PNG rendering
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

        fetchState();          // immediate
        pollTimer = setInterval(fetchState, POLL_MS);
    }

    /**
     * Called by swordsmanAgent.js when a characterState arrives in an SSE event.
     * Provides instant feedback before the next poll cycle.
     */
    function hintAction(actionName) {
        if (!actionName) return;
        const upper = actionName.toUpperCase();
        if (upper !== currentAction) {
            frameIndex = 0;
            currentAction = upper;
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
            if (charImg) charImg.src = path;
        }

        if (actionChanged) {
            preloadAction(s.action, frameCount, path);
        }
    }

    /* ===========================================================
     *  Preloading
     * =========================================================== */

    function preloadAction(actionName, count, samplePath) {
        const match = samplePath.match(/^(.+\/samurai)(\d+)\.png$/);
        if (!match) return;

        const prefix = match[1];         // e.g. "/img/swordman/action3/samurai"
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

    return { init, destroy, hintAction, getAction, getFrameIdx, getFrameCount, isFinished };
})();