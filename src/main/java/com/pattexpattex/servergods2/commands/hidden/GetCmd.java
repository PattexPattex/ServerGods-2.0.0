package com.pattexpattex.servergods2.commands.hidden;

import com.pattexpattex.servergods2.commands.slash.fun.UserCmd;
import com.pattexpattex.servergods2.core.commands.BotHidden;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GetCmd extends BotHidden {

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        String id = args[0];
        JDA jda = event.getJDA();
        EmbedBuilder builder = FormatUtil.defaultEmbed(null, "Info for Snowflake `" + id + "`");

        @Nullable ISnowflake snowflake = null;

        for (int i = 0; i < 5; i++) {
            if (i == 0) snowflake = jda.getUserById(id);
            else if (i == 1 && snowflake == null) snowflake = jda.getGuildById(id);
            else if (i == 2 && snowflake == null) snowflake = jda.getRoleById(id);
            else if (i == 3 && snowflake == null) snowflake = jda.getTextChannelById(id);
            else if (snowflake == null) snowflake = jda.getVoiceChannelById(id);
        }

        if (snowflake == null) {
            throw new RuntimeException("Snowflake not found");
        }

        if (snowflake instanceof User user) {
            builder.appendDescription("Snowflake type `user`\n")
                    .appendDescription("\n**Tag:** " + user.getAsTag())
                    .appendDescription("\n**Created:** " + FormatUtil.epochTimestamp(user.getTimeCreated().toEpochSecond()))
                    .appendDescription("\n**Avatar:** " + user.getEffectiveAvatarUrl())
                    .appendDescription("\n**Bot Account:** " + user.isBot())
                    .appendDescription("\n**Nitro User:**" + UserCmd.isNitro(user));
        }
        else if (snowflake instanceof Guild guild) {
            builder.appendDescription("Snowflake type `guild`\n")
                    .appendDescription("\n**Name:** " + guild.getName())
                    .appendDescription("\n**Owner Id:** " + guild.getOwnerId())
                    .appendDescription("\n**Member Count:** " + guild.getMemberCount())
                    .appendDescription("\n**Created:** " + FormatUtil.epochTimestamp(guild.getTimeCreated().toEpochSecond()))
                    .appendDescription("\n**Region:** " + FormatUtil.formatArray(guild.retrieveRegions().complete()))
                    .appendDescription("\n**Features:** " + FormatUtil.formatArray(guild.getFeatures()));
        }
        else if (snowflake instanceof Role role) {
            List<String> permissions = new ArrayList<>();
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getName());
            }

            builder.appendDescription("Snowflake type `role`\n")
                    .appendDescription("\n**Name:** " + role.getName())
                    .appendDescription("\n**Guild Id:** " + role.getGuild().getId())
                    .appendDescription("\n**Color:** " + (role.getColor() == null ? "None" : role.getColor().getRGB()))
                    .appendDescription("\n**Created:** " + FormatUtil.epochTimestamp(role.getTimeCreated().toEpochSecond()))
                    .appendDescription("\n**Position:** " + role.getPosition())
                    .appendDescription("\n**Member Count:** " + role.getGuild().getMembersWithRoles(role).size())
                    .appendDescription("\n**Managed:** " + role.isManaged())
                    .appendDescription("\n**Bot Role:** " + role.getTags().isBot())
                    .appendDescription("\n**Boost Role:** " + role.getTags().isBoost())
                    .appendDescription("\n**Integration Role:** " + role.getTags().isIntegration())
                    .appendDescription("\n**Hoisted:** " + role.isHoisted())
                    .appendDescription("\n**Mentionable:** " + role.isMentionable())
                    .appendDescription("\n**Permissions:**" + FormatUtil.formatArray(permissions));
        }
        else if (snowflake instanceof TextChannel channel) {
            List<String> members = new ArrayList<>();
            for (Member member : channel.getMembers()) {
                members.add(member.getUser().getAsTag());
            }

            builder.appendDescription("Snowflake type `textChannel`\n")
                    .appendDescription("\n**Name:** " + channel.getName())
                    .appendDescription("\n**Description:** " + channel.getTopic())
                    .appendDescription("\n**Category:** " + (channel.getParentCategory() == null ? "None" : channel.getParentCategory().getName()))
                    .appendDescription("\n**Guild Id:** " + channel.getGuild().getId())
                    .appendDescription("\n**Position**: " + channel.getPosition())
                    .appendDescription("\n**Created:** " + FormatUtil.epochTimestamp(channel.getTimeCreated().toEpochSecond()))
                    .appendDescription("\n**Slowmode:** " + channel.getSlowmode())
                    .appendDescription("\n**NSFW:** " + channel.isNSFW())
                    .appendDescription("\n**Synced:** " + channel.isSynced())
                    .appendDescription("\n**History Size:** " + channel.getHistory().size())
                    .appendDescription("\n**Permission Overrides:** " + channel.getPermissionOverrides().size())
                    .appendDescription("\n**Members:** " + FormatUtil.formatArray(members))
                    .appendDescription("\n**Invites:** " + FormatUtil.formatArray(channel.retrieveInvites().complete()));
        }
        else {
            VoiceChannel channel = (VoiceChannel) snowflake;
            List<String> members = new ArrayList<>();
            for (Member member : channel.getMembers()) {
                members.add(member.getUser().getAsTag());
            }

            builder.appendDescription("Snowflake type `voiceChannel`\n")
                    .appendDescription("\n**Name:** " + channel.getName())
                    .appendDescription("\n**Category:** " + (channel.getParentCategory() == null ? "None" : channel.getParentCategory().getName()))
                    .appendDescription("\n**Guild Id:** " + channel.getGuild().getId())
                    .appendDescription("\n**Position**: " + channel.getPosition())
                    .appendDescription("\n**Created:** " + FormatUtil.epochTimestamp(channel.getTimeCreated().toEpochSecond()))
                    .appendDescription("\n**Synced:** " + channel.isSynced())
                    .appendDescription("\n**Region:** " + channel.getRegion().getName())
                    .appendDescription("\n**Permission Overrides:** " + channel.getPermissionOverrides().size())
                    .appendDescription("\n**Currently Connected:** " + FormatUtil.formatArray(members))
                    .appendDescription("\n**User Limit:** " + channel.getUserLimit())
                    .appendDescription("\n**Invites:** " + FormatUtil.formatArray(channel.retrieveInvites().complete()));
        }

        event.getChannel().sendMessageEmbeds(builder.build()).queue();
    }
}
