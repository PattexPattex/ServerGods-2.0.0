package com.pattexpattex.servergods2.commands.slash.moderation;

import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KickCmd extends BotSlash {

    public void run(@NotNull SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS))
            throw new BotException("You have insufficient permissions!");

        Member kickedMember = Objects.requireNonNull(event.getOption("member")).getAsMember();
        String mention = Objects.requireNonNull(kickedMember).getAsMention();

        String reason = event.getOption("reason") != null ? Objects.requireNonNull(event.getOption("reason")).getAsString() : null;

        kickedMember.kick(reason).queue(null, this::rethrow);

        event.replyEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Kicked " + mention + (reason != null ? " with reason: `" + reason + "`" : "")).build()).queue();
    }

    public String getName() {
        return "kick";
    }

    public String getDesc() {
        return "Kicks a member from the server";
    }

    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.USER, "member", "Member to kick", true),
                new OptionData(OptionType.STRING, "reason", "Kick reason", false)
        };
    }
}
