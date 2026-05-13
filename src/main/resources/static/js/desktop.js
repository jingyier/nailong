/**
 * Desktop window manager — drag, z-index, random offset
 */
const Desktop = {
    zIndexCounter: 10,
    charEl: null,

    init() {
        this.charEl = document.getElementById('character');
        this.initWindows();
        this.initCharacter();
    },

    initWindows() {
        document.querySelectorAll('.window').forEach(win => {
            this.makeDraggable(win);
            this.bringToFront(win);
            win.addEventListener('mousedown', () => this.bringToFront(win));
        });

        // Window button handlers
        document.querySelectorAll('.win-close').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                btn.closest('.window').style.display = 'none';
            });
        });

        document.querySelectorAll('.win-min').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const body = btn.closest('.window').querySelector('.window-body');
                body.style.display = body.style.display === 'none' ? '' : 'none';
            });
        });
    },

    makeDraggable(win) {
        const header = win.querySelector('.window-header');
        if (!header) return;

        let offsetX = 0, offsetY = 0;

        header.addEventListener('mousedown', (e) => {
            if (e.target.closest('.win-btn')) return; // Don't drag on buttons
            offsetX = e.clientX - win.offsetLeft;
            offsetY = e.clientY - win.offsetTop;
            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
            win.style.transition = 'none';
        });

        const onMove = (e) => {
            const x = this.clampX(e.clientX - offsetX, win);
            const y = this.clampY(e.clientY - offsetY, win);
            win.style.left = x + 'px';
            win.style.top = y + 'px';
        };

        const onUp = () => {
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            win.style.transition = '';
        };
    },

    bringToFront(win) {
        win.style.zIndex = ++this.zIndexCounter;
    },

    clampX(x, win) {
        return Math.max(0, Math.min(window.innerWidth - win.offsetWidth, x));
    },

    clampY(y, win) {
        return Math.max(0, Math.min(window.innerHeight - win.offsetHeight, y));
    },

    /* === Character === */
    initCharacter() {
        const char = this.charEl;
        if (!char) return;

        // Initial random position
        this.randomOffset();

        // Drag support
        let offsetX = 0, offsetY = 0;

        char.addEventListener('mousedown', (e) => {
            offsetX = e.clientX - char.offsetLeft;
            offsetY = e.clientY - char.offsetTop;
            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
            char.style.transition = 'none';
        });

        const onMove = (e) => {
            char.style.left = Math.max(0, Math.min(window.innerWidth - char.offsetWidth, e.clientX - offsetX)) + 'px';
            char.style.top = Math.max(0, Math.min(window.innerHeight - char.offsetHeight, e.clientY - offsetY)) + 'px';
        };

        const onUp = () => {
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            char.style.transition = '';
        };

        // Double-click to toggle chat window
        char.addEventListener('dblclick', () => {
            const chatWin = document.getElementById('chat-window');
            if (chatWin) {
                chatWin.style.display = chatWin.style.display === 'none' ? '' : 'none';
            }
        });
    },

    randomOffset() {
        const char = this.charEl;
        if (!char) return;

        const maxX = window.innerWidth - char.offsetWidth;
        const maxY = window.innerHeight - char.offsetHeight;
        const x = Math.random() * maxX * 0.6 + maxX * 0.2; // Bias toward center
        const y = Math.random() * maxY * 0.4 + maxY * 0.3;
        char.style.left = x + 'px';
        char.style.top = y + 'px';
    },

    /* Toast notification */
    showToast(message, duration = 3000) {
        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), duration);
    }
};
