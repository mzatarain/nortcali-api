package com.nortcali.api.controller;

import com.nortcali.api.dto.request.MenuCategoryRequest;
import com.nortcali.api.dto.request.MenuItemRequest;
import com.nortcali.api.dto.request.MenuItemVariantRequest;
import com.nortcali.api.dto.response.MenuCategoryResponse;
import com.nortcali.api.dto.response.MenuItemResponse;
import com.nortcali.api.dto.response.MenuItemVariantResponse;
import com.nortcali.api.dto.request.RecipeRequest;
import com.nortcali.api.dto.response.RecipeResponse;
import com.nortcali.api.service.MenuCategoryService;
import com.nortcali.api.service.MenuItemService;
import com.nortcali.api.service.MenuItemVariantService;
import com.nortcali.api.service.RecipeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class MenuController {

    private final MenuCategoryService categoryService;
    private final MenuItemService itemService;
    private final MenuItemVariantService variantService;
    private final RecipeService recipeService;

    public MenuController(MenuCategoryService categoryService,
                          MenuItemService itemService,
                          MenuItemVariantService variantService,
                          RecipeService recipeService) {
        this.categoryService = categoryService;
        this.itemService = itemService;
        this.variantService = variantService;
        this.recipeService = recipeService;
    }

    /* =====================
     * CATEGORIES
     * ===================== */

    @GetMapping("/restaurants/{restaurantId}/menu/categories")
    public ResponseEntity<List<MenuCategoryResponse>> getCategories(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(categoryService.getByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/menu/categories/{id}")
    public ResponseEntity<MenuCategoryResponse> getCategoryById(@PathVariable Long restaurantId,
                                                                @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PostMapping("/restaurants/{restaurantId}/menu/categories")
    public ResponseEntity<MenuCategoryResponse> createCategory(@PathVariable Long restaurantId,
                                                               @Valid @RequestBody MenuCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(restaurantId, request));
    }

    @PutMapping("/restaurants/{restaurantId}/menu/categories/{id}")
    public ResponseEntity<MenuCategoryResponse> updateCategory(@PathVariable Long restaurantId,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody MenuCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/restaurants/{restaurantId}/menu/categories/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long restaurantId,
                                                   @PathVariable Long id) {
        categoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /* =====================
     * ITEMS
     * ===================== */

    @GetMapping("/restaurants/{restaurantId}/menu/items")
    public ResponseEntity<List<MenuItemResponse>> getItems(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(itemService.getByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/menu/items/{id}")
    public ResponseEntity<MenuItemResponse> getItemById(@PathVariable Long restaurantId,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PostMapping("/restaurants/{restaurantId}/menu/items")
    public ResponseEntity<MenuItemResponse> createItem(@PathVariable Long restaurantId,
                                                       @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.create(restaurantId, request));
    }

    @PutMapping("/restaurants/{restaurantId}/menu/items/{id}")
    public ResponseEntity<MenuItemResponse> updateItem(@PathVariable Long restaurantId,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @DeleteMapping("/restaurants/{restaurantId}/menu/items/{id}")
    public ResponseEntity<Void> deactivateItem(@PathVariable Long restaurantId,
                                               @PathVariable Long id) {
        itemService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /* =====================
     * VARIANTS
     * ===================== */

    @GetMapping("/menu-items/{itemId}/variants")
    public ResponseEntity<List<MenuItemVariantResponse>> getVariants(@PathVariable Long itemId) {
        return ResponseEntity.ok(variantService.getByMenuItem(itemId));
    }

    @GetMapping("/menu-items/{itemId}/variants/{variantId}")
    public ResponseEntity<MenuItemVariantResponse> getVariantById(@PathVariable Long itemId,
                                                                   @PathVariable Long variantId) {
        return ResponseEntity.ok(variantService.getById(variantId));
    }

    @PostMapping("/menu-items/{itemId}/variants")
    public ResponseEntity<MenuItemVariantResponse> createVariant(@PathVariable Long itemId,
                                                                  @Valid @RequestBody MenuItemVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variantService.create(itemId, request));
    }

    @PutMapping("/menu-items/{itemId}/variants/{variantId}")
    public ResponseEntity<MenuItemVariantResponse> updateVariant(@PathVariable Long itemId,
                                                                  @PathVariable Long variantId,
                                                                  @Valid @RequestBody MenuItemVariantRequest request) {
        return ResponseEntity.ok(variantService.update(variantId, request));
    }

    @DeleteMapping("/menu-items/{itemId}/variants/{variantId}")
    public ResponseEntity<Void> deactivateVariant(@PathVariable Long itemId,
                                                   @PathVariable Long variantId) {
        variantService.deactivate(variantId);
        return ResponseEntity.noContent().build();
    }

    /* =====================
     * RECIPE
     * ===================== */

    @GetMapping("/menu-items/{itemId}/recipe")
    public ResponseEntity<RecipeResponse> getRecipe(@PathVariable Long itemId) {
        return ResponseEntity.ok(recipeService.getByMenuItem(itemId));
    }

    @PostMapping("/menu-items/{itemId}/recipe")
    public ResponseEntity<RecipeResponse> createOrUpdateRecipe(@PathVariable Long itemId,
                                                               @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.createOrUpdate(itemId, request));
    }
}
