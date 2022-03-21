package com.pattexpattex.servergods2.commands.slash.moderation;

import com.pattexpattex.servergods2.core.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class WakeUpCmd extends BotSlash {

    public void run(@NotNull SlashCommandEvent event) {

        event.deferReply().queue();

        Guild guild = Objects.requireNonNull(event.getGuild());

        Member moveMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
        AudioChannel currentChannel = Objects.requireNonNull(Objects.requireNonNull(moveMember).getVoiceState()).getChannel();
        AudioChannel moveChannel = null;
        List<VoiceChannel> allChannels = guild.getVoiceChannels();

        String mention = moveMember.getAsMention();

        for (AudioChannel channel : allChannels) {
            if (channel != currentChannel) {
                moveChannel = channel;
                break;
            }
        }

        if (currentChannel == null) {
            throw new BotException("Member is not in a voice channel");
        }

        if (moveChannel == null) {
            throw new BotException("No second voice channel found");
        }

        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.VOICE + " Moving " + mention + " around a bit").build()).queue();

        for (int i = 0; i < 5; i++) {
            guild.moveVoiceMember(moveMember, moveChannel).completeAfter(500L, TimeUnit.MILLISECONDS);
            guild.moveVoiceMember(moveMember, currentChannel).completeAfter(500L, TimeUnit.MILLISECONDS);
        }

        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Moved " + mention + " around a bit and you can do it again, if you want, of course").build()).queue();
    }

    public String getName() {
        return "wake";
    }

    public String getDesc() {
        return "Moves a member around voice channels until he wakes up";
    }

    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.USER, "user", "Who to wake", true)
        };
    }

    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.VOICE_MOVE_OTHERS
        };
    }
}
