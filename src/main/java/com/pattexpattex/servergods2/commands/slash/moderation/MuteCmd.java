package com.pattexpattex.servergods2.commands.slash.moderation;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.mute.Mute;
import com.pattexpattex.servergods2.core.mute.MuteManager;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MuteCmd extends BotSlash {

    public void run(@NotNull SlashCommandEvent event) {

        Guild guild = Objects.requireNonNull(event.getGuild());
        String commandPath = event.getCommandPath();
        MuteManager manager = Bot.getMuteManager();

        switch (commandPath) {
            case "mute/start" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
                Member mod = event.getMember();
                String mention = Objects.requireNonNull(member).getAsMention();

                String time = event.getOption("time") != null ? Objects.requireNonNull(event.getOption("time")).getAsString() : null;
                String reason = event.getOption("reason") != null ? Objects.requireNonNull(event.getOption("reason")).getAsString() : null;
                int timeRaw = (int) FormatUtil.decodeTimeAlternate(time);

                if (member.getUser() == Bot.getJDA().getSelfUser()) {
                    throw new BotException("You can't mute me");
                }

                if (manager.getMute(member.getIdLong()) != null) {
                    throw new BotException("Member is already muted");
                }

                long start = Instant.now().getEpochSecond();
                long end = start + timeRaw;

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(
                        Emotes.YES + " Muted " + mention +
                                (reason == null ? "" : " with reason `" + reason + "`") +
                                (timeRaw < 0 ? "" : ", mute ends " + FormatUtil.epochTimestampRelative(end))).build())
                        .queue((msg) -> {
                            Mute mute = new Mute(manager, member.getIdLong(), Objects.requireNonNull(mod).getIdLong(), guild.getIdLong(), start, end, reason);
                            mute.start();
                        });
            }
            case "mute/end" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
                String mention = Objects.requireNonNull(member).getAsMention();

                Mute mute = manager.getMute(member.getIdLong());

                Role role = MuteManager.updateMutedRole(guild);

                if (mute == null) {
                    if (!member.getRoles().contains(role)) {
                        throw new BotException("Member is not muted");
                    }

                    guild.removeRoleFromMember(member, role).reason("Mute end").queue();
                }
                else {
                    mute.end();
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Unmuted " + mention).build()).queue();
            }
            case "mute/get" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();

                Mute mute = manager.getMute(Objects.requireNonNull(member).getIdLong());
                Role role = MuteManager.updateMutedRole(guild);


                if (!member.getRoles().contains(role)) {
                    throw new BotException("Member is not muted");
                }

                if (mute == null) {
                    throw new BotException("Mute not found");
                }

                event.getHook().editOriginalEmbeds(
                        FormatUtil.defaultEmbed(
                                "Muted by " + Objects.requireNonNull(guild.getMemberById(mute.moderator)).getAsMention() +
                                        (mute.reason == null ? "" : "\nWith reason `" + mute.reason + "`") +
                                        "\nOn " + FormatUtil.epochTimestamp(mute.start) +
                                        (mute.infinite ? "" : "\nMute ends " + FormatUtil.epochTimestampRelative(mute.end)), "Mute info for " + member.getEffectiveName()).build()).queue();
            }
            case "mute/active" -> {
                event.deferReply().queue();
                Role role = MuteManager.updateMutedRole(guild);

                List<Mute> mutes = new ArrayList<>();
                guild.getMembersWithRoles(role).forEach((member) -> {
                    Mute mute = manager.getMute(member.getIdLong());

                    if (mute != null) {
                        mutes.add(mute);
                    }
                });

                if (mutes.isEmpty()) {
                    throw new BotException("No active mutes");
                }

                EmbedBuilder builder = FormatUtil.defaultEmbed(null, "Active mutes");
                int i = 0;

                for (Mute mute : mutes) {
                    i++;

                    builder.appendDescription(
                            "**" + i + ".** " + Objects.requireNonNull(guild.getMemberById(mute.id)).getAsMention() +
                                    " by " + Objects.requireNonNull(guild.getMemberById(mute.moderator)).getAsMention() +
                                    (mute.reason == null ? "" : " with reason `" + mute.reason + "`") +
                                    " on " + FormatUtil.epochTimestamp(mute.start) +
                                    (mute.infinite ? "" : ", ends " + FormatUtil.epochTimestampRelative(mute.end)) + "\n");
                }

                event.getHook().editOriginalEmbeds(builder.build()).queue();
            }
        }
    }

    public String getName() {
        return "mute";
    }

    public String getDesc() {
        return "Mutes a member";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("start", "Mute a member")
                        .addOption(OptionType.USER, "member", "Member to mute", true)
                        .addOption(OptionType.STRING, "time", "Time of the mute, e.g.: 1d 2h 3m 4s (leave empty for an endless mute)", false)
                        .addOption(OptionType.STRING, "reason", "Mute reason"),
                new SubcommandData("end", "Unmute a member")
                        .addOption(OptionType.USER, "member", "Member to unmute", true),
                new SubcommandData("get", "Get info about a mute")
                        .addOption(OptionType.USER, "member", "Muted member", true),
                new SubcommandData("active", "Get the currently active mutes")
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
