package com.syncflow.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MoveCardRequest {
    @NotNull @PositiveOrZero
    private Long listId;
    @NotNull @PositiveOrZero @DecimalMax("1000000000000.0")
    private Double position;
    @NotNull @PositiveOrZero
    private Long version;

    public Long getListId() { return listId; }
    public void setListId(Long listId) { this.listId = listId; }
    public Double getPosition() { return position; }
    public void setPosition(Double position) { this.position = position; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
