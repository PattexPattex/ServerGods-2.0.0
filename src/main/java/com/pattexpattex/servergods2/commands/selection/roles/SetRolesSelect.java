package com.pattexpattex.servergods2.commands.selection.roles;

import com.pattexpattex.servergods2.commands.selection.DisabledSelect;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotSelection;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SetRolesSelect extends BotSelection {

    public void run(@NotNull SelectionMenuEvent event) {

        List<String> selectedRolesId = new ArrayList<>();
        List<SelectOption> selectOptions = event.getSelectedOptions();

        if (selectOptions != null)
            selectOptions.forEach((option) -> selectedRolesId.add(option.getValue()));

        Bot.getGuildConfig(event.getGuild()).setFunRoles(selectedRolesId);

        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.SETTINGS + " Set the cosmetic roles").build()).queue();

        event.getHook().editOriginalComponents(ActionRow.of(new DisabledSelect())).queue();
    }

    @Nullable
    public String getPlaceholder() {
        return "Select a role";
    }

    public int getMaxValues() {
        return getOptions().size();
    }

    public int getMinValues() {
        return 0;
    }

    @Nullable
    public String getId() {
        return "menu:set_roles";
    }
}
