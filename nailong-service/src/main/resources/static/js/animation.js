/**
 * Animation engine — receives authoritative animation state via SSE
 * (with polling fallback), renders PNG sequence frames, manages
 * preloading, updates nameplate.
 *
 * Architecture:
 *   SSE (primary) / Polling (fallback, 80ms) → StateSync → RenderLayer + NameplateLayer
 *
 * Backend is authoritative for both action AND frame index.
 */
const AnimationEngine = (() => {

    let currentAction   = 'IDLE';
    let frameIndex      = 0;
    let frameCount      = 4;
    let playMode        = 'LOOP';
    let finished         = false;
    let lastFramePath   = '';

    let charImg      = null;
    let nameplateEl  = null;
    let stateTextEl  = null;

    let eventSource  = null;
    let pollTimer    = null;
    const POLL_MS    = 80;

    const preloadCache = {};

    /* Chinese martial arts state labels */
    const STATE_LABELS = {
        IDLE:      '待机',
        WALK:      '移步',
        COMBO:     '连招',
        JUMP:      '纵跃',
        FALL:      '坠落',
        LAND:      '落地',
        DEATH:     '落败',
        PAIN:      '受创',
        ATTACK:    '出招',
        LISTENING: '倾听',
        THINKING:  '思虑',
        SPEAKING:  '言说',
        ERROR:     '困惑',
        SLEEPING:  '休眠'
    };

    /* ===========================================================
     *  Public API
     * =========================================================== */

    function init() {
        const charEl = document.getElementById('character');
        if (!charEl) return;

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

        stateTextEl = document.querySelector('.nameplate-state');
        nameplateEl = document.getElementById('character-nameplate');

        connectSSE();
    }

    function connectSSE() {
        if (eventSource) {
            eventSource.close();
        }

        eventSource = new EventSource('/api/v1/animation/stream');

        eventSource.addEventListener('state', function(e) {
            try {
                const s = JSON.parse(e.data);
                applyState(s);
            } catch (_) {}
        });

        eventSource.onerror = function() {
            eventSource.close();
            eventSource = null;
            startPollingFallback();
        };
    }

    function startPollingFallback() {
        if (pollTimer) return;
        fetchState();
        pollTimer = setInterval(fetchState, POLL_MS);
    }

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
        if (eventSource) { eventSource.close(); eventSource = null; }
        if (pollTimer)  { clearInterval(pollTimer); pollTimer = null; }
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
            /* keep last frame on network glitch */
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

            stateTextEl.style.color = 'var(--cinnabar)';
            stateTextEl.style.transition = 'none';
            requestAnimationFrame(() => {
                stateTextEl.style.transition = 'color 600ms ease';
                stateTextEl.style.color = 'var(--text-ink-muted)';
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

    function getAction()    { return currentAction; }
    function getFrameIdx()  { return frameIndex; }
    function getFrameCount(){ return frameCount; }
    function isFinished()   { return finished; }

    function getStateLabel(action) {
        return STATE_LABELS[action] || action;
    }

    return {
        init, destroy, hintAction,
        getAction, getFrameIdx, getFrameCount, isFinished,
        getStateLabel
    };
})();
