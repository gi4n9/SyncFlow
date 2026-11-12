package com.syncflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SetCardLabelsRequest {
    @NotNull @Size(max = 20)
    private List<@Size(min = 1, max = 64) String> labels;
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
}
