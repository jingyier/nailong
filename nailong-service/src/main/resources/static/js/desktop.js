/**
 * Desktop — window manager, particle system, character stage, toasts.
 *
 * "江湖客栈" aesthetic — sunlit dust motes drifting through
 * a warm bluestone courtyard, smooth drag, layered z-index.
 */
const Desktop = {
    zIndexCounter: 10,
    stageEl: null,
    charEl: null,

    init() {
        this.stageEl = document.getElementById('character-stage');
        this.charEl  = document.getElementById('character');

        this.initParticles();
        this.initWindows();
        this.initStage();
        this.positionStage();
    },

    /* ================================================================
     *  Particle system — sunlit dust motes in a warm courtyard
     * ================================================================ */

    initParticles() {
        const canvas = document.getElementById('particles');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const particles = [];
        const MAX = 55;

        function resize() {
            canvas.width  = window.innerWidth;
            canvas.height = window.innerHeight;
        }
        resize();
        window.addEventListener('resize', resize);

        for (let i = 0; i < MAX; i++) {
            particles.push({
                x:  Math.random() * canvas.width,
                y:  Math.random() * canvas.height,
                r:  Math.random() * 1.8 + 0.5,
                vx: (Math.random() - 0.5) * 0.25,
                vy: (Math.random() - 0.3) * 0.45,
                alpha:     Math.random() * 0.45 + 0.08,
                alphaBase: Math.random() * 0.45 + 0.08,
                alphaFreq: Math.random() * 0.015 + 0.004,
                phase:     Math.random() * Math.PI * 2,
                hue: Math.random() * 20 + 38
            });
        }

        function animate() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            for (let i = 0; i < particles.length; i++) {
                const p = particles[i];

                p.x += p.vx;
                p.y += p.vy;

                p.alpha = p.alphaBase + Math.sin(Date.now() * p.alphaFreq + p.phase) * 0.1;
                p.alpha = Math.max(0.03, Math.min(0.6, p.alpha));

                if (p.x < -10) p.x = canvas.width + 10;
                if (p.x > canvas.width + 10) p.x = -10;
                if (p.y < -10) p.y = canvas.height + 10;
                if (p.y > canvas.height + 10) { p.y = -10; p.x = Math.random() * canvas.width; }

                /* Warm golden dust */
                ctx.beginPath();
                ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
                ctx.fillStyle = `hsla(${p.hue}, 45%, 68%, ${p.alpha})`;
                ctx.fill();

                /* Outer glow for larger motes */
                if (p.r > 1) {
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, p.r * 3, 0, Math.PI * 2);
                    ctx.fillStyle = `hsla(${p.hue}, 40%, 75%, ${p.alpha * 0.12})`;
                    ctx.fill();
                }
            }

            requestAnimationFrame(animate);
        }
        animate();
    },

    /* ================================================================
     *  Window management
     * ================================================================ */

    initWindows() {
        document.querySelectorAll('.window').forEach(win => {
            this.makeDraggable(win);
            this.bringToFront(win);
            win.addEventListener('mousedown', () => this.bringToFront(win));
        });

        document.querySelectorAll('.win-close').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const win = btn.closest('.window');
                win.style.animation = 'none';
                win.style.opacity = '0';
                win.style.transform = 'scale(0.95)';
                win.style.transition = 'opacity 200ms ease, transform 200ms ease';
                setTimeout(() => { win.style.display = 'none'; }, 200);
            });
        });

        document.querySelectorAll('.win-min').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const body = btn.closest('.window').querySelector('.window-body');
                const isHidden = body.style.display === 'none';
                body.style.display = isHidden ? '' : 'none';
                body.style.animation = isHidden ? 'windowReveal 280ms cubic-bezier(0.16,1,0.3,1)' : 'none';
            });
        });
    },

    makeDraggable(win) {
        const header = win.querySelector('.window-header');
        if (!header) return;

        let offsetX = 0, offsetY = 0, dragging = false;

        header.addEventListener('mousedown', (e) => {
            if (e.target.closest('.win-btn')) return;
            dragging = true;
            offsetX = e.clientX - win.offsetLeft;
            offsetY = e.clientY - win.offsetTop;
            win.style.transition = 'none';
            win.style.animation = 'none';
            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
        });

        const onMove = (e) => {
            if (!dragging) return;
            win.style.left = this.clampX(e.clientX - offsetX, win) + 'px';
            win.style.top  = this.clampY(e.clientY - offsetY, win) + 'px';
        };

        const onUp = () => {
            dragging = false;
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            win.style.transition = '';
        };
    },

    bringToFront(win) {
        win.style.zIndex = ++this.zIndexCounter;
    },

    clampX(x, win) {
        return Math.max(-20, Math.min(window.innerWidth - win.offsetWidth + 20, x));
    },
    clampY(y, win) {
        return Math.max(-10, Math.min(window.innerHeight - win.offsetHeight + 10, y));
    },

    /* ================================================================
     *  Character Stage — drag & position
     * ================================================================ */

    initStage() {
        const stage = this.stageEl;
        if (!stage) return;

        let offsetX = 0, offsetY = 0;

        stage.addEventListener('mousedown', (e) => {
            stage.classList.add('dragging');
            offsetX = e.clientX - stage.offsetLeft;
            offsetY = e.clientY - stage.offsetTop;
            stage.style.transition = 'none';
            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
        });

        const onMove = (e) => {
            const maxX = window.innerWidth - stage.offsetWidth;
            const maxY = window.innerHeight - stage.offsetHeight;
            stage.style.left = Math.max(0, Math.min(maxX, e.clientX - offsetX)) + 'px';
            stage.style.top  = Math.max(0, Math.min(maxY, e.clientY - offsetY)) + 'px';
            stage.style.transform = 'none';
        };

        const onUp = () => {
            stage.classList.remove('dragging');
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            stage.style.transition = '';
        };

        stage.addEventListener('dblclick', () => {
            const chatWin = document.getElementById('chat-window');
            if (chatWin) {
                const visible = chatWin.style.display !== 'none';
                if (visible) {
                    chatWin.style.opacity = '0';
                    chatWin.style.transform = 'scale(0.95)';
                    chatWin.style.transition = 'opacity 200ms ease, transform 200ms ease';
                    setTimeout(() => { chatWin.style.display = 'none'; }, 200);
                } else {
                    chatWin.style.display = '';
                    chatWin.style.opacity = '1';
                    chatWin.style.transform = 'scale(1)';
                    chatWin.style.transition = 'opacity 200ms ease, transform 200ms ease';
                    this.bringToFront(chatWin);
                }
            }
        });
    },

    positionStage() {
        const stage = this.stageEl;
        if (!stage) return;

        const maxX = window.innerWidth - 240;
        const maxY = window.innerHeight - 320;
        const x = maxX * 0.55 + Math.random() * maxX * 0.25;
        const y = maxY * 0.35 + Math.random() * maxY * 0.3;

        stage.style.left = x + 'px';
        stage.style.top  = y + 'px';
        stage.style.transform = 'none';
    },

    /* ================================================================
     *  Toast notifications
     * ================================================================ */

    showToast(message, duration = 3200, type = 'error') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = 'toast ' + type;
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) toast.remove();
        }, duration);
    }
};
