package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.RecipeIngredientRequest;
import com.nortcali.api.dto.request.RecipeRequest;
import com.nortcali.api.dto.response.RecipeResponse;
import com.nortcali.api.entity.*;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.RecipeMapper;
import com.nortcali.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock RecipeRepository recipeRepo;
    @Mock MenuItemRepository menuItemRepo;
    @Mock MenuItemVariantRepository variantRepo;
    @Mock SupplyRepository supplyRepo;
    @Mock UnitRepository unitRepo;
    @Mock RecipeMapper mapper;

    @InjectMocks
    RecipeServiceImpl service;

    // ── helpers ────────────────────────────────────────────────────────────────

    private MenuItem menuItem(Long id) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName("Tacos");
        return m;
    }

    private Supply supply(Long id, BigDecimal unitCost) {
        Supply s = new Supply();
        s.setId(id);
        s.setName("Tortilla");
        s.setUnitCost(unitCost);
        return s;
    }

    private Unit unit(Long id) {
        Unit u = new Unit();
        u.setId(id);
        u.setName("Kg");
        return u;
    }

    private RecipeRequest requestWith(int portions, Long supplyId, BigDecimal qty, Long unitId) {
        RecipeIngredientRequest ing = new RecipeIngredientRequest();
        ing.setSupplyId(supplyId);
        ing.setQuantity(qty);
        ing.setUnitId(unitId);

        RecipeRequest req = new RecipeRequest();
        req.setPortions(portions);
        req.setActive(true);
        req.setIngredients(List.of(ing));
        return req;
    }

    // ── Creación nueva ─────────────────────────────────────────────────────────

    @Test
    void createOrUpdate_nuevaReceta_calculaCostoDeIngrediente() {
        Long menuItemId = 1L;
        BigDecimal qty = new BigDecimal("0.5");
        BigDecimal unitCost = new BigDecimal("20.00");
        // calculatedCost = 0.5 * 20 = 10.00

        when(menuItemRepo.findById(menuItemId)).thenReturn(Optional.of(menuItem(menuItemId)));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)).thenReturn(Optional.empty());
        when(supplyRepo.findById(2L)).thenReturn(Optional.of(supply(2L, unitCost)));
        when(unitRepo.findById(3L)).thenReturn(Optional.of(unit(3L)));

        Recipe savedRecipe = new Recipe();
        when(recipeRepo.save(any(Recipe.class))).thenReturn(savedRecipe);
        when(mapper.toResponse(savedRecipe)).thenReturn(mock(RecipeResponse.class));

        service.createOrUpdate(menuItemId, requestWith(1, 2L, qty, 3L));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());
        Recipe persisted = captor.getValue();

        assertThat(persisted.getIngredients()).hasSize(1);
        RecipeIngredient ingredient = persisted.getIngredients().get(0);
        assertThat(ingredient.getCalculatedCost()).isEqualByComparingTo("10.00");
        assertThat(ingredient.getQuantity()).isEqualByComparingTo(qty);
    }

    // ── Update reemplaza ingredientes ──────────────────────────────────────────

    @Test
    void createOrUpdate_recetaExistente_reemplazaIngredientes() {
        Long menuItemId = 1L;

        // Receta existente con un ingrediente viejo
        Recipe existingRecipe = new Recipe();
        existingRecipe.setId(10L);
        RecipeIngredient oldIngredient = new RecipeIngredient();
        oldIngredient.setId(99L);
        existingRecipe.getIngredients().add(oldIngredient);

        when(menuItemRepo.findById(menuItemId)).thenReturn(Optional.of(menuItem(menuItemId)));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)).thenReturn(Optional.of(existingRecipe));
        when(supplyRepo.findById(2L)).thenReturn(Optional.of(supply(2L, new BigDecimal("15.00"))));
        when(unitRepo.findById(3L)).thenReturn(Optional.of(unit(3L)));
        when(recipeRepo.save(any(Recipe.class))).thenReturn(existingRecipe);
        when(mapper.toResponse(existingRecipe)).thenReturn(mock(RecipeResponse.class));

        service.createOrUpdate(menuItemId, requestWith(2, 2L, new BigDecimal("1.0"), 3L));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());
        Recipe persisted = captor.getValue();

        // El ingrediente viejo fue reemplazado; solo queda el nuevo
        assertThat(persisted.getIngredients()).hasSize(1);
        // calculatedCost = 1.0 * 15 = 15.00
        assertThat(persisted.getIngredients().get(0).getCalculatedCost()).isEqualByComparingTo("15.00");
    }

    // ── Múltiples ingredientes ─────────────────────────────────────────────────

    @Test
    void createOrUpdate_variosIngredientes_sumaCostosCorrectamente() {
        Long menuItemId = 1L;

        RecipeIngredientRequest ing1 = new RecipeIngredientRequest();
        ing1.setSupplyId(2L);
        ing1.setQuantity(new BigDecimal("2.0")); // cost = 2 * 5 = 10
        ing1.setUnitId(3L);

        RecipeIngredientRequest ing2 = new RecipeIngredientRequest();
        ing2.setSupplyId(4L);
        ing2.setQuantity(new BigDecimal("3.0")); // cost = 3 * 4 = 12
        ing2.setUnitId(3L);

        RecipeRequest req = new RecipeRequest();
        req.setPortions(1);
        req.setActive(true);
        req.setIngredients(List.of(ing1, ing2));

        when(menuItemRepo.findById(menuItemId)).thenReturn(Optional.of(menuItem(menuItemId)));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)).thenReturn(Optional.empty());
        when(supplyRepo.findById(2L)).thenReturn(Optional.of(supply(2L, new BigDecimal("5.00"))));
        when(supplyRepo.findById(4L)).thenReturn(Optional.of(supply(4L, new BigDecimal("4.00"))));
        when(unitRepo.findById(3L)).thenReturn(Optional.of(unit(3L)));

        Recipe savedRecipe = new Recipe();
        when(recipeRepo.save(any(Recipe.class))).thenReturn(savedRecipe);
        when(mapper.toResponse(savedRecipe)).thenReturn(mock(RecipeResponse.class));

        service.createOrUpdate(menuItemId, req);

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());
        Recipe persisted = captor.getValue();

        assertThat(persisted.getIngredients()).hasSize(2);
        assertThat(persisted.getIngredients().get(0).getCalculatedCost()).isEqualByComparingTo("10.00");
        assertThat(persisted.getIngredients().get(1).getCalculatedCost()).isEqualByComparingTo("12.00");
    }

    // ── MenuItem no encontrado ─────────────────────────────────────────────────

    @Test
    void createOrUpdate_menuItemNoEncontrado_lanzaResourceNotFoundException() {
        when(menuItemRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrUpdate(99L, requestWith(1, 1L, BigDecimal.ONE, 1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recipeRepo, never()).save(any());
    }

    // ── Insumo no encontrado ───────────────────────────────────────────────────

    @Test
    void createOrUpdate_insumoNoEncontrado_lanzaResourceNotFoundException() {
        Long menuItemId = 1L;
        when(menuItemRepo.findById(menuItemId)).thenReturn(Optional.of(menuItem(menuItemId)));
        when(recipeRepo.findByMenuItemIdAndIsActiveTrue(menuItemId)).thenReturn(Optional.empty());
        when(supplyRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrUpdate(menuItemId, requestWith(1, 99L, BigDecimal.ONE, 1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recipeRepo, never()).save(any());
    }
}
