package com.pattexpattex.servergods2.commands.slash.moderation;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MuteCmd extends BotSlash {

    public static final Map<Long, Mute> mutes = new HashMap<>();
    private static final EnumSet<Permission> permissions = EnumSet.of(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_ADD_REACTION,
            Permission.VOICE_SPEAK,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_VAD,
            Permission.CREATE_INSTANT_INVITE,
            Permission.NICKNAME_CHANGE);

    public void run(@NotNull SlashCommandEvent event) {

        Guild guild = Objects.requireNonNull(event.getGuild());
        String commandPath = event.getCommandPath();

        switch (commandPath) {
            case "mute/start" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
                Member mod = event.getMember();
                String mention = Objects.requireNonNull(member).getAsMention();

                String time = event.getOption("time") != null ? Objects.requireNonNull(event.getOption("time")).getAsString() : null;
                String reason = event.getOption("reason") != null ? Objects.requireNonNull(event.getOption("reason")).getAsString() : null;
                int timeRaw = (int) FormatUtil.decodeTimeAlternate(time);

                Role role = updateMutedRole(guild);

                if (member.getUser() == Bot.getJDA().getSelfUser()) {
                    throw new BotException("You can't mute me");
                }

                if (mutes.containsKey(member.getIdLong())) {
                    throw new BotException("Member is already muted");
                }

                long start = Instant.now().getEpochSecond();
                long end = start + timeRaw;

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(
                        BotEmoji.YES + " Muted " + mention +
                                (reason == null ? "" : " with reason `" + reason + "`") +
                                (timeRaw < 0 ? "" : ", mute ends " + FormatUtil.epochTimestampRelative(end))).build())
                        .queue((msg) -> mutes.put(member.getIdLong(), new Mute(member, mod, reason, start, end, role, msg).begin()));
            }
            case "mute/end" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
                String mention = Objects.requireNonNull(member).getAsMention();

                Mute mute = mutes.remove(member.getIdLong());
                Role role = updateMutedRole(guild);

                if (!member.getRoles().contains(role) || !mutes.containsKey(member.getIdLong())) {
                    throw new BotException("Member is not muted");
                }

                if (mute == null) {
                    guild.removeRoleFromMember(member, role).reason("Mute end").queue();
                }
                else {
                    mute.end();
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Unmuted " + mention).build()).queue();
            }
            case "mute/get" -> {
                event.deferReply().queue();

                Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();

                Mute mute = mutes.get(Objects.requireNonNull(member).getIdLong());
                Role role = updateMutedRole(guild);


                if (!member.getRoles().contains(role) || !mutes.containsKey(member.getIdLong())) {
                    throw new BotException("Member is not muted");
                }

                if (mute == null) {
                    throw new BotException("Mute not found");
                }

                event.getHook().editOriginalEmbeds(
                        FormatUtil.defaultEmbed(
                                "Muted by " + mute.mod.getAsMention() +
                                        (mute.reason == null ? "" : "\nWith reason `" + mute.reason + "`") +
                                        "\nOn " + FormatUtil.epochTimestamp(mute.start) +
                                        (mute.time < 0 ? "" : "\nMute ends " + FormatUtil.epochTimestamp(mute.end)), "Mute info for " + member.getEffectiveName()).build()).queue();
            }
            case "mute/active" -> {
                event.deferReply().queue();

                if (mutes.isEmpty()) {
                    throw new BotException("No active mutes");
                }

                EmbedBuilder builder = FormatUtil.defaultEmbed(null, "Active mutes");
                int i = 0;

                for (Long id : mutes.keySet()) {
                    Mute mute = mutes.get(id);
                    i++;

                    builder.appendDescription(
                            i + ". " + mute.member.getAsMention() +
                                    " by " + mute.mod.getAsMention() +
                                    (mute.reason == null ? "" : " with reason `" + mute.reason + "`") +
                                    " on " + FormatUtil.epochTimestamp(mute.start) +
                                    (mute.time < 0 ? "" : ", ends " + FormatUtil.epochTimestampRelative(mute.end)) + "\n");
                }

                event.getHook().editOriginalEmbeds(builder.build()).queue();
            }
        }
    }

    private Role updateMutedRole(Guild guild) {
        Role role = Bot.getGuildConfig(guild).getMuted(guild);

        if (role == null) {
            if (guild.getRolesByName("muted", true).size() != 0) {
                role = guild.getRolesByName("muted", true).get(0);
            }
            else if (guild.getRolesByName("mute", true).size() != 0) {
                role = guild.getRolesByName("mute", true).get(0);
            }
            else {
                try {
                    role = guild.createRole().setPermissions(Permission.EMPTY_PERMISSIONS).setName("Muted").setColor(new Color(0x5c676f)).reason("Create mute role").complete();
                    Bot.getGuildConfig(guild).setMuted(role);
                }
                catch (RuntimeException e) {
                    rethrow(e);
                }
            }
        }

        for (GuildChannel channel : guild.getChannels(true)) {
            IPermissionContainer container = channel.getPermissionContainer();

            if (container.getPermissionOverride(role) != null) continue;
            channel.getPermissionContainer().createPermissionOverride(role).setDeny(permissions).reason("Update channel for effective mutes").queue();
        }

        return role;
    }

    public static class Mute extends Thread {

        public final Member member;
        public final Member mod;
        public final long start, end;
        public final String reason;

        private final int time;
        private final Role role;
        private final Guild guild;
        private final Message message;

        /**
         * @param start Epoch second of mute start
         * @param end Epoch second of mute end
         * @implNote if {@code end} is set to anything below {@code start}, the mute will not end on its own.
         */
        public Mute(Member member, Member mod, @Nullable String reason, long start, long end, Role role, Message message) {
            this.member = member;
            this.mod = mod;
            this.reason = reason;
            this.start = start;
            this.end = end;
            this.time = (int) (end <= start ? -1 : end - start);

            this.role = role;
            this.message = message;
            this.guild = member.getGuild();
        }

        @Override
        public void run() {
            guild.addRoleToMember(member, role).reason("Mute: " + reason).queue(null, (f) -> {throw new BotException(f);});

            if (time < 0) return;

            try {
                guild.removeRoleFromMember(member, role).reason("Mute ended").completeAfter(time, TimeUnit.SECONDS);

                message.getChannel().sendMessageEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Unmuted " + member.getAsMention()).build()).queue();
            }
            catch (RuntimeException ok) {
                guild.removeRoleFromMember(member, role).reason("Mute ended").queue();
            }
            finally {
                //Remove this mute from the list of active mutes
                mutes.remove(member.getIdLong());
            }
        }

        public Mute begin() {
            start();
            return this;
        }

        public void end() {
            this.interrupt();

            if (time < 0) {
                guild.removeRoleFromMember(member, role).reason("Mute ended").queue();

                //Remove this mute from the list of active mutes
                mutes.remove(member.getIdLong());
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
                new SubcommandData("active", "Get all active mutes")
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
