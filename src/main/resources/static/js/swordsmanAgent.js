/**
 * Swordsman Agent client — NDJSON stream consumer.
 *
 * Animation rendering is delegated to AnimationEngine.
 * This module handles chat streaming and notifies the animation
 * engine of characterState changes from SSE events.
 */
const SwordsmanAgentClient = {
    isStreaming: false,

    init() {
        this.setupSendHandler();
    },

    /* === Message Sending === */
    setupSendHandler() {
        const btn = document.getElementById('chat-send');
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
        const input = document.getElementById('chat-input');
        const content = input.value.trim();
        if (!content || this.isStreaming) return;

        input.value = '';
        this.isStreaming = true;

        const btn = document.getElementById('chat-send');
        btn.disabled = true;

        const sessionKey = ChatHistory.getOrCreateSession();

        ChatHistory.appendMessage('user', content);
        AnimationEngine.hintAction('listening');

        const typingEl = this.addTypingIndicator();

        try {
            const response = await fetch('/api/v1/conversations/' + sessionKey + '/messages', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: content })
            });

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                Desktop.showToast(errData.message || '连接失败，请稍后再试...');
                AnimationEngine.hintAction('idle');
                this.removeTypingIndicator(typingEl);
                this.isStreaming = false;
                btn.disabled = false;
                return;
            }

            await this.handleStream(response, typingEl);
        } catch (e) {
            console.error('Send failed:', e);
            Desktop.showToast('网络连接失败，请检查网络后重试...');
            AnimationEngine.hintAction('idle');
            this.removeTypingIndicator(typingEl);
        }

        this.isStreaming = false;
        btn.disabled = false;
    },

    async handleStream(response, typingEl) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        let buffer = '';
        let assistantMsgDiv = null;
        let fullResponse = '';
        const container = document.getElementById('chat-messages');

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
                            getAssistantDiv: () => assistantMsgDiv,
                            setAssistantDiv: (d) => { assistantMsgDiv = d; },
                            appendFullResponse: (t) => { fullResponse += t; }
                        });
                    } catch (e) {
                        buffer = line + '\n' + buffer;
                    }
                }
            }
        } catch (e) {
            console.error('Stream read error:', e);
        }

        ChatHistory.loadConversationList();
    },

    handleEvent(data, ctx) {
        const { type, content, characterState, recommendations, messageId } = data;

        // Notify animation engine immediately for instant visual feedback
        if (characterState) {
            AnimationEngine.hintAction(characterState);
        }

        switch (type) {
            case 'thinking':
                if (content && ctx.typingEl) {
                    ctx.typingEl.textContent = content;
                }
                break;

            case 'text':
                if (ctx.typingEl) {
                    this.removeTypingIndicator(ctx.typingEl);
                    ctx.typingEl = null;
                }

                if (!ctx.getAssistantDiv()) {
                    const msgDiv = document.createElement('div');
                    msgDiv.className = 'message assistant';
                    ctx.container.appendChild(msgDiv);
                    ctx.setAssistantDiv(msgDiv);
                }

                if (content) {
                    ctx.getAssistantDiv().textContent += content;
                    ctx.appendFullResponse(content);
                }
                ctx.container.scrollTop = ctx.container.scrollHeight;
                break;

            case 'reference':
                if (data.references && data.references.length > 0) {
                    const refDiv = document.createElement('div');
                    refDiv.className = 'message assistant';
                    refDiv.innerHTML = data.references
                        .map(r => '<a href="' + r.url + '" target="_blank" style="color:#aac8ff">' + r.title + '</a>')
                        .join(' · ');
                    ctx.container.appendChild(refDiv);
                }
                break;

            case 'recommend':
                if (recommendations && recommendations.length > 0) {
                    const recDiv = document.createElement('div');
                    recDiv.className = 'message assistant';
                    recDiv.style.background = 'transparent';
                    recDiv.style.border = 'none';
                    recDiv.style.padding = '6px 14px';

                    recDiv.innerHTML = '<div style="font-size:11px;color:rgba(255,255,255,0.4);margin-bottom:6px">猜你想问：</div>' +
                        recommendations.map(r =>
                            '<span class="rec-chip" style="display:inline-block;padding:4px 10px;margin:2px 4px;border-radius:12px;' +
                            'background:rgba(100,140,220,0.15);cursor:pointer;font-size:12px;color:#aac8ff"' +
                            '>' + r + '</span>'
                        ).join('');

                    recDiv.querySelectorAll('.rec-chip').forEach(chip => {
                        chip.addEventListener('click', () => {
                            document.getElementById('chat-input').value = chip.textContent;
                            SwordsmanAgentClient.sendMessage();
                        });
                    });

                    ctx.container.appendChild(recDiv);
                }
                break;

            case 'complete':
                if (messageId) {
                    const div = ctx.getAssistantDiv();
                    if (div) {
                        const metaSpan = document.createElement('div');
                        metaSpan.className = 'msg-meta';
                        if (data.metadata && data.metadata.firstResponseTimeMs) {
                            metaSpan.textContent = '响应 ' + (data.metadata.firstResponseTimeMs / 1000).toFixed(1) + 's';
                        }
                        div.appendChild(metaSpan);
                    }
                }
                ChatHistory.loadConversationList();
                break;

            case 'error':
                Desktop.showToast(data.message || '服务暂时不可用，请稍后再试...');
                AnimationEngine.hintAction('idle');
                break;
        }
    },

    addTypingIndicator() {
        const container = document.getElementById('chat-messages');
        const el = document.createElement('div');
        el.className = 'typing-indicator';
        el.innerHTML = '<span class="dot"></span><span class="dot"></span><span class="dot"></span>';
        container.appendChild(el);
        container.scrollTop = container.scrollHeight;
        return el;
    },

    removeTypingIndicator(el) {
        if (el && el.parentNode) {
            el.remove();
        }
    }
};