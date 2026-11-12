package com.syncflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {
    @NotBlank @Size(max = 10000)
    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
