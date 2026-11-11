package com.syncflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBoardListRequest {
    @NotBlank @Size(max = 255)
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
