package com.syncflow.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MoveBoardListRequest {
    @NotNull @PositiveOrZero @DecimalMax("1000000000000.0")
    private Double position;
    public Double getPosition() { return position; }
    public void setPosition(Double position) { this.position = position; }
}
