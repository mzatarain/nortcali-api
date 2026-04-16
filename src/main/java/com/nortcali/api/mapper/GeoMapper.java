package com.nortcali.api.mapper;

import com.nortcali.api.dto.request.CityRequest;
import com.nortcali.api.dto.request.CountryRequest;
import com.nortcali.api.dto.request.StateRequest;
import com.nortcali.api.dto.response.CityResponse;
import com.nortcali.api.dto.response.CountryResponse;
import com.nortcali.api.dto.response.StateResponse;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Country;
import com.nortcali.api.entity.State;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GeoMapper {

    CountryResponse toCountryResponse(Country entity);

    @Mapping(target = "id", ignore = true)
    Country toCountryEntity(CountryRequest request);

    @Mapping(target = "id", ignore = true)
    void updateCountry(CountryRequest request, @MappingTarget Country entity);

    @Mapping(source = "country.id", target = "countryId")
    @Mapping(source = "country.name", target = "countryName")
    StateResponse toStateResponse(State entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "country", ignore = true)
    State toStateEntity(StateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "country", ignore = true)
    void updateState(StateRequest request, @MappingTarget State entity);

    @Mapping(source = "state.id", target = "stateId")
    @Mapping(source = "state.name", target = "stateName")
    CityResponse toCityResponse(City entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    City toCityEntity(CityRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    void updateCity(CityRequest request, @MappingTarget City entity);
}
