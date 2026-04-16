package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RecipeRequest;
import com.nortcali.api.dto.response.RecipeResponse;
import com.nortcali.api.entity.Recipe;
import com.nortcali.api.entity.RecipeIngredient;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.RecipeMapper;
import com.nortcali.api.repository.*;
import com.nortcali.api.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemVariantRepository variantRepo;
    private final SupplyRepository supplyRepo;
    private final UnitRepository unitRepo;
    private final RecipeMapper mapper;

    public RecipeServiceImpl(RecipeRepository recipeRepo,
                             MenuItemRepository menuItemRepo,
                             MenuItemVariantRepository variantRepo,
                             SupplyRepository supplyRepo,
                             UnitRepository unitRepo,
                             RecipeMapper mapper) {
        this.recipeRepo = recipeRepo;
        this.menuItemRepo = menuItemRepo;
        this.variantRepo = variantRepo;
        this.supplyRepo = supplyRepo;
        this.unitRepo = unitRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getByMenuItem(Long menuItemId) {
        Recipe recipe = recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receta activa no encontrada para el platillo con id " + menuItemId));
        return mapper.toResponse(recipe);
    }

    @Override
    public RecipeResponse createOrUpdate(Long menuItemId, RecipeRequest request) {
        var menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", menuItemId));

        // Buscar receta existente (con o sin variante)
        Recipe recipe;
        if (request.getVariantId() != null) {
            recipe = recipeRepo.findByMenuItemIdAndVariantIdAndIsActiveTrue(menuItemId, request.getVariantId())
                    .orElse(new Recipe());
        } else {
            recipe = recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)
                    .orElse(new Recipe());
        }

        recipe.setMenuItem(menuItem);
        recipe.setPortions(request.getPortions());
        recipe.setActive(request.isActive());

        if (request.getVariantId() != null) {
            var variant = variantRepo.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItemVariant", request.getVariantId()));
            recipe.setVariant(variant);
        }

        // Reemplazar ingredientes
        List<RecipeIngredient> ingredients = buildIngredients(request, recipe);
        recipe.getIngredients().clear();
        recipe.getIngredients().addAll(ingredients);

        log.info("Guardando receta para platillo {} con {} ingredientes", menuItemId, ingredients.size());
        return mapper.toResponse(recipeRepo.save(recipe));
    }

    private List<RecipeIngredient> buildIngredients(RecipeRequest request, Recipe recipe) {
        List<RecipeIngredient> result = new ArrayList<>();
        for (var dto : request.getIngredients()) {
            var supply = supplyRepo.findById(dto.getSupplyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supply", dto.getSupplyId()));
            var unit = unitRepo.findById(dto.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", dto.getUnitId()));

            // Regla de negocio: calculatedCost = quantity * supply.unitCost
            BigDecimal calculatedCost = dto.getQuantity().multiply(supply.getUnitCost());

            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setRecipe(recipe);
            ingredient.setSupply(supply);
            ingredient.setUnit(unit);
            ingredient.setQuantity(dto.getQuantity());
            ingredient.setCalculatedCost(calculatedCost);
            result.add(ingredient);
        }
        return result;
    }
}
