package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RickrollCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {

        event.deferReply(true).queue(null, this::rethrow);
        Guild guild = Objects.requireNonNull(event.getGuild());
        String commandPath = event.getCommandPath();

        switch (commandPath) {
            case "rickroll/start" -> {
                Member member = event.getOption("member") != null ? Objects.requireNonNull(Objects.requireNonNull(event.getOption("member")).getAsMember()) : Objects.requireNonNull(event.getMember());
                AudioChannel audioChannel = Objects.requireNonNull(member.getVoiceState()).getChannel();

                if (audioChannel == null) {
                    throw new BotException("The user is not connected to a voice channel");
                }

                Bot.getKvintakord().loadAndPlay(audioChannel, "https://www.youtube.com/watch?v=dQw4w9WgXcQ", true);

                if (member == event.getMember()) {
                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Rickrolled " + audioChannel.getAsMention()).build()).queue(null, this::rethrow);
                }
                else {
                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Rickrolled " + member.getAsMention() + " in " + audioChannel.getAsMention()).build()).queue(null, this::rethrow);
                }

                new Thread(() -> {
                    while (Bot.getKvintakord().getCurrentTrack(guild) == null)
                        Thread.onSpinWait();

                    Bot.getKvintakord().getCurrentTrack(guild).setMarker(new TrackMarker(62000, state -> Bot.getKvintakord().skipToTrack(0, guild)));
                }).start();
            }
            case "rickroll/stop" -> {
                if (Bot.getKvintakord().getDiscordManager().currentVoiceChannel(guild) == null) {
                    throw new BotException("I am not connected to any voice channel");
                }
                else {
                    AudioChannel channel = Objects.requireNonNull(Bot.getKvintakord().getDiscordManager().currentVoiceChannel(guild));

                    Bot.getKvintakord().skipToTrack(0, guild);

                    event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Stopped rickrolling in " + channel.getAsMention()).build()).queue(null, this::rethrow);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "rickroll";
    }

    @Override
    public String getDesc() {
        return "Joins your voice channel and starts playing Never gonna give you up";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("start", "Start a rickroll")
                        .addOption(OptionType.USER, "member", "Who to rickroll", false),
                new SubcommandData("stop", "Stop a rickroll")
        };
    }
}
