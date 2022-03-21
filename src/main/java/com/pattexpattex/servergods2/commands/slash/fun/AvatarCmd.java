package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.commands.BotSlash;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AvatarCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) throws Exception {
        event.deferReply().queue();

        User user = event.getOption("user") != null ? Objects.requireNonNull(event.getOption("user")).getAsUser() : event.getUser();
        Member member = event.getOption("user") != null ? Objects.requireNonNull(event.getOption("user")).getAsMember() : event.getMember();

        String url;
        if (member == null) {
            url = user.getEffectiveAvatarUrl();
        }
        else {
            url = member.getEffectiveAvatarUrl();
        }

        event.getHook().editOriginal("**" + user.getAsTag() + "'s** avatar:\n" + url + "?size=4096").queue();
    }

    @Override
    public String getName() {
        return "avatar";
    }

    @Override
    public String getDesc() {
        return "Get an avatar from a user";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.USER, "user", "The user", false)
        };
    }
}
