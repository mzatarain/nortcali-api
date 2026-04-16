package com.nortcali.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RecipeRequest {

    private Long variantId;

    @NotNull(message = "Las porciones son obligatorias")
    @Min(value = 1, message = "Las porciones deben ser al menos 1")
    private Integer portions;

    private boolean isActive = true;

    @NotEmpty(message = "La receta debe tener al menos un ingrediente")
    @Valid
    private List<RecipeIngredientRequest> ingredients;

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Integer getPortions() { return portions; }
    public void setPortions(Integer portions) { this.portions = portions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public List<RecipeIngredientRequest> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredientRequest> ingredients) { this.ingredients = ingredients; }
}
