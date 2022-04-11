package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InviteCmd extends BotSlash {

    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        Guild guild = Objects.requireNonNull(event.getGuild());
        Member member = Objects.requireNonNull(event.getMember());
        TextChannel channel = event.getTextChannel();
        String commandPath = event.getCommandPath();

        switch (commandPath) {
            case "invite/create" -> {
                int maxUse = event.getOption("max-uses") != null ? (int) Objects.requireNonNull(event.getOption("max-uses")).getAsLong() : 1;
                String maxAge = event.getOption("max-age") != null ? Objects.requireNonNull(event.getOption("max-age")).getAsString() : "5m";
                int maxAgeRaw;
                try {
                    maxAgeRaw = (int) FormatUtil.decodeTimeAlternate(maxAge);
                }
                catch (NumberFormatException e) {
                    throw new BotException("Invalid time input", e);
                }

                String invite = channel.createInvite().setMaxAge(maxAgeRaw).setMaxUses(maxUse).complete().getUrl();

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " *This invite is valid for `" + FormatUtil.formatTimeAlternate(maxAgeRaw) + "` and has a max use of `" + maxUse + "`*").setTitle(invite, invite).build()).queue();
            }
            case "invite/permanent/refresh", "invite/permanent/clear" -> {
                if (!member.hasPermission(Permission.MANAGE_SERVER)) {
                    event.getHook().editOriginalEmbeds(FormatUtil.noPermissionEmbed(Permission.MANAGE_SERVER).build()).queue();
                    return;
                }

                guild.retrieveInvites().complete().forEach((invite) -> {
                    if (invite.getUrl().equals(Bot.getGuildConfig(guild).getInvite())) {
                        invite.delete().queue();
                    }
                });

                if (commandPath.endsWith("refresh")) {
                    String invite = channel.createInvite().setMaxUses(0).setMaxAge(0).complete().getUrl();
                    Bot.getGuildConfig(guild).setInvite(invite);

                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " *This is the new permanent invite*").setTitle(invite, invite).build()).queue();
                }
                else {
                    String invite = Bot.getGuildConfig(guild).getInvite();

                    if (invite == null) {
                        throw new BotException("Invite is already cleared");
                    }
                    else {
                        Bot.getGuildConfig(guild).setInvite(null);

                        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Deleted the permanent invite").build()).queue();
                    }
                }
            }
            case "invite/permanent/get" -> {
                String invite = Bot.getGuildConfig(guild).getInvite();

                if (invite == null) {
                    invite = channel.createInvite().setMaxAge(0).setMaxUses(0).complete().getUrl();
                    Bot.getGuildConfig(guild).setInvite(invite);
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " *This is the current permanent invite*").setTitle(invite, invite).build()).queue();
            }
        }
    }

    public String getName() {
        return "invite";
    }

    public String getDesc() {
        return "Creates a fancy invite to this server";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("create", "Create a new invite")
                        .addOption(OptionType.INTEGER, "max-uses", "Number of allowed uses (1 by default)", false)
                        .addOption(OptionType.STRING, "max-age", "Max age of the new invite, formatted like [1d 2h 3m 4s] (5m by default)", false),
        };
    }

    @Override
    public SubcommandGroupData[] getSubcommandGroups() {
        return new SubcommandGroupData[]{
                new SubcommandGroupData("permanent", "Permanent invite").addSubcommands(
                        new SubcommandData("get", "Get the permanent invite"),
                        new SubcommandData("refresh", "Replace the current invite with a new one")
                                .addOption(OptionType.STRING, "new", "The new invite URL", false),
                        new SubcommandData("clear", "Clear the permanent invite"))
        };
    }
}
