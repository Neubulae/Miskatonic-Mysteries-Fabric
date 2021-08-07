package com.miskatonicmysteries.client.compat.rei.category;

import com.miskatonicmysteries.common.feature.PotentialItem;
import com.miskatonicmysteries.common.feature.recipe.ChemistryRecipe;
import com.miskatonicmysteries.common.registry.MMObjects;
import com.miskatonicmysteries.common.util.Constants;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.EntryIngredient;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.api.RecipeDisplay;
import me.shedaniel.rei.api.widgets.Widgets;
import me.shedaniel.rei.gui.widget.Widget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChemistrySetCategory implements RecipeCategory<ChemistrySetCategory.Display> {
    public static final Identifier ID = new Identifier(Constants.MOD_ID, "chemistry_recipe");
    public static final EntryStack LOGO = EntryStack.create(MMObjects.CHEMISTRY_SET);

    @Override
    public @NotNull Identifier getIdentifier() {
        return ID;
    }

    @Override
    public @NotNull EntryStack getLogo() {
        return LOGO;
    }

    @Override
    public @NotNull String getCategoryName() {
        return I18n.translate("rei." + ID.toString().replaceAll(":", "."));
    }

    @Override
    public int getDisplayHeight() {
        return 64;
    }

    @Override
    public @NotNull List<Widget> setupDisplay(Display recipeDisplay, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        Point startPoint = new Point(bounds.getCenterX() - 64, bounds.getCenterY() - 2);
        widgets.add(Widgets.createRecipeBase(bounds));
        List<EntryIngredient> input = recipeDisplay.getInputEntries();
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 4, startPoint.y - 24)).entries(input.get(0)).markInput());
        if (input.size() > 1) {
            widgets.add(Widgets.createSlot(new Point(startPoint.x + 22, startPoint.y - 24)).entries(recipeDisplay.getInputEntries().get(1)).markInput());
            if (input.size() > 2) {
                widgets.add(Widgets.createSlot(new Point(startPoint.x + 4, startPoint.y - 6)).entries(recipeDisplay.getInputEntries().get(2)).markInput());
                if (input.size() > 3) {
                    widgets.add(Widgets.createSlot(new Point(startPoint.x + 22, startPoint.y - 6)).entries(recipeDisplay.getInputEntries().get(3)).markInput());
                    if (input.size() > 4) {
                        widgets.add(Widgets.createSlot(new Point(startPoint.x + 4, startPoint.y + 12)).entries(recipeDisplay.getInputEntries().get(4)).markInput());
                        if (input.size() > 5) {
                            widgets.add(Widgets.createSlot(new Point(startPoint.x + 22, startPoint.y + 12)).entries(recipeDisplay.getInputEntries().get(5)).markInput());
                        }
                    }
                }
            }
        }
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 39, startPoint.y - 6)).animationDurationTicks(100));
        return widgets;
    }

    public static class Display implements RecipeDisplay {
        private final List<List<EntryStack>> input;
        private final List<List<EntryStack>> output;
        private final List<List<EntryStack>> outputMatches;

        public Display(ChemistryRecipe recipe) {
            input = EntryStack.ofIngredients(recipe.ingredients);

            List<List<EntryStack>> output = new ArrayList<>();
            List<List<EntryStack>> outputMatches = new ArrayList<>();
            for (PotentialItem potentialItem : recipe.output) {
                output.add(Collections.singletonList(EntryStack.create(potentialItem.out)));
                outputMatches.add(Collections.singletonList(EntryStack.create(potentialItem.in)));
            }
            this.output = output;
            this.outputMatches = outputMatches;
        }

        @Override
        public @NotNull List<List<EntryStack>> getInputEntries() {
            return input;
        }

        @Override
        public @NotNull List<List<EntryStack>> getResultingEntries() {
            return output;
        }

        public List<List<EntryStack>> getOutputMatches() {
            return outputMatches;
        }

        @Override
        public @NotNull Identifier getRecipeCategory() {
            return ID;
        }
    }
}
