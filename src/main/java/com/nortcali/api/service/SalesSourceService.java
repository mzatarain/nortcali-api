package com.nortcali.api.service;

import com.nortcali.api.dto.request.SalesSourceRequest;
import com.nortcali.api.dto.response.SalesSourceResponse;

import java.util.List;

public interface SalesSourceService {

    List<SalesSourceResponse> getAll();

    SalesSourceResponse getById(Long id);

    SalesSourceResponse create(SalesSourceRequest request);

    SalesSourceResponse update(Long id, SalesSourceRequest request);

    void deactivate(Long id);
}
