package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.VariantModifierRequest;
import com.nortcali.api.dto.response.VariantModifierResponse;
import com.nortcali.api.entity.VariantModifier;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.ModifierGroupMapper;
import com.nortcali.api.repository.MenuItemVariantRepository;
import com.nortcali.api.repository.ModifierRepository;
import com.nortcali.api.repository.VariantModifierRepository;
import com.nortcali.api.service.VariantModifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class VariantModifierServiceImpl implements VariantModifierService {

    private final VariantModifierRepository vmRepo;
    private final MenuItemVariantRepository variantRepo;
    private final ModifierRepository modifierRepo;
    private final ModifierGroupMapper mapper;

    public VariantModifierServiceImpl(VariantModifierRepository vmRepo,
                                      MenuItemVariantRepository variantRepo,
                                      ModifierRepository modifierRepo,
                                      ModifierGroupMapper mapper) {
        this.vmRepo = vmRepo;
        this.variantRepo = variantRepo;
        this.modifierRepo = modifierRepo;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<VariantModifierResponse> getByVariant(Long variantId) {
        return vmRepo.findByVariantId(variantId).stream()
                .map(mapper::toVariantModifierResponse).toList();
    }

    @Override
    public VariantModifierResponse add(Long variantId, VariantModifierRequest request) {
        var variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", variantId));
        var modifier = modifierRepo.findById(request.getModifierId())
                .orElseThrow(() -> new ResourceNotFoundException("Modifier", request.getModifierId()));

        if (vmRepo.findByVariantIdAndModifierId(variantId, request.getModifierId()).isPresent()) {
            throw new BusinessRuleException("El modificador ya está asignado a esta variante");
        }

        VariantModifier vm = new VariantModifier();
        vm.setVariant(variant);
        vm.setModifier(modifier);
        vm.setPrice(request.getPrice());
        return mapper.toVariantModifierResponse(vmRepo.save(vm));
    }

    @Override
    public void remove(Long variantId, Long modifierId) {
        if (vmRepo.findByVariantIdAndModifierId(variantId, modifierId).isEmpty()) {
            throw new ResourceNotFoundException("VariantModifier", modifierId);
        }
        vmRepo.deleteByVariantIdAndModifierId(variantId, modifierId);
    }
}
