package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;
import com.nortcali.api.entity.SalesSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SalesSourceMapper {

    @Mapping(source = "active", target = "isActive")
    SalesSourceResponse toResponse(SalesSource entity);

    SalesSource toEntity(SalesSourceRequest request);

    void updateEntity(SalesSourceRequest request, @MappingTarget SalesSource entity);
}
