/**
 * Chat history — conversation list, history loading, render
 */
const ChatHistory = {
    currentSessionKey: null,
    conversations: [],

    init() {
        this.loadConversationList();
        this.setupEventHandlers();
    },

    setupEventHandlers() {
        document.getElementById('btn-new-conv').addEventListener('click', () => {
            this.createConversation();
        });
    },

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
                this.switchConversation(result.data.sessionKey);
            }
        } catch (e) {
            Desktop.showToast('创建对话失败');
        }
    },

    async switchConversation(sessionKey) {
        this.currentSessionKey = sessionKey;
        this.renderConvList();

        // Load history
        try {
            const resp = await fetch('/api/v1/conversations/' + sessionKey + '/messages');
            const result = await resp.json();

            const container = document.getElementById('chat-messages');
            container.innerHTML = '';

            if (result.code === 200 && result.data) {
                result.data.forEach(msg => {
                    this.appendMessage(msg.role, msg.content, msg.metadata);
                });
            }
        } catch (e) {
            console.warn('Failed to load messages:', e);
        }

        // Focus input
        document.getElementById('chat-input').focus();
    },

    async deleteConversation(sessionKey) {
        try {
            await fetch('/api/v1/conversations/' + sessionKey, { method: 'DELETE' });
            if (this.currentSessionKey === sessionKey) {
                this.currentSessionKey = null;
                document.getElementById('chat-messages').innerHTML = '';
            }
            await this.loadConversationList();
        } catch (e) {
            Desktop.showToast('删除失败');
        }
    },

    renderConvList() {
        const list = document.getElementById('conv-list');
        list.innerHTML = '';

        this.conversations.forEach(conv => {
            const li = document.createElement('li');
            li.className = 'conv-item' + (conv.sessionKey === this.currentSessionKey ? ' active' : '');

            const titleSpan = document.createElement('span');
            titleSpan.className = 'conv-title';
            titleSpan.textContent = conv.title;

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'conv-delete';
            deleteBtn.textContent = 'x';
            deleteBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.deleteConversation(conv.sessionKey);
            });

            li.appendChild(titleSpan);
            li.appendChild(deleteBtn);
            li.addEventListener('click', () => this.switchConversation(conv.sessionKey));
            list.appendChild(li);
        });
    },

    appendMessage(role, content, metadata) {
        const container = document.getElementById('chat-messages');
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message ' + role;
        msgDiv.textContent = content;

        if (metadata) {
            const metaDiv = document.createElement('div');
            metaDiv.className = 'msg-meta';
            try {
                const m = typeof metadata === 'string' ? JSON.parse(metadata) : metadata;
                if (m.firstResponseTimeMs) {
                    metaDiv.textContent = '响应 ' + (m.firstResponseTimeMs / 1000).toFixed(1) + 's';
                }
            } catch (e) { /* ignore */ }
            msgDiv.appendChild(metaDiv);
        }

        container.appendChild(msgDiv);
        container.scrollTop = container.scrollHeight;
        return msgDiv;
    },

    getOrCreateSession() {
        if (this.currentSessionKey) return this.currentSessionKey;
        // Generate a UUID v4 client-side
        const uuid = crypto.randomUUID ? crypto.randomUUID() :
            'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
                const r = Math.random() * 16 | 0;
                return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            });
        this.currentSessionKey = uuid;
        return uuid;
    }
};
