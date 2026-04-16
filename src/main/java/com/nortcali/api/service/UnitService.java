package com.nortcali.api.service;

import com.nortcali.api.dto.request.UnitRequest;
import com.nortcali.api.dto.response.UnitResponse;

import java.util.List;

public interface UnitService {

    List<UnitResponse> getAll();

    UnitResponse getById(Long id);

    UnitResponse create(UnitRequest request);

    UnitResponse update(Long id, UnitRequest request);
}
