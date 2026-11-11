package com.syncflow.backend.dto;

import com.syncflow.backend.model.CardPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class CreateCardRequest {
    @NotBlank @Size(max = 255)
    private String title;
    @Size(max = 10000)
    private String description;
    private Instant dueDate;
    private CardPriority priority = CardPriority.MEDIUM;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public CardPriority getPriority() { return priority; }
    public void setPriority(CardPriority priority) { this.priority = priority; }
}
