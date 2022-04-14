package com.pattexpattex.servergods2.commands.slash.config;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.config.GuildConfig;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ConfigCmd extends BotSlash {

    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        Guild guild = event.getGuild();
        String commandPath = event.getCommandPath();
        GuildConfig config = Bot.getGuildConfig(guild);
        String empty = "`empty`";

        switch (commandPath) {
            case "config/get" -> {
                Role muted = config.getMuted(guild);
                Role giveaway = config.getGiveaway(guild);
                Role poll = config.getPoll(guild);
                TextChannel welcome = config.getWelcome(guild);
                String invite = config.getInvite();

                EmbedBuilder builder = FormatUtil.defaultEmbed(null, Emotes.GEAR + " Server Settings");

                builder.appendDescription("\nMuted role - " +         (muted != null      ? muted.getAsMention() : empty))
                        .appendDescription("\nGiveaway role - " +     (giveaway != null   ? giveaway.getAsMention() : empty))
                        .appendDescription("\nPoll role - " +         (poll != null       ? poll.getAsMention() : empty))
                        .appendDescription("\nWelcome channel - " +   (welcome != null    ? welcome.getAsMention() : empty))
                        .appendDescription("\nPermanent invite - " +  (invite != null     ? "`" + invite + "`" : empty));

                event.getHook().editOriginalEmbeds(builder.build()).queue();
            }

            case "config/set/muted", "config/set/giveaway", "config/set/poll" -> {
                Role role = Objects.requireNonNull(event.getOption("role")).getAsRole();
                String name;
                String value = role.getAsMention();

                if (commandPath.equals("config/set/muted")) {
                    config.setMuted(role);
                    name = "muted";
                }
                else if (commandPath.equals("config/set/giveaway")) {
                    config.setGiveaway(role);
                    name = "giveaway";
                }
                else {
                    config.setPoll(role);
                    name = "poll";
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.GEAR + " Set `" + name + "` to " + value).build()).queue();
            }
            case "config/set/welcome" -> {
                TextChannel channel = (TextChannel) Objects.requireNonNull(event.getOption("channel")).getAsGuildChannel();

                config.setWelcome(channel);

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.GEAR + " Set `welcome` to " + channel.getAsMention()).build()).queue();
            }
            case "config/set/invite" -> {
                String url = Objects.requireNonNull(event.getOption("url")).getAsString();

                config.setInvite(url);

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.GEAR + " Set `invite` to `" + url + "`").build()).queue();
            }

            case "config/clear/muted", "config/clear/giveaway", "config/clear/poll", "config/clear/welcome", "config/clear/invite" -> {
                String name;

                if (commandPath.endsWith("muted")) {
                    config.setMuted(null);
                    name = "muted";
                }
                else if (commandPath.endsWith("giveaway")) {
                    config.setGiveaway(null);
                    name = "giveaway";
                }
                else if (commandPath.endsWith("poll")) {
                    config.setPoll(null);
                    name = "poll";
                }
                else if (commandPath.endsWith("welcome")) {
                    config.setWelcome(null);
                    name = "welcome";
                }
                else {
                    config.setInvite(null);
                    name = "invite";
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.GEAR + " Cleared `" + name + "`").build()).queue();
            }
        }
    }

    public String getName() {
        return "config";
    }

    public String getDesc() {
        return "Config for this server";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("get", "Current settings")
        };
    }

    @Override
    public SubcommandGroupData[] getSubcommandGroups() {
        return new SubcommandGroupData[]{
                new SubcommandGroupData("set", "Set a value").addSubcommands(
                        new SubcommandData("muted", "Set the muted role")
                                .addOption(OptionType.ROLE, "role", "New role", true),
                        new SubcommandData("giveaway", "Set the giveaways role")
                                .addOption(OptionType.ROLE, "role", "New role", true),
                        new SubcommandData("poll", "Set the polls role")
                                .addOption(OptionType.ROLE, "role", "New role", true),
                        new SubcommandData("welcome", "Set the welcome channel")
                                .addOption(OptionType.CHANNEL, "channel", "New channel", true),
                        new SubcommandData("invite", "Set the permanent invite")
                                .addOption(OptionType.STRING, "url", "New URL", true)),
                new SubcommandGroupData("clear", "Clear a value").addSubcommands(
                        new SubcommandData("muted", "Clear the muted role"),
                        new SubcommandData("giveaway", "Clear the giveaway role"),
                        new SubcommandData("poll", "Clear the poll role"),
                        new SubcommandData("welcome", "Clear the welcome channel"),
                        new SubcommandData("invite", "Clear the permanent invite"))
        };
    }

    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
