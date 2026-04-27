package com.nortcali.api.dto.response;

public record ModifierResponse(
        Long id,
        Long groupId,
        String groupName,
        String name,
        boolean isActive
) {}
