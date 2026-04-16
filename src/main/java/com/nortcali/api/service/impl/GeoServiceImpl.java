package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CityRequest;
import com.nortcali.api.dto.request.CountryRequest;
import com.nortcali.api.dto.request.StateRequest;
import com.nortcali.api.dto.response.CityResponse;
import com.nortcali.api.dto.response.CountryResponse;
import com.nortcali.api.dto.response.StateResponse;
import com.nortcali.api.entity.City;
import com.nortcali.api.entity.Country;
import com.nortcali.api.entity.State;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.GeoMapper;
import com.nortcali.api.repository.CityRepository;
import com.nortcali.api.repository.CountryRepository;
import com.nortcali.api.repository.StateRepository;
import com.nortcali.api.service.GeoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class GeoServiceImpl implements GeoService {

    private final CountryRepository countryRepo;
    private final StateRepository stateRepo;
    private final CityRepository cityRepo;
    private final GeoMapper mapper;

    public GeoServiceImpl(CountryRepository countryRepo,
                          StateRepository stateRepo,
                          CityRepository cityRepo,
                          GeoMapper mapper) {
        this.countryRepo = countryRepo;
        this.stateRepo = stateRepo;
        this.cityRepo = cityRepo;
        this.mapper = mapper;
    }

    /* ========== COUNTRIES ========== */

    @Override @Transactional(readOnly = true)
    public List<CountryResponse> getAllCountries() {
        return countryRepo.findAll().stream().map(mapper::toCountryResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public CountryResponse getCountryById(Long id) {
        return mapper.toCountryResponse(findCountryOrThrow(id));
    }

    @Override
    public CountryResponse createCountry(CountryRequest request) {
        Country entity = mapper.toCountryEntity(request);
        return mapper.toCountryResponse(countryRepo.save(entity));
    }

    @Override
    public CountryResponse updateCountry(Long id, CountryRequest request) {
        Country entity = findCountryOrThrow(id);
        mapper.updateCountry(request, entity);
        return mapper.toCountryResponse(countryRepo.save(entity));
    }

    @Override
    public void deleteCountry(Long id) {
        if (!countryRepo.existsById(id)) throw new ResourceNotFoundException("Country", id);
        countryRepo.deleteById(id);
    }

    /* ========== STATES ========== */

    @Override @Transactional(readOnly = true)
    public List<StateResponse> getAllStates() {
        return stateRepo.findAll().stream().map(mapper::toStateResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<StateResponse> getStatesByCountry(Long countryId) {
        return stateRepo.findByCountryId(countryId).stream().map(mapper::toStateResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public StateResponse getStateById(Long id) {
        return mapper.toStateResponse(findStateOrThrow(id));
    }

    @Override
    public StateResponse createState(StateRequest request) {
        Country country = findCountryOrThrow(request.getCountryId());
        State entity = mapper.toStateEntity(request);
        entity.setCountry(country);
        return mapper.toStateResponse(stateRepo.save(entity));
    }

    @Override
    public StateResponse updateState(Long id, StateRequest request) {
        State entity = findStateOrThrow(id);
        Country country = findCountryOrThrow(request.getCountryId());
        mapper.updateState(request, entity);
        entity.setCountry(country);
        return mapper.toStateResponse(stateRepo.save(entity));
    }

    @Override
    public void deleteState(Long id) {
        if (!stateRepo.existsById(id)) throw new ResourceNotFoundException("State", id);
        stateRepo.deleteById(id);
    }

    /* ========== CITIES ========== */

    @Override @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepo.findAll().stream().map(mapper::toCityResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CityResponse> getCitiesByState(Long stateId) {
        return cityRepo.findByStateId(stateId).stream().map(mapper::toCityResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public CityResponse getCityById(Long id) {
        return mapper.toCityResponse(findCityOrThrow(id));
    }

    @Override
    public CityResponse createCity(CityRequest request) {
        State state = findStateOrThrow(request.getStateId());
        City entity = mapper.toCityEntity(request);
        entity.setState(state);
        return mapper.toCityResponse(cityRepo.save(entity));
    }

    @Override
    public CityResponse updateCity(Long id, CityRequest request) {
        City entity = findCityOrThrow(id);
        State state = findStateOrThrow(request.getStateId());
        mapper.updateCity(request, entity);
        entity.setState(state);
        return mapper.toCityResponse(cityRepo.save(entity));
    }

    @Override
    public void deleteCity(Long id) {
        if (!cityRepo.existsById(id)) throw new ResourceNotFoundException("City", id);
        cityRepo.deleteById(id);
    }

    /* ========== Helpers ========== */

    private Country findCountryOrThrow(Long id) {
        return countryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Country", id));
    }

    private State findStateOrThrow(Long id) {
        return stateRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("State", id));
    }

    private City findCityOrThrow(Long id) {
        return cityRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("City", id));
    }
}
