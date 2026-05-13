package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.RestaurantRequest;
import com.nortcali.api.dto.response.RestaurantResponse;
import com.nortcali.api.entity.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {

    @Mapping(source = "city.id", target = "cityId")
    @Mapping(source = "city.name", target = "cityName")
    @Mapping(source = "active", target = "isActive")
    RestaurantResponse toResponse(Restaurant entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "timezone",
             expression = "java(request.getTimezone() != null ? request.getTimezone() : \"America/Tijuana\")")
    Restaurant toEntity(RestaurantRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "timezone", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RestaurantRequest request, @MappingTarget Restaurant entity);
}
