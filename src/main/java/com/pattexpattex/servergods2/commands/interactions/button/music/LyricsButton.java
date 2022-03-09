package com.pattexpattex.servergods2.commands.interactions.button.music;

import com.jagrosh.jlyrics.Lyrics;
import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.Kvintakord;
import com.pattexpattex.servergods2.commands.interactions.button.BotButton;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class LyricsButton implements BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) throws Exception {
        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferReply(Kvintakord.lastQueueMessageExists(guild)).queue();

        if (Kvintakord.isNotLastQueueMessage(guild, event.getMessage()) || !Kvintakord.isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).complete()
                    .editMessageComponents(Collections.emptyList()).queue();
            return;
        }

        String name;
        AudioTrack track = Objects.requireNonNull(Kvintakord.getCurrentTrack(guild));

        if (Kvintakord.isSpotifyTrack(track)) {
            name = Kvintakord.getTrackName(track) + " " + Kvintakord.getTrackAuthor(track);
        }
        else {
            name = Kvintakord.getTrackName(track);
        }

        Lyrics lyrics = Kvintakord.lyricsFor(name);

        if (lyrics == null) {
            throw new BotException("Lyrics not found");
        }

        EmbedBuilder embed = FormatUtil.kvintakordEmbed(lyrics.getContent())
                .setTitle(lyrics.getTitle() + " by " + lyrics.getAuthor(), lyrics.getURL())
                .setAuthor("Provided by " + Bot.getConfig().getLyricsProvider(), FormatUtil.getLyricsProviderUrl());

        if (lyrics.getContent().length() > 15000) {
            throw new BotException("Lyrics for " + name + " found but likely not correct: " + lyrics.getURL());
        }
        else if (lyrics.getContent().length() > 2000) {
            String content = lyrics.getContent().trim();

            while (content.length() > 2000) {
                int index = content.lastIndexOf("\n\n", 2000);

                if (index == -1) {
                    index = content.lastIndexOf("\n", 2000);
                }
                if (index == -1) {
                    index = content.lastIndexOf(" ", 2000);
                }
                if (index == -1) {
                    index = 2000;
                }

                content = content.substring(0, index);
                content = content.substring(index).trim();
            }

            event.getHook().sendMessageEmbeds(embed.setDescription(content).build()).queue();
        }
        else {
            event.getHook().sendMessageEmbeds(embed.setDescription(lyrics.getContent()).build()).queue();
        }
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.SECONDARY;
    }

    @Override
    public @Nullable Emoji getEmoji() {
        return Emoji.fromUnicode("\uD83D\uDCDC");
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Lyrics";
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.lyrics";
    }
}
