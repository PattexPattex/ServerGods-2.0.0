package com.pattexpattex.servergods2.commands.selection.roles;

import com.pattexpattex.servergods2.commands.selection.DisabledSelect;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotSelection;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GetRolesSelect extends BotSelection {

    public void run(@NotNull SelectionMenuEvent event) {

        Guild guild = event.getGuild();
        Member member = event.getMember();

        List<Role> selectedRoles = new ArrayList<>();
        List<SelectOption> selectedOptions = event.getSelectedOptions();

        //List of roles that a member can give to himself
        List<Role> availableRoles = new ArrayList<>();
        List<String> availableRolesConf = Bot.getGuildConfig(guild).getFunRoles();

        //Roles of the member
        List<Role> memberRoles = Objects.requireNonNull(member).getRoles();

        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        availableRolesConf.forEach((role) ->
            availableRoles.add(Objects.requireNonNull(guild).getRoleById(role)));

        if (selectedOptions != null) {
            selectedOptions.forEach((option) ->
                    selectedRoles.add(Objects.requireNonNull(guild).getRoleById(option.getValue())));
        }

        selectedRoles.forEach((role) -> {
            if (!memberRoles.contains(role)) {
                rolesToAdd.add(role);
            }
        });

        availableRoles.forEach((role) -> {
            if (memberRoles.contains(role) && !selectedRoles.contains(role)) {
                rolesToRemove.add(role);
            }
        });

        //Modify the member's roles
        Objects.requireNonNull(guild).modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();

        //Reply
        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Modified your roles").build()).queue();

        //Disable the selection
        event.getHook().editOriginalComponents(ActionRow.of(new DisabledSelect())).queue();
    }

    public int getMinValues() {
        return 0;
    }

    @Nullable
    public String getPlaceholder() {
        return "Select a role";
    }

    @Nullable
    public String getId() {
        return "menu:get_roles";
    }
}
