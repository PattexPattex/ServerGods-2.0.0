package com.pattexpattex.servergods2.core.listeners;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.core.kvintakord.discord.AloneInVoiceHandler;
import com.pattexpattex.servergods2.core.mute.Mute;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Here are registered other events, such as when the bot joins a new guild, or a new member joins to a guild,
 * and for handling deleted roles and channels
 * @since 1.0.0, but combined with {@link HiddenEventListener}
 * */
public class MiscEventListener extends ListenerAdapter {

    private static final String MEMBER_WELCOME = """
            <:rules:934439539299221504> **First a few rules:**
            > <:no:934441383136211036> Don't be an absolute troll,
            > <:text_channel:934505784144986232> No content outside channels that are made for it
            > <:muted:934505784044290088> Pls don't earrape, people like their ears
            > <:members:934505784195301376> Could you not insult people, nobody likes it?
            
            """;

    private static final String MEMBER_WELCOME_ROLES = """
            <:role:934439539328577616> **Select your cosmetic roles via the `/roles` command!**
            """;

    private static final String MEMBER_WELCOME_ABOUT = """
            <:slash:934441817234100304> **Check out what I can do with `/about`!**
            """;

    private static final String MEMBER_WELCOME_INVITE = """
            <:invite:934441048204259429> **Invite your friends quickly with `/invite`!**
            """;

    private static final String MEMBER_WELCOME_ENJOY = """
            
            <:accepted:934440305434963969> **Enjoy your stay here!**""";

    private static final String BOT_JOIN = """
            I'm a generic Discord bot that can do a bunch, developed by **PattexPattex**! Use **/about** to see what I can!
            
            *If you are the owner of this server, please check my Github repository for instructions on how to set me up properly!*
            """;

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        // Iterates through the TextChannels in a guild
        for (TextChannel channel : event.getGuild().getTextChannels()) {

            // If the bot can talk in the channel
            if (channel.canTalk()) {

                // A nice welcome message
                channel.sendMessageEmbeds(FormatUtil.defaultEmbed(
                        BOT_JOIN, "Hello, I'm " + Bot.getJDA().getSelfUser().getName() + "!", "DEFAULT", null)
                        .build())
                        .setActionRows(ActionRow.of(
                                Button.link("https://github.com/PattexPattex/ServerGods", "Github")
                                        .withEmoji(Emoji.fromEmote("github", 934755405802930176L, false))))
                        .queue();

                // Breaks the loop
                break;
            }
        }

        SlashEventListener.getEnabledCommands().forEach((cmd) -> cmd.setup(cmd));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {

        // The TextChannel of a guild to send welcome messages to
        TextChannel channel = Bot.getGuildConfig(event.getGuild()).getWelcome(event.getGuild());
        Guild guild = event.getGuild();
        List<String> commands = Bot.getGuildConfig(guild).getCommands();

        EmbedBuilder builder = FormatUtil.defaultEmbed(MEMBER_WELCOME,
                event.getMember().getEffectiveName() + ", welcome to " + event.getGuild().getName() + "!",
                event.getMember().getEffectiveAvatarUrl(), null);

        if (commands.contains("roles")) {
            builder.appendDescription(MEMBER_WELCOME_ROLES);
        }

        if (commands.contains("about")) {
            builder.appendDescription(MEMBER_WELCOME_ABOUT);
        }

        if (commands.contains("invite")) {
            builder.appendDescription(MEMBER_WELCOME_INVITE);
        }

        builder.appendDescription(MEMBER_WELCOME_ENJOY);

        if (channel != null) {
            channel.sendMessageEmbeds(builder.build()).queue();

            FormatUtil.mentionUserAndDelete(event.getUser(), channel);
        }
    }


    /* ---- Update the config if something is updated ---- */

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        Guild guild = event.getGuild();

        //Cosmetic roles
        {
            List<String> roles = Bot.getGuildConfig(guild).getFunRoles();
            Role deletedRole = event.getRole();

            roles.remove(deletedRole.getId());

            Bot.getGuildConfig(guild).setFunRoles(roles);
        }

        //Muted role
        {
            Role oldRole = Bot.getGuildConfig(guild).getMuted(guild);
            Role deletedRole = event.getRole();

            if (oldRole != null && oldRole.getIdLong() == deletedRole.getIdLong()) {
                Bot.getGuildConfig(guild).setMuted(null);
            }
        }

        //Giveaway role
        {
            Role oldRole = Bot.getGuildConfig(guild).getGiveaway(guild);
            Role deletedRole = event.getRole();

            if (oldRole != null && oldRole.getIdLong() == deletedRole.getIdLong()) {
                Bot.getGuildConfig(guild).setGiveaway(null);
            }
        }

        //Poll role
        {
            Role oldRole = Bot.getGuildConfig(guild).getPoll(guild);
            Role deletedRole = event.getRole();

            if (oldRole != null && oldRole.getIdLong() == deletedRole.getIdLong()) {
                Bot.getGuildConfig(guild).setPoll(null);
            }
        }
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        Guild guild = event.getGuild();

        //Welcome channel
        {
            TextChannel channel = Bot.getGuildConfig(guild).getWelcome(guild);
            Channel deletedChannel = event.getChannel();

            if (channel != null && channel.getIdLong() == deletedChannel.getIdLong()) {
                Bot.getGuildConfig(guild).setWelcome(null);
            }
        }
    }

    @Override
    public void onGuildInviteDelete(@NotNull GuildInviteDeleteEvent event) {
        Guild guild = event.getGuild();

        {
            String invite = Bot.getGuildConfig(guild).getInvite();
            String deletedInvite = event.getUrl();

            if (invite != null && invite.equals(deletedInvite)) {
                Bot.getGuildConfig(guild).setInvite(null);
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        List<Role> roles = event.getRoles();
        Guild guild = event.getGuild();
        long id = event.getMember().getIdLong();

        if (!roles.contains(Bot.getGuildConfig(guild).getMuted(guild))) return;

        Mute mute = Bot.getMuteManager().getMute(id);
        if (mute != null) {
            mute.end();
        }
    }

    @Override
    public void onGuildVoiceGuildDeafen(@NotNull GuildVoiceGuildDeafenEvent event) {

        //Some people deafen their music bots, like they are recording them or something
        Member member = event.getMember();
        Guild guild = event.getGuild();
        GuildVoiceState state = Objects.requireNonNull(member.getVoiceState());

        if (member.getUser() == Bot.getJDA().getSelfUser()) {
            if (!state.isGuildDeafened()) return;
            if (!state.inAudioChannel()) return;

            member.deafen(false).queue();
            guild.getAudioManager().setSelfDeafened(true);
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {

        //Make sure to update the giveaway list
        Guild guild = event.getGuild();
        long id = event.getMessageIdLong();

        Bot.getGiveawayManager().removeGiveaway(id);
        Bot.getGiveawayManager().writeGiveaways();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Bot.getGiveawayManager().resumeActiveGiveaways();
        Bot.getMuteManager().resumeActiveMutes();
    }

    @Override public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (event.getMember() == event.getGuild().getSelfMember()) {
            Bot.getKvintakord().stop(event.getGuild());
        }
    }

    @Override public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Bot.getKvintakord().getAloneInVoiceHandler().onVoiceUpdate(event);
    }
}
