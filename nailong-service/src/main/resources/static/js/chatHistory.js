/**
 * Chat history — conversation list CRUD, message history loading, render.
 *
 * Manages the conversation sidebar and the chat message panel
 * including welcome/empty state transitions.
 */
const ChatHistory = {
    currentSessionKey: null,
    conversations: [],

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
                body: JSON.stringify({ title: title || '新对话' })
            });
            const result = await resp.json();
            if (result.code === 200) {
                await this.loadConversationList();
                if (result.data && result.data.sessionKey) {
                    this.switchConversation(result.data.sessionKey);
                }
            }
        } catch (e) {
            Desktop.showToast('创建对话失败', 3000, 'error');
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
            Desktop.showToast('删除失败', 3000, 'error');
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
            titleSpan.textContent = conv.title || '无题对话';

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'conv-delete';
            deleteBtn.innerHTML = '&#x2715;';
            deleteBtn.title = '删除';
            deleteBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                if (confirm('确定删除此对话？')) {
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
        const welcome = container.querySelector('.welcome-state');
        const emptySt = container.querySelector('.chat-empty-state');
        if (welcome) welcome.remove();
        if (emptySt) emptySt.remove();

        const msgDiv = document.createElement('div');
        msgDiv.className = 'message ' + role;
        msgDiv.textContent = content;

        if (metadata) {
            const metaDiv = document.createElement('div');
            metaDiv.className = 'msg-meta';
            try {
                const m = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;
                if (m && m.firstResponseTimeMs) {
                    metaDiv.textContent = '应答 ' + (m.firstResponseTimeMs / 1000).toFixed(1) + 's';
                }
            } catch (_) { /* ignore */ }
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
                <div class="welcome-seal">劍</div>
                <p class="welcome-haiku">一壶浊酒喜相逢<br>古今多少事<br>都付笑谈中</p>
                <p class="welcome-sub">有请剑客出招，以武论道，以心问剑</p>
            </div>`;
    },

    showChatEmpty() {
        const container = document.getElementById('chat-messages');
        container.innerHTML = `
            <div class="welcome-state" style="opacity:0.6;">
                <div class="welcome-seal" style="font-size:36px;">話</div>
                <p class="welcome-sub">开始对话吧</p>
            </div>`;
    },

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
