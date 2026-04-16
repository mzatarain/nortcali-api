package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;
import com.nortcali.api.entity.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    UnitResponse toResponse(Unit entity);

    Unit toEntity(UnitRequest request);

    void updateEntity(UnitRequest request, @MappingTarget Unit entity);
}
