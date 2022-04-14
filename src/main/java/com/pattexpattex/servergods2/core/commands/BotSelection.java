package com.pattexpattex.servergods2.core.commands;

import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BotSelection implements SelectionMenu {

    //Options in the selection
    final List<SelectOption> list = new ArrayList<>();

    //Basically builds a SelectionMenu
    public final SelectionMenu setup(List<SelectOption> options, @Nullable List<SelectOption> defaultOptions) {

        list.clear();
        list.addAll(options);

        SelectionMenu.Builder menu = SelectionMenu.create(Objects.requireNonNull(getId()));

        //No options,
        // this is intended to be used as a failsafe
        if (options.isEmpty()) {
            menu.setId("menu:empty")
                    .setPlaceholder("Wow such empty")
                    .setRequiredRange(1, 1)
                    .addOptions(FormatUtil.getEmptyOption())
                    .setDisabled(true);
        }
        else {
            menu.setPlaceholder(getPlaceholder())
                    .setRequiredRange(getMinValues(), getMaxValues())
                    .addOptions(options);

            //Default options were given
            if (defaultOptions != null) menu.setDefaultOptions(defaultOptions);
        }

        return menu.build();
    }

    public abstract void run(@NotNull SelectionMenuEvent event);

    public @NotNull Type getType() {
        return Type.SELECTION_MENU;
    }

    public boolean isDisabled() {
        return false;
    }

    //This is CTRL+C, CTRL+V-ed here from... somewhere
    public final @NotNull DataObject toData() {
        DataObject data = DataObject.empty();

        data.put("type", 3);
        data.put("custom_id", getId());
        data.put("min_values", getMinValues());
        data.put("max_values", getMaxValues());
        data.put("disabled", isDisabled());
        data.put("options", DataArray.fromCollection(getOptions()));
        if (getPlaceholder() != null)
            data.put("placeholder", getPlaceholder());

        return data;
    }

    public @NotNull List<SelectOption> getOptions() {
        return list;
    }

    public int getMinValues() {
        return 1;
    }

    public int getMaxValues() {
        return list.size();
    }
}
