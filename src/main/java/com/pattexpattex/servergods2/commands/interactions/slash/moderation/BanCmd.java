package com.pattexpattex.servergods2.commands.interactions.slash.moderation;

import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BanCmd implements BotSlash {

    public void run(@NotNull SlashCommandEvent event) {

        Member bannedMember = Objects.requireNonNull(event.getOption("member")).getAsMember();
        String mention = Objects.requireNonNull(bannedMember).getAsMention();

        int delDays = event.getOption("delete_days") != null ? (int) Objects.requireNonNull(event.getOption("delete_days")).getAsLong() : 0;
        String reason = event.getOption("reason") != null ? Objects.requireNonNull(event.getOption("reason")).getAsString() : null;

        Checks.notNegative(delDays, "Option deleteDays");


        bannedMember.ban(delDays, reason).queue(null, this::rethrow);


        event.replyEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Banned " + mention + (reason != null ? " with reason: `" + reason + "`": "")).build()).queue();

    }

    public String getName() {
        return "ban";
    }

    public String getDesc() {
        return "Bans a member from the server";
    }

    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.USER, "member", "Who to ban", true),
                new OptionData(OptionType.INTEGER, "delete_days", "Age of banned members' messages to delete in days (0 to not delete any)", false),
                new OptionData(OptionType.STRING, "reason", "Ban reason", false)
        };
    }
}
