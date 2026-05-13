package cn.jingyier.nail.nailong.entity.vo;

import jakarta.validation.constraints.NotBlank;

public class MessageSendRequest {
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private String contentType = "text";

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
