/**
 * Chat history — conversation list CRUD, message history loading, render.
 *
 * Manages the conversation sidebar (lantern cards) and the chat message
 * panel including welcome/empty state transitions.
 */
const ChatHistory = {
    currentSessionKey: null,
    conversations: [],

    /* ================================================================
     *  Init
     * ================================================================ */

    init() {
        this.loadConversationList();
        this.setupEventHandlers();
    },

    setupEventHandlers() {
        const newBtn = document.getElementById('btn-new-conv');
        if (newBtn) {
            newBtn.addEventListener('click', () => this.createConversation());
        }
    },

    /* ================================================================
     *  Conversation CRUD
     * ================================================================ */

    async loadConversationList() {
        try {
            const resp = await fetch('/api/v1/conversations');
            const result = await resp.json();
            if (result.code === 200) {
                this.conversations = result.data || [];
                this.renderConvList();
            }
        } catch (e) {
            console.warn('Failed to load conversations:', e);
        }
    },

    async createConversation(title) {
        try {
            const resp = await fetch('/api/v1/conversations', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: title || '新たな対話' })
            });
            const result = await resp.json();
            if (result.code === 200) {
                await this.loadConversationList();
                if (result.data && result.data.sessionKey) {
                    this.switchConversation(result.data.sessionKey);
                }
            }
        } catch (e) {
            Desktop.showToast('対話の作成に失敗', 3000, 'error');
        }
    },

    async switchConversation(sessionKey) {
        this.currentSessionKey = sessionKey;
        this.renderConvList();

        const container = document.getElementById('chat-messages');
        container.innerHTML = '';

        try {
            const resp = await fetch('/api/v1/conversations/' + sessionKey + '/messages');
            const result = await resp.json();

            if (result.code === 200 && result.data && result.data.length > 0) {
                result.data.forEach(msg => {
                    this.appendMessage(msg.role, msg.content, msg.metadata);
                });
            } else {
                /* Show a minimal welcome for an empty conversation */
                this.showChatEmpty();
            }
        } catch (e) {
            console.warn('Failed to load messages:', e);
            this.showChatEmpty();
        }

        document.getElementById('chat-input').focus();
    },

    async deleteConversation(sessionKey) {
        try {
            const resp = await fetch('/api/v1/conversations/' + sessionKey, { method: 'DELETE' });
            if (resp.ok || resp.status === 204) {
                if (this.currentSessionKey === sessionKey) {
                    this.currentSessionKey = null;
                    this.showWelcome();
                }
                await this.loadConversationList();
            }
        } catch (e) {
            Desktop.showToast('削除に失敗', 3000, 'error');
        }
    },

    /* ================================================================
     *  Render — conversation list
     * ================================================================ */

    renderConvList() {
        const list  = document.getElementById('conv-list');
        const empty = document.getElementById('conv-empty');
        if (!list) return;

        list.innerHTML = '';

        if (this.conversations.length === 0) {
            if (empty) empty.style.display = 'flex';
            return;
        }

        if (empty) empty.style.display = 'none';

        this.conversations.forEach(conv => {
            const li = document.createElement('li');
            li.className = 'conv-item' + (conv.sessionKey === this.currentSessionKey ? ' active' : '');

            const titleSpan = document.createElement('span');
            titleSpan.className = 'conv-title';
            titleSpan.textContent = conv.title || '無題の対話';

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'conv-delete';
            deleteBtn.innerHTML = '&#x2715;';
            deleteBtn.title = '削除';
            deleteBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                if (confirm('この対話を削除しますか？')) {
                    this.deleteConversation(conv.sessionKey);
                }
            });

            li.appendChild(titleSpan);
            li.appendChild(deleteBtn);
            li.addEventListener('click', () => this.switchConversation(conv.sessionKey));
            list.appendChild(li);
        });
    },

    /* ================================================================
     *  Render — messages
     * ================================================================ */

    appendMessage(role, content, metadata) {
        const container = document.getElementById('chat-messages');
        /* Clear welcome/empty state on first message */
        const welcome = container.querySelector('.welcome-state');
        const emptySt = container.querySelector('.chat-empty-state');
        if (welcome) welcome.remove();
        if (emptySt) emptySt.remove();

        const msgDiv = document.createElement('div');
        msgDiv.className = 'message ' + role;
        msgDiv.textContent = content;

        /* Metadata — response time */
        if (metadata) {
            const metaDiv = document.createElement('div');
            metaDiv.className = 'msg-meta';
            try {
                const m = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;
                if (m && m.firstResponseTimeMs) {
                    const seconds = (m.firstResponseTimeMs / 1000).toFixed(1);
                    metaDiv.textContent = '応答 ' + seconds + 's';
                }
            } catch (_) { /* ignore malformed metadata */ }
            msgDiv.appendChild(metaDiv);
        }

        container.appendChild(msgDiv);
        container.scrollTop = container.scrollHeight;
        return msgDiv;
    },

    /* ================================================================
     *  Welcome / Empty states
     * ================================================================ */

    showWelcome() {
        const container = document.getElementById('chat-messages');
        container.innerHTML = `
            <div class="welcome-state">
                <div class="welcome-seal">剣</div>
                <p class="welcome-haiku">風の音に<br>剣の心を<br>問いかける</p>
                <p class="welcome-sub">向剑客提问，他会以武士之道回应</p>
            </div>`;
    },

    showChatEmpty() {
        const container = document.getElementById('chat-messages');
        container.innerHTML = `
            <div class="welcome-state" style="opacity:0.6;">
                <div class="welcome-seal" style="font-size:36px;">話</div>
                <p class="welcome-sub">対話を始めましょう</p>
            </div>`;
    },

    /* ================================================================
     *  Session management
     * ================================================================ */

    getOrCreateSession() {
        if (this.currentSessionKey) return this.currentSessionKey;

        const uuid = crypto.randomUUID ? crypto.randomUUID() :
            'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
                const r = Math.random() * 16 | 0;
                return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            });

        this.currentSessionKey = uuid;
        return uuid;
    }
};
