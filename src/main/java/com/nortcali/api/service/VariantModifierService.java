package com.nortcali.api.service;

import com.nortcali.api.dto.request.VariantModifierRequest;
import com.nortcali.api.dto.response.VariantModifierResponse;

import java.util.List;

public interface VariantModifierService {

    List<VariantModifierResponse> getByVariant(Long variantId);
    VariantModifierResponse add(Long variantId, VariantModifierRequest request);
    void remove(Long variantId, Long modifierId);
}
