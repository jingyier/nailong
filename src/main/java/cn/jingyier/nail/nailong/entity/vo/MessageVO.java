package cn.jingyier.nail.nailong.entity.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class MessageVO {
    private Long id;
    private String role;
    private Integer sequence;
    private String content;
    private String contentType;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public MessageVO() {}

    public MessageVO(Long id, String role, Integer sequence, String content,
                     String contentType, Map<String, Object> metadata, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.sequence = sequence;
        this.content = content;
        this.contentType = contentType;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
