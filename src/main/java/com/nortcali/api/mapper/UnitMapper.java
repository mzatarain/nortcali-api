package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.entity.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    UnitResponse toResponse(Unit entity);

    @Mapping(target = "id", ignore = true)
    Unit toEntity(UnitRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UnitRequest request, @MappingTarget Unit entity);
}
