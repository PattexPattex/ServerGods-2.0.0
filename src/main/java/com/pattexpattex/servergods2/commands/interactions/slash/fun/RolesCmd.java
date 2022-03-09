package com.pattexpattex.servergods2.commands.interactions.slash.fun;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.commands.interactions.selection.roles.GetRolesSelect;
import com.pattexpattex.servergods2.commands.interactions.selection.roles.SetOthersRolesSelect;
import com.pattexpattex.servergods2.commands.interactions.selection.roles.SetRolesSelect;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.listeners.interactions.SelectionEventListener;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This command creates a nice {@code SelectionMenu} to pick what roles to give to a member.
 *
 * @since 2.1.0
 */
public class RolesCmd implements BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {

        event.deferReply(true).queue();

        String commandPath = event.getCommandPath();
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());

        List<SelectOption> availableOptions = new ArrayList<>();
        List<SelectOption> selectedOptions = new ArrayList<>();
        List<SelectOption> allOptions = new ArrayList<>();

        List<String> enabledRolesIds = Bot.getGuildConfig(guild).getFunRoles();
        List<Role> memberRoles;
        List<Role> allRoles = guild.getRoles();

        allRoles.forEach((role) -> {
            if (guild.getSelfMember().canInteract(role) && !role.isPublicRole() && !role.isManaged()) {
                allOptions.add(SelectOption.of(role.getName(), role.getId()));
            }
        });

        enabledRolesIds.forEach((roleId) -> availableOptions.add(SelectOption.of(Objects.requireNonNull(guild.getRoleById(roleId)).getName(), roleId)));

        switch (commandPath) {
            case ("role/select") -> {
                Member optionMember = event.getOption("member") != null ? Objects.requireNonNull(event.getOption("member")).getAsMember() : null;

                if (optionMember != null) {
                    memberRoles = optionMember.getRoles();

                    if (!member.hasPermission(Permission.MANAGE_ROLES)) {
                        event.getHook().editOriginalEmbeds(FormatUtil.noPermissionEmbed(Permission.MANAGE_ROLES).build()).queue();
                        return;
                    }

                    memberRoles.forEach((role) -> {
                        if (allRoles.contains(guild.getRoleById(role.getId()))) {
                            selectedOptions.add(SelectOption.of(role.getName(), role.getId()));
                        }
                    });

                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.ROLE + " Edit the roles").build()).complete().editMessageComponents(
                                    ActionRow.of(SelectionEventListener.addSelection(new SetOthersRolesSelect(optionMember)).setup(allOptions, selectedOptions))).queue();
                }
                else {
                    memberRoles = member.getRoles();

                    memberRoles.forEach((role) -> {
                        if (enabledRolesIds.contains(role.getId())) {
                            selectedOptions.add(SelectOption.of(role.getName(), role.getId()));
                        }
                    });

                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.ROLE + " Select your roles").build())
                            .complete().editMessageComponents(ActionRow.of(new GetRolesSelect().setup(availableOptions, selectedOptions))).queue();
                }
            }
            case ("role/edit") -> {
                if (!member.hasPermission(Permission.MANAGE_ROLES)) {
                    event.getHook().editOriginalEmbeds(FormatUtil.noPermissionEmbed(Permission.MANAGE_SERVER).build()).queue();
                    return;
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.ROLE + " Edit the cosmetic roles").build())
                        .complete().editMessageComponents(ActionRow.of(new SetRolesSelect().setup(allOptions, availableOptions))).queue();
            }
        }
    }

    @Override
    public String getName() {
        return "roles";
    }

    @Override
    public String getDesc() {
        return "Manage cosmetic roles";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("select", "Select your cosmetic roles")
                        .addOption(OptionType.USER, "member", "Whose roles to edit (optional)", false),
                new SubcommandData("edit", "Edit the available cosmetic roles")
        };
    }
}
