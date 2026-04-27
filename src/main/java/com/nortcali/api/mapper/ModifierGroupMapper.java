package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.ModifierGroupResponse;
import com.nortcali.api.dto.response.ModifierResponse;
import com.nortcali.api.dto.response.VariantModifierResponse;
import com.nortcali.api.entity.Modifier;
import com.nortcali.api.entity.ModifierGroup;
import com.nortcali.api.entity.VariantModifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ModifierGroupMapper {

    @Mapping(source = "restaurant.id", target = "restaurantId")
    @Mapping(source = "active", target = "isActive")
    ModifierGroupResponse toGroupResponse(ModifierGroup entity);

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(source = "active", target = "isActive")
    ModifierResponse toModifierResponse(Modifier entity);

    @Mapping(source = "modifier.id", target = "modifierId")
    @Mapping(source = "modifier.name", target = "modifierName")
    @Mapping(source = "modifier.group.id", target = "groupId")
    @Mapping(source = "modifier.group.name", target = "groupName")
    VariantModifierResponse toVariantModifierResponse(VariantModifier entity);
}
