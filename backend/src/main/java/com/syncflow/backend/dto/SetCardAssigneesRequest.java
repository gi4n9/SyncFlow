package com.syncflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;

public class SetCardAssigneesRequest {
    @NotNull
    private Set<@NotNull Long> userIds;
    @NotNull @PositiveOrZero
    private Long version;

    public Set<Long> getUserIds() { return userIds; }
    public void setUserIds(Set<Long> userIds) { this.userIds = userIds; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
