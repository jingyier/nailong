package cn.jingyier.nail.nailong.event;

public class MessageSavedEvent {

    private final Long conversationId;
    private final Long messageId;
    private final String role;

    public MessageSavedEvent(Long conversationId, Long messageId, String role) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.role = role;
    }

    public Long getConversationId() { return conversationId; }
    public Long getMessageId() { return messageId; }
    public String getRole() { return role; }
}