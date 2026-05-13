/**
 * NaiLong Agent client — NDJSON stream consumer + character animation engine
 *
 * Character states: idle | listening | thinking | speaking | laughing | sleepy
 * Character images are in /img/nailong/{state}/01.png
 */

const NaiLongAgentClient = {
    currentState: 'idle',
    frameIndex: 0,
    frameInterval: 500,
    frameTimer: null,
    charEl: null,
    isStreaming: false,

    stateIntervals: {
        idle: 500,
        listening: 350,
        thinking: 200,
        speaking: 250,
        laughing: 150,
        sleepy: 800
    },

    init() {
        this.charEl = document.getElementById('character');
        this.setCharacterState('idle');
        this.startFrameEngine();
        this.setupSendHandler();
    },

    /* === Frame Animation Engine === */
    startFrameEngine() {
        const tick = () => {
            if (!this.charEl) return;
            const src = '/img/nailong/' + this.currentState + '/01.png';
            this.charEl.style.backgroundImage = 'url(' + src + '?t=' + Date.now() + ')';
            this.frameIndex++;
            this.frameTimer = setTimeout(tick, this.frameInterval);
        };
        tick();
    },

    setCharacterState(state) {
        if (this.currentState === state) return;

        this.charEl.classList.remove('idle', 'listening', 'thinking', 'speaking', 'laughing', 'sleepy');
        this.charEl.classList.add(state);

        this.currentState = state;
        this.frameInterval = this.stateIntervals[state] || 500;
        this.frameIndex = 0;

        this.charEl.style.backgroundImage = 'url(/img/nailong/' + state + '/01.png)';
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
        this.setCharacterState('listening');

        const typingEl = this.addTypingIndicator();

        try {
            const response = await fetch('/api/v1/conversations/' + sessionKey + '/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ content: content })
            });

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                Desktop.showToast(errData.message || '奶龙掉线了...');
                this.setCharacterState('idle');
                this.removeTypingIndicator(typingEl);
                this.isStreaming = false;
                btn.disabled = false;
                return;
            }

            await this.handleStream(response, typingEl);
        } catch (e) {
            console.error('Send failed:', e);
            Desktop.showToast('网络连接失败，奶龙掉线了...');
            this.setCharacterState('idle');
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
                        // Incomplete JSON chunk, put it back
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

        if (characterState) {
            this.setCharacterState(characterState);
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
                            NaiLongAgentClient.sendMessage();
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
                Desktop.showToast(data.message || '奶龙遇到了一点小问题...');
                this.setCharacterState('idle');
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