package com.pattexpattex.servergods2.commands.selection;

import com.pattexpattex.servergods2.core.commands.BotSelection;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

//This is an empty and disabled selection
public class DisabledSelect extends BotSelection {

    public void run(@NotNull SelectionMenuEvent event) {}

    public @Nullable String getPlaceholder() {
        return "Interaction completed";
    }

    public int getMaxValues() {
        return 1;
    }

    public @Nullable String getId() {
        return "menu:disabled";
    }

    public boolean isDisabled() {
        return true;
    }

    public @NotNull List<SelectOption> getOptions() {
        return new ArrayList<>(List.of(FormatUtil.getEmptyOption()));
    }
}
