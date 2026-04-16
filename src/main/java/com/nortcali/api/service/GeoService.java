package com.nortcali.api.service;

import com.nortcali.api.dto.request.CityRequest;
import com.nortcali.api.dto.request.CountryRequest;
import com.nortcali.api.dto.request.StateRequest;
import com.nortcali.api.dto.response.CityResponse;
import com.nortcali.api.dto.response.CountryResponse;
import com.nortcali.api.dto.response.StateResponse;

import java.util.List;

public interface GeoService {

    // Countries
    List<CountryResponse> getAllCountries();
    CountryResponse getCountryById(Long id);
    CountryResponse createCountry(CountryRequest request);
    CountryResponse updateCountry(Long id, CountryRequest request);
    void deleteCountry(Long id);

    // States
    List<StateResponse> getAllStates();
    List<StateResponse> getStatesByCountry(Long countryId);
    StateResponse getStateById(Long id);
    StateResponse createState(StateRequest request);
    StateResponse updateState(Long id, StateRequest request);
    void deleteState(Long id);

    // Cities
    List<CityResponse> getAllCities();
    List<CityResponse> getCitiesByState(Long stateId);
    CityResponse getCityById(Long id);
    CityResponse createCity(CityRequest request);
    CityResponse updateCity(Long id, CityRequest request);
    void deleteCity(Long id);
}
