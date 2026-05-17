/**
 * Swordsman Agent client — NDJSON stream consumer.
 *
 * Handles the full message lifecycle: send → streaming receive → render.
 * Delegates animation hints to AnimationEngine.
 *
 * NDJSON event types handled:
 *   thinking   — AI is processing, with optional characterState
 *   text       — streaming text chunks, with optional characterState
 *   reference  — search result references
 *   recommend  — follow-up question suggestions
 *   complete   — stream finished, includes messageId and metadata
 *   error      — error message
 */
const SwordsmanAgentClient = {
    isStreaming: false,

    /* ================================================================
     *  Init
     * ================================================================ */

    init() {
        this.setupSendHandler();
    },

    /* ================================================================
     *  Send handler
     * ================================================================ */

    setupSendHandler() {
        const btn   = document.getElementById('chat-send');
        const input = document.getElementById('chat-input');

        btn.addEventListener('click', () => this.sendMessage());

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
    },

    async sendMessage() {
        const input   = document.getElementById('chat-input');
        const content = input.value.trim();
        if (!content || this.isStreaming) return;

        /* Clear input */
        input.value = '';
        this.isStreaming = true;

        const btn = document.getElementById('chat-send');
        btn.disabled = true;

        /* Ensure we have a session — trigger creation if needed */
        const sessionKey = ChatHistory.getOrCreateSession();

        /* Render user message */
        ChatHistory.appendMessage('user', content);

        /* Show typing indicator */
        const typingEl = this.addTypingIndicator();

        try {
            const response = await fetch('/api/v1/conversations/' + sessionKey + '/messages', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: content })
            });

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                Desktop.showToast(errData.message || '接続に失敗しました', 3500, 'error');
                this.removeTypingIndicator(typingEl);
                this.finishSend(btn);
                return;
            }

            await this.handleStream(response, typingEl);
        } catch (e) {
            console.error('Send failed:', e);
            Desktop.showToast('ネットワークエラー、再試行してください', 3500, 'error');
            this.removeTypingIndicator(typingEl);
        }

        this.finishSend(btn);
    },

    finishSend(btn) {
        this.isStreaming = false;
        btn.disabled = false;
        document.getElementById('chat-input').focus();
    },

    /* ================================================================
     *  Stream reader — NDJSON line-by-line parsing
     * ================================================================ */

    async handleStream(response, typingEl) {
        const reader  = response.body.getReader();
        const decoder = new TextDecoder();

        let buffer         = '';
        let assistantDiv   = null;
        let fullResponse   = '';
        const container    = document.getElementById('chat-messages');

        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    const trimmed = line.trim();
                    if (!trimmed) continue;

                    try {
                        const data = JSON.parse(trimmed);
                        this.handleEvent(data, {
                            container,
                            typingEl,
                            getDiv: () => assistantDiv,
                            setDiv: (d) => { assistantDiv = d; },
                            appendText: (t) => { fullResponse += t; }
                        });
                    } catch (_) {
                        /* Incomplete chunk — re-buffer */
                        buffer = line + '\n' + buffer;
                    }
                }
            }
        } catch (e) {
            console.error('Stream read error:', e);
        }

        /* Refresh conversation list to pick up updated titles/timestamps */
        ChatHistory.loadConversationList();
    },

    /* ================================================================
     *  Event dispatcher
     * ================================================================ */

    handleEvent(data, ctx) {
        const { type } = data;

        /* Forward characterState to animation engine for instant feedback */
        if (data.characterState) {
            AnimationEngine.hintAction(data.characterState);
        }

        switch (type) {
            case 'thinking':
                this.onThinking(data, ctx);
                break;
            case 'text':
                this.onText(data, ctx);
                break;
            case 'reference':
                this.onReference(data, ctx);
                break;
            case 'recommend':
                this.onRecommend(data, ctx);
                break;
            case 'complete':
                this.onComplete(data, ctx);
                break;
            case 'error':
                this.onError(data, ctx);
                break;
        }
    },

    /* ---- Event: thinking ---- */
    onThinking(data, ctx) {
        const { content } = data;
        if (content && ctx.typingEl) {
            /* Update typing text with thinking content */
            const textSpan = ctx.typingEl.querySelector('.typing-text');
            if (textSpan) {
                textSpan.textContent = content;
            } else {
                /* Add text label next to dots */
                const span = document.createElement('span');
                span.className = 'typing-text';
                span.textContent = content;
                ctx.typingEl.appendChild(span);
            }
        }
    },

    /* ---- Event: text (streaming chunks) ---- */
    onText(data, ctx) {
        const { content } = data;
        if (!content) return;

        /* Remove typing indicator on first text */
        if (ctx.typingEl) {
            this.removeTypingIndicator(ctx.typingEl);
            ctx.typingEl = null;
        }

        /* Create assistant bubble on first chunk */
        if (!ctx.getDiv()) {
            const div = document.createElement('div');
            div.className = 'message assistant streaming';
            ctx.container.appendChild(div);
            ctx.setDiv(div);
        }

        ctx.getDiv().textContent += content;
        ctx.appendText(content);
        ctx.container.scrollTop = ctx.container.scrollHeight;
    },

    /* ---- Event: reference ---- */
    onReference(data, ctx) {
        const refs = data.references;
        if (!refs || refs.length === 0) return;

        const refDiv = document.createElement('div');
        refDiv.className = 'message assistant';
        refDiv.style.background = 'transparent';
        refDiv.style.border = 'none';
        refDiv.style.padding = '4px 0';

        refDiv.innerHTML = `
            <div class="ref-card">
                ${refs.map(r => `
                    <a href="${this.escapeHtml(r.url)}" target="_blank"
                       rel="noopener noreferrer" class="ref-link">
                        ${this.escapeHtml(r.title || r.url)}
                    </a>
                `).join('')}
            </div>`;

        ctx.container.appendChild(refDiv);
        ctx.container.scrollTop = ctx.container.scrollHeight;
    },

    /* ---- Event: recommend ---- */
    onRecommend(data, ctx) {
        const recs = data.recommendations;
        if (!recs || recs.length === 0) return;

        const recDiv = document.createElement('div');
        recDiv.className = 'message assistant';
        recDiv.style.background = 'transparent';
        recDiv.style.border = 'none';
        recDiv.style.padding = '8px 0';

        recDiv.innerHTML = `
            <div class="rec-section">
                <div class="rec-label">猜你想問：</div>
                <div class="rec-chips">
                    ${recs.map(r => `
                        <span class="rec-chip">${this.escapeHtml(r)}</span>
                    `).join('')}
                </div>
            </div>`;

        /* Click-to-ask on recommendation chips */
        recDiv.querySelectorAll('.rec-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.getElementById('chat-input').value = chip.textContent;
                SwordsmanAgentClient.sendMessage();
            });
        });

        ctx.container.appendChild(recDiv);
        ctx.container.scrollTop = ctx.container.scrollHeight;
    },

    /* ---- Event: complete ---- */
    onComplete(data, ctx) {
        const div = ctx.getDiv();
        if (div) {
            /* Remove streaming indicator class */
            div.classList.remove('streaming');

            /* Attach response time metadata */
            if (data.metadata && data.metadata.firstResponseTimeMs) {
                const metaSpan = document.createElement('div');
                metaSpan.className = 'msg-meta';
                metaSpan.textContent = '応答 ' + (data.metadata.firstResponseTimeMs / 1000).toFixed(1) + 's';
                div.appendChild(metaSpan);
            }
        }
        /* Refresh conversation list for updated titles */
        ChatHistory.loadConversationList();
    },

    /* ---- Event: error ---- */
    onError(data, ctx) {
        const msg = data.message || 'サービスが一時的に利用できません';
        Desktop.showToast(msg, 4000, 'error');

        /* Remove typing indicator and partial content on error */
        if (ctx.typingEl) {
            this.removeTypingIndicator(ctx.typingEl);
            ctx.typingEl = null;
        }
    },

    /* ================================================================
     *  Typing indicator
     * ================================================================ */

    addTypingIndicator() {
        const container = document.getElementById('chat-messages');
        /* Clear welcome state if present */
        const welcome = container.querySelector('.welcome-state');
        if (welcome) welcome.remove();

        const el = document.createElement('div');
        el.className = 'typing-indicator';
        el.innerHTML = `
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>`;
        container.appendChild(el);
        container.scrollTop = container.scrollHeight;
        return el;
    },

    removeTypingIndicator(el) {
        if (el && el.parentNode) {
            el.remove();
        }
    },

    /* ================================================================
     *  Utilities
     * ================================================================ */

    escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
};
