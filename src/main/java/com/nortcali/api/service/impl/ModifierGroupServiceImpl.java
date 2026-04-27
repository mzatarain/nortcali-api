package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.ModifierGroupRequest;
import com.nortcali.api.dto.request.ModifierRequest;
import com.nortcali.api.dto.response.ModifierGroupResponse;
import com.nortcali.api.dto.response.ModifierResponse;
import com.nortcali.api.entity.Modifier;
import com.nortcali.api.entity.ModifierGroup;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.ModifierGroupMapper;
import com.nortcali.api.repository.ModifierGroupRepository;
import com.nortcali.api.repository.ModifierRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.ModifierGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ModifierGroupServiceImpl implements ModifierGroupService {

    private final ModifierGroupRepository groupRepo;
    private final ModifierRepository modifierRepo;
    private final RestaurantRepository restaurantRepo;
    private final ModifierGroupMapper mapper;

    public ModifierGroupServiceImpl(ModifierGroupRepository groupRepo,
                                    ModifierRepository modifierRepo,
                                    RestaurantRepository restaurantRepo,
                                    ModifierGroupMapper mapper) {
        this.groupRepo = groupRepo;
        this.modifierRepo = modifierRepo;
        this.restaurantRepo = restaurantRepo;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<ModifierGroupResponse> getByRestaurant(Long restaurantId) {
        return groupRepo.findByRestaurantId(restaurantId).stream()
                .map(mapper::toGroupResponse).toList();
    }

    @Override
    public ModifierGroupResponse create(Long restaurantId, ModifierGroupRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        ModifierGroup entity = new ModifierGroup();
        entity.setName(request.getName());
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        return mapper.toGroupResponse(groupRepo.save(entity));
    }

    @Override
    public ModifierGroupResponse update(Long groupId, ModifierGroupRequest request) {
        ModifierGroup entity = findGroupOrThrow(groupId);
        entity.setName(request.getName());
        return mapper.toGroupResponse(groupRepo.save(entity));
    }

    @Override
    public void delete(Long groupId) {
        ModifierGroup entity = findGroupOrThrow(groupId);
        entity.setActive(false);
        groupRepo.save(entity);
    }

    @Override @Transactional(readOnly = true)
    public List<ModifierResponse> getModifiers(Long groupId) {
        findGroupOrThrow(groupId);
        return modifierRepo.findByGroupId(groupId).stream()
                .map(mapper::toModifierResponse).toList();
    }

    @Override
    public ModifierResponse addModifier(Long groupId, ModifierRequest request) {
        ModifierGroup group = findGroupOrThrow(groupId);
        Modifier modifier = new Modifier();
        modifier.setName(request.getName());
        modifier.setGroup(group);
        return mapper.toModifierResponse(modifierRepo.save(modifier));
    }

    @Override
    public ModifierResponse updateModifier(Long modifierId, ModifierRequest request) {
        Modifier modifier = findModifierOrThrow(modifierId);
        modifier.setName(request.getName());
        return mapper.toModifierResponse(modifierRepo.save(modifier));
    }

    @Override
    public void deleteModifier(Long modifierId) {
        Modifier modifier = findModifierOrThrow(modifierId);
        modifier.setActive(false);
        modifierRepo.save(modifier);
    }

    private ModifierGroup findGroupOrThrow(Long id) {
        return groupRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("ModifierGroup", id));
    }

    private Modifier findModifierOrThrow(Long id) {
        return modifierRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Modifier", id));
    }
}
