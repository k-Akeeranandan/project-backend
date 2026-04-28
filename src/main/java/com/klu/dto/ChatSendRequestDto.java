package com.klu.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatSendRequestDto {

    private Long recipientId;

    @NotBlank(message = "Message content is required")
    private String content;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
