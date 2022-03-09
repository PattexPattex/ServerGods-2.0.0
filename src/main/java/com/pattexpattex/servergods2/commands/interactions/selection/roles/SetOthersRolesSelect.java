package com.pattexpattex.servergods2.commands.interactions.selection.roles;

import com.pattexpattex.servergods2.commands.interactions.selection.BotSelection;
import com.pattexpattex.servergods2.commands.interactions.selection.DisabledSelect;
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

//IntelliJ says this class can be a record,
// but I dunno what a record is, so yeah
public class SetOthersRolesSelect implements BotSelection {

    //This member is not the one who triggered the SelectionMenuEvent
    private final Member member;

    //This constructor is special, see RolesCmd and SelectionEventListener to see why
    public SetOthersRolesSelect(Member member) {
        this.member = member;
    }

    public void run(@NotNull SelectionMenuEvent event) {

        Guild guild = event.getGuild();

        List<Role> selectedRoles = new ArrayList<>();
        List<SelectOption> selectOptions = event.getSelectedOptions();

        //Roles of the member
        List<Role> memberRoles = member.getRoles();

        //All roles in a guild
        List<Role> availableRoles = Objects.requireNonNull(guild).getRoles();

        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        if (selectOptions != null) {
            selectOptions.forEach((option) -> selectedRoles.add(guild.getRoleById(option.getValue())));
        }

        selectedRoles.forEach((role) -> {
            if (!memberRoles.contains(role))
                rolesToAdd.add(role);
        });

        availableRoles.forEach((role) -> {
            if (memberRoles.contains(role) && !selectedRoles.contains(role))
                rolesToRemove.add(role);
        });

        //Modify the roles of the member
        guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();

        //Reply
        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Modified roles of " +
                member.getAsMention()).build()).queue();

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
        return "menu:set_others_roles";
    }
}
