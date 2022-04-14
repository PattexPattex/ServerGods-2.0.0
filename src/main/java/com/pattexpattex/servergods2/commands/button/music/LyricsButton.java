package com.pattexpattex.servergods2.commands.button.music;

import com.jagrosh.jlyrics.Lyrics;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class LyricsButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) throws Exception {
        Kvintakord kvintakord = Bot.getKvintakord();
        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferReply(Bot.getKvintakord().getDiscordManager().lastQueueMessageExists(guild)).queue();

        if (Bot.getKvintakord().getDiscordManager().isNotLastQueueMessage(guild, event.getMessage()) || !Bot.getKvintakord().isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(Emotes.YES + " Interaction ended").build()).setActionRows(Collections.emptyList()).queue();
            return;
        }

        AudioTrack track = Objects.requireNonNull(kvintakord.getCurrentTrack(guild));
        String name = track.getInfo().title + " " + track.getInfo().author;

        Lyrics lyrics = kvintakord.lyricsFor(name);

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
    public @Nullable net.dv8tion.jda.api.entities.Emoji getEmoji() {
        return net.dv8tion.jda.api.entities.Emoji.fromUnicode("\uD83D\uDCDC");
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
