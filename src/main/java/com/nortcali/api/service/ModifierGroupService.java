package com.nortcali.api.service;

import com.nortcali.api.dto.request.ModifierGroupRequest;
import com.nortcali.api.dto.request.ModifierRequest;
import com.nortcali.api.dto.response.ModifierGroupResponse;
import com.nortcali.api.dto.response.ModifierResponse;

import java.util.List;

public interface ModifierGroupService {

    List<ModifierGroupResponse> getByRestaurant(Long restaurantId);
    ModifierGroupResponse create(Long restaurantId, ModifierGroupRequest request);
    ModifierGroupResponse update(Long groupId, ModifierGroupRequest request);
    void delete(Long groupId);

    List<ModifierResponse> getModifiers(Long groupId);
    ModifierResponse addModifier(Long groupId, ModifierRequest request);
    ModifierResponse updateModifier(Long modifierId, ModifierRequest request);
    void deleteModifier(Long modifierId);
}
