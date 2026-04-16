package com.nortcali.api.mapper;

import com.nortcali.api.dto.response.RecipeIngredientResponse;
import com.nortcali.api.dto.response.RecipeResponse;
import com.nortcali.api.entity.Recipe;
import com.nortcali.api.entity.RecipeIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    @Mapping(source = "menuItem.id", target = "menuItemId")
    @Mapping(source = "menuItem.name", target = "menuItemName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    @Mapping(source = "active", target = "isActive")
    @Mapping(expression = "java(totalCost(entity))", target = "totalCost")
    RecipeResponse toResponse(Recipe entity);

    @Mapping(source = "supply.id", target = "supplyId")
    @Mapping(source = "supply.name", target = "supplyName")
    @Mapping(source = "unit.id", target = "unitId")
    @Mapping(source = "unit.abbreviation", target = "unitAbbreviation")
    RecipeIngredientResponse toIngredientResponse(RecipeIngredient entity);

    default BigDecimal totalCost(Recipe recipe) {
        return recipe.getIngredients().stream()
                .map(RecipeIngredient::getCalculatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
