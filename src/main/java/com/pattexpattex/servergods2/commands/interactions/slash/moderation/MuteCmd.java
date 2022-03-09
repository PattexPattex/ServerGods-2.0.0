package com.pattexpattex.servergods2.commands.interactions.slash.moderation;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MuteCmd implements BotSlash {

    protected static final Map<Long, Mute> mutes = new HashMap<>();

    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        Guild guild = Objects.requireNonNull(event.getGuild());
        String commandPath = event.getCommandPath();

        Member member = Objects.requireNonNull(event.getOption("member")).getAsMember();
        String mention = Objects.requireNonNull(member).getAsMention();

        switch (commandPath) {
            case "mute/mute" -> {
                String time = event.getOption("time") != null ? Objects.requireNonNull(event.getOption("time")).getAsString() : "1h";
                int timeRaw = (int) FormatUtil.decodeTimeAlternate(time);

                Role role = updateMutedRole(guild);

                if (member.getRoles().contains(role)) {
                    throw new BotException("Member is already muted");
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Muted " + mention + " for `" + FormatUtil.formatTimeAlternate(timeRaw) + "`").build())
                        .queue((msg) -> mutes.put(member.getIdLong(), new Mute(member, timeRaw, role, msg).begin()));
            }
            case "mute/unmute" -> {
                Mute mute = mutes.remove(member.getIdLong());
                Role role = updateMutedRole(guild);

                if (!member.getRoles().contains(role)) {
                    throw new BotException("Member is not muted");
                }

                if (mute == null) {
                    guild.removeRoleFromMember(member, role).queue();

                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Unmuted " + mention).build()).queue();
                }
                else {
                    mute.end();
                }
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
                    role = guild.createRole().setPermissions(Permission.EMPTY_PERMISSIONS).setName("Muted").setColor(new Color(0x5c676f)).complete();

                    for (GuildChannel channel : guild.getChannels(true)) {
                        channel.getPermissionContainer().createPermissionOverride(role).setDeny(
                                Permission.MESSAGE_SEND,
                                Permission.MESSAGE_ADD_REACTION,
                                Permission.VOICE_SPEAK,
                                Permission.VOICE_STREAM,
                                Permission.VOICE_USE_VAD,
                                Permission.CREATE_INSTANT_INVITE,
                                Permission.NICKNAME_CHANGE).queue();
                    }

                    Bot.getGuildConfig(guild).setMuted(role);
                } catch (RuntimeException e) {
                    rethrow(e);
                }
            }
        }

        return role;
    }

    private static class Mute extends Thread {

        protected Member member;
        protected int time;

        private final Role role;
        private final Guild guild;
        private final Message message;
        private final String mention;

        public Mute(Member member, int time, Role role, Message message) {
            this.member = member;
            this.time = time;

            this.role = role;
            this.message = message;
            this.guild = member.getGuild();
            this.mention = member.getAsMention();
        }

        @Override
        public void run() {
            guild.addRoleToMember(member, role).queue(null, (f) -> {throw new BotException(f);});

            try {
                guild.removeRoleFromMember(member, role).completeAfter(time, TimeUnit.SECONDS);
            }
            catch (RuntimeException ok) {
                guild.removeRoleFromMember(member, role).queue();
            }
            finally {
                //Remove this mute from the list of active mutes
                mutes.remove(member.getIdLong());

                message.getChannel().sendMessageEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Unmuted " + mention).build()).queue();
            }
        }

        public Mute begin() {
            start();
            return this;
        }

        public void end() {
            this.interrupt();
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
                new SubcommandData("mute", "Mute a member")
                        .addOption(OptionType.USER, "member", "Member to mute", true)
                        .addOption(OptionType.STRING, "time", "Time of the mute, e.g.: 1d 2h 3m 4s (1h by default)", false),
                new SubcommandData("unmute", "Unmute a member")
                        .addOption(OptionType.USER, "member", "Member to unmute", true)
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
