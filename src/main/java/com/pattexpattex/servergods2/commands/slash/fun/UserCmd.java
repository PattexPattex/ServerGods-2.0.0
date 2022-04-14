package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class UserCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        Member member = event.getOption("user") != null ? Objects.requireNonNull(event.getOption("user")).getAsMember() : event.getMember();
        User user = event.getOption("user") != null ? Objects.requireNonNull(event.getOption("user")).getAsUser() : event.getUser();

        if (member == null) {
            renderUserEmbed(event, user);
            return;
        }

        renderMemberEmbed(event, member, event.getGuild());
    }

    private void renderUserEmbed(SlashCommandEvent event, User user) {
        OffsetDateTime time = user.getTimeCreated();

        MessageEmbed embed = FormatUtil.defaultEmbed(null, "User info")
                .setThumbnail(user.getEffectiveAvatarUrl())
                .appendDescription("User info for " + user.getAsMention() + ":")
                .appendDescription("\n\n**User Tag:** " + user.getAsTag())
                .appendDescription("\n**User Id:** " + user.getId())
                .appendDescription("\n**Account Created:** " + FormatUtil.epochTimestamp(time.toEpochSecond()) + " (" + FormatUtil.epochTimestampRelative(time.toEpochSecond()) + ")")
                .appendDescription("\n" + nitroUserLink + (isNitro(user) ? Emotes.YES : Emotes.WARNING))
                .appendDescription("\n**Bot Account:** " + (user.isBot() ? Emotes.YES : Emotes.WARNING))
                .appendDescription("\n\n_Use `/avatar <user>` to get a user's avatar_")
                .build();

        event.getHook().editOriginalEmbeds(embed).queue();
    }

    private void renderMemberEmbed(SlashCommandEvent event, Member member, Guild guild) {
        User user = member.getUser();
        OffsetDateTime userTime = user.getTimeCreated();
        OffsetDateTime memberTime = member.getTimeJoined();

        String boostingSince = "Never";
        if (member.getTimeBoosted() != null) {
            OffsetDateTime boostTime = member.getTimeBoosted();

            boostingSince = FormatUtil.epochTimestamp(boostTime.toEpochSecond()) + " (" + FormatUtil.epochTimestampRelative(boostTime.toEpochSecond()) + ")";
        }

        List<Member> loadedMembers = guild.getMemberCache().asList();

        MessageEmbed embed = FormatUtil.defaultEmbed(null, "User info")
                .setThumbnail(user.getEffectiveAvatarUrl())
                .appendDescription("User info for " + user.getAsMention() + ":")
                .appendDescription("\n\n**User Tag:** " + user.getAsTag())
                .appendDescription("\n**User Id:** " + user.getId())
                .appendDescription("\n**Display Name:** " + member.getEffectiveName())
                .appendDescription("\n**Account Created:** " + FormatUtil.epochTimestamp(userTime.toEpochSecond()) + " (" + FormatUtil.epochTimestampRelative(userTime.toEpochSecond()) + ")")
                .appendDescription("\n**Joined Server:** " + FormatUtil.epochTimestamp(memberTime.toEpochSecond()) + " (" + FormatUtil.epochTimestampRelative(memberTime.toEpochSecond()) + ")")
                .appendDescription("\n**Join Position:** " + getJoinPosition(loadedMembers, member))
                .appendDescription("\n**Join Order:** " + generateJoinOrder(loadedMembers, member))
                .appendDescription("\n" + nitroUserLink + (isNitro(user) ? Emotes.YES : Emotes.WARNING))
                .appendDescription("\n**Boosting Since:** " + boostingSince)
                .appendDescription("\n**Bot Account:** " + (user.isBot() ? Emotes.YES : Emotes.WARNING))
                .appendDescription("\n\n_Use `/avatar <user>` to get a user's avatar_").build();

        event.getHook().editOriginalEmbeds(embed).queue();
    }

    private String generateJoinOrder(List<Member> members, Member member) {
        StringBuilder sb = new StringBuilder();
        List<Member> joins = members.stream().sorted(Comparator.comparing(Member::getTimeJoined)).toList();
        int in = joins.indexOf(member);
        in -= 3;

        if (in < 0) {
            in = 0;
        }

        if (joins.get(in) == member) {
            sb.append("**").append(joins.get(in).getEffectiveName()).append("**");
        }
        else {
            sb.append(joins.get(in).getEffectiveName());
        }

        for (int i = in + 1; i < in + 7; i++) {
            if (i >= joins.size()) {
                break;
            }

            Member m = joins.get(i);
            String un = m.getEffectiveName();

            if (m == member) {
                un = "**" + un + "**";
            }

            sb.append(" \\> ").append(un);
        }

        return sb.toString();
    }

    private int getJoinPosition(List<Member> members, Member member) {
        return (int) members.stream().sorted(Comparator.comparing(Member::getTimeJoined)).takeWhile((p) -> p != member).count() + 1;
    }

    private final String nitroUserLink = "**[Nitro User: ](https://github.com/DuncteBot/SkyBot/issues/201#issuecomment-486182959 \"Click for more info on the nitro user check\")**";

    public static boolean isNitro(User user) {
        return user.retrieveProfile().complete().getBannerId() != null || user.getAvatarId() != null && user.getAvatarId().startsWith("a_");
    }

    @Override
    public String getName() {
        return "user";
    }

    @Override
    public String getDesc() {
        return "Get info about a user";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.USER, "user", "The user", false)
        };
    }
}
