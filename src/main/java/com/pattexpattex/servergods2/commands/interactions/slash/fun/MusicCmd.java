package com.pattexpattex.servergods2.commands.interactions.slash.fun;

import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jlyrics.Lyrics;
import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.Kvintakord;
import com.pattexpattex.servergods2.commands.interactions.button.music.*;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.listeners.interactions.SlashEventListener;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Consume combined with Kvintakord.
 */
public class MusicCmd implements BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) throws Exception {

        String subcommand = event.getCommandPath();
        Member member = event.getMember();
        Guild guild = Objects.requireNonNull(event.getGuild());

        Kvintakord.registerAudioEventListener(guild, new Kvintakord.AudioEventListener() {

            @Override public void onTrackStart(AudioTrack track) {
                MessageEmbed embed = FormatUtil.kvintakordEmbed(BotEmoji.YES + " Started playing " + FormatUtil.formatTrackLink(track)).build();

                if (Kvintakord.updateLastQueueMessage(guild)) {
                    return;
                }

                event.getChannel().sendMessageEmbeds(embed).queue();
            }

            @Override public void onTrackQueue(AudioTrack track) {
                MessageEmbed embed = FormatUtil.kvintakordEmbed(BotEmoji.YES + " Added to queue " + FormatUtil.formatTrackLink(track)).build();

                if (Kvintakord.updateLastQueueMessage(guild)) {
                    return;
                }

                if (Kvintakord.updateLastTrackQueuedMessage(guild, embed)) {
                    return;
                }

                event.getChannel().sendMessageEmbeds(embed).queue((msg) -> Kvintakord.setLastTrackQueuedMessage(guild, msg));
            }

            @Override public void onTrackNoMatches(String id) {
                event.getChannel().sendMessageEmbeds(FormatUtil.errorEmbed(new BotException("No matches for query: " + id), SlashEventListener.class).build()).queue();
            }

            @Override public void onTrackLoadFail(FriendlyException exception) {
                event.getChannel().sendMessageEmbeds(FormatUtil.errorEmbed(new BotException("Track loading failed: " + exception.getMessage(), exception), SlashEventListener.class).build()).queue();
            }

            @Override public void onDisconnectFromAudioChannelBecauseEmpty(AudioChannel channel) {
                MessageEmbed embed = FormatUtil.kvintakordEmbed("\uD83C\uDF99 Disconnected from " + channel.getAsMention() + " because it was empty").build();

                event.getChannel().sendMessageEmbeds(embed).queue();
            }
        });

        Checks.notNull(guild, "Guild");
        Checks.notNull(member, "Member");

        //This should never throw an Exception, because CacheFlag.VOICE_STATE is enabled
        Checks.notNull(member.getVoiceState(), "GuildVoiceState");

        switch (subcommand) {
            case "music/play", "music/playnext" -> {
                event.deferReply(true).queue();

                String query = event.getOption("query") != null ? Objects.requireNonNull(event.getOption("query")).getAsString() : null;
                AudioChannel channel = member.getVoiceState().getChannel();
                boolean playFirst = subcommand.equals("music/playnext");

                Checks.notNull(query, "Query");
                if (channel == null) {
                    throw new BotException("You are not in a voice channel");
                }

                Kvintakord.removeLastTrackQueuedMessage(guild);

                try {
                    Kvintakord.loadAndPlay(channel, query, playFirst);
                }
                catch (Exception e) {
                    throw new BotException(e);
                }

                event.getHook().editOriginal(BotEmoji.YES).queue();
            }
            case "music/stop" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                Kvintakord.stop(guild);

                event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Stopped and cleared the queue").build()).queue();
            }
            case "music/pause", "music/resume" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                if (Kvintakord.pause(guild)) {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Paused playback").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }
                else {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Resumed playback").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/volume" -> {

                Integer vol = event.getOption("volume") != null ? (int) Objects.requireNonNull(event.getOption("volume")).getAsLong() : null;

                if (vol == null) {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Volume is `" + Kvintakord.getVolume(guild)+ "`").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }
                else {
                    Kvintakord.setVolume(guild, vol);

                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Set volume to `" + vol + "`").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/loop" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                Boolean singleLoop = event.getOption("single") != null ? Objects.requireNonNull(event.getOption("single")).getAsBoolean() : null;

                if (singleLoop != null) {
                    if (singleLoop) {
                        Kvintakord.loopTrack(guild, Kvintakord.LoopMode.SINGLE);
                        event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Single track loop enabled").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                    }
                    else {
                        Kvintakord.loopTrack(guild, Kvintakord.LoopMode.OFF);
                        event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Loop disabled").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                    }
                }
                else {
                    switch (Kvintakord.isLooping(guild)) {
                        case OFF, SINGLE -> {
                            Kvintakord.loopTrack(guild, Kvintakord.LoopMode.ALL);
                            event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Queue loop enabled").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                        }
                        case ALL -> {
                            Kvintakord.loopTrack(guild, Kvintakord.LoopMode.OFF);
                            event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Loop disabled").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                        }
                    }
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/queue" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                event.deferReply().queue();

                Message oldMessage = Kvintakord.removeLastQueueMessage(guild);

                event.getHook().editOriginalEmbeds(FormatUtil.getQueueEmbed(guild)).complete()
                        .editMessageComponents(
                                ActionRow.of(new PauseButton(), new SkipButton(), new LoopButton(), new StopButton(), new LyricsButton()),
                                ActionRow.of(new RefreshButton(), new ClearButton(), new DestroyButton())
                        ).queue((msg) -> Kvintakord.setLastQueueMessage(guild, msg));

                if (oldMessage != null) {
                    oldMessage.editMessageEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).complete()
                            .editMessageComponents(Collections.emptyList()).queue();
                }
            }
            case "music/skip" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                int location = event.getOption("to") != null ? (int) Objects.requireNonNull(event.getOption("to")).getAsLong() : 1;

                if (!Kvintakord.skipToTrack(location - 1, guild)) {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Stopped and disconnected; no tracks were queued").build()).queue();
                }
                else {
                    if (location != 1) {
                        event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Skipped to `" + location + "`").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                    }
                    else {
                        event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Skipped current track").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                    }
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/seek" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                String input = event.getOption("time") != null ? Objects.requireNonNull(event.getOption("time")).getAsString() : null;

                if (!Kvintakord.seekTo(FormatUtil.decodeTime(input), guild)) {
                    throw new BotException("Seek is not supported by this track");
                }
                else {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Started playing from " + input).build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/remove" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                int location = (int) Objects.requireNonNull(event.getOption("location")).getAsLong();
                AudioTrack track = Kvintakord.getTrackAt(location - 1, guild);

                if (Kvintakord.removeTrack(location - 1, guild)) {
                    event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Removed " + FormatUtil.formatTrackLink(track) + " at `" + location + "`").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();
                }
                else {
                    throw new BotException("Removal failed");
                }

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/move" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                int from = (int) Objects.requireNonNull(event.getOption("from")).getAsLong();
                int to = (int) Objects.requireNonNull(event.getOption("to")).getAsLong();

                AudioTrack track = Kvintakord.getTrackAt(from - 1, guild);
                Kvintakord.moveTrack(from - 1, to - 1, guild);

                event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Moved " + FormatUtil.formatTrackLink(track) + " from `" + from + "` to `" + to + "`").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();

                Kvintakord.updateLastQueueMessage(guild);
            }
            case "music/lyrics" -> {

                event.deferReply(Kvintakord.lastQueueMessageExists(guild)).queue();

                String name = event.getOption("name") != null ? Objects.requireNonNull(event.getOption("name")).getAsString() : null;

                if (name == null) {
                    Kvintakord.isPlayingElseThrowException(guild);
                    AudioTrack track = Objects.requireNonNull(Kvintakord.getCurrentTrack(guild));

                    if (Kvintakord.isSpotifyTrack(track)) {
                        name = Kvintakord.getTrackName(track) + " " + Kvintakord.getTrackAuthor(track);
                    }
                    else {
                        name = Kvintakord.getTrackName(track);
                    }
                }

                Lyrics lyrics = Kvintakord.lyricsFor(name);

                if (lyrics == null) {
                    throw new BotException("Lyrics not found, have you tried /music lyrics <name>?");
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

                    event.getHook().editOriginalEmbeds(embed.setDescription(content).build()).queue();
                }
                else {
                    event.getHook().editOriginalEmbeds(embed.setDescription(lyrics.getContent()).build()).queue();
                }
            }
            case "music/search" -> {

                String query = event.getOption("query") != null ? Objects.requireNonNull(event.getOption("query")).getAsString() : null;
                Checks.notNull(query, "Query");

                OrderedMenu.Builder builder = new OrderedMenu.Builder()
                        .allowTextInput(true)
                        .useCancelButton(true)
                        .setEventWaiter(Bot.getWaiter())
                        .setColor(FormatUtil.KVINTAKORD_COLOR)
                        .setTimeout(1, TimeUnit.MINUTES);

                event.deferReply(true).queue();

                Kvintakord.getPlayerManager().loadItemOrdered(guild, "ytsearch: " + query, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        EmbedBuilder embed = FormatUtil.kvintakordEmbed(FormatUtil.formatTrackLink(track), "Search result for: `" + query + "`");

                        event.getHook().editOriginalEmbeds(embed.build()).queue();

                        if (Kvintakord.isPlaying(guild)) {
                            Kvintakord.play(Kvintakord.currentVoiceChannel(guild), Kvintakord.getGuildAudioPlayer(guild), track);

                            Kvintakord.updateLastQueueMessage(guild);
                        }
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        AudioChannel channel = member.getVoiceState().getChannel();

                        builder.setText("**Search results for: `" + query + "`**")
                                .setSelection((msg, i) -> {
                                    AudioTrack track = playlist.getTracks().get(i - 1);

                                    if (channel == null) {
                                        throw new BotException("You are not in a voice channel");
                                    }
                                    Kvintakord.play(channel, Kvintakord.getGuildAudioPlayer(guild), track);

                                    Kvintakord.updateLastQueueMessage(guild);
                                })
                                .setCancel((msg) -> {})
                                .setUsers(event.getUser());

                        for (int i = 0; i < 5 && i < playlist.getTracks().size(); i++) {
                            AudioTrack track = playlist.getTracks().get(i);

                            builder.addChoice("**" + (i + 1) + ".** [" + FormatUtil.formatTime(track.getDuration()) + "] " + FormatUtil.formatTrackLink(track) + "\n");
                        }

                        event.getHook().editOriginal(BotEmoji.YES).complete();
                        builder.build().display(event.getChannel());
                    }

                    @Override
                    public void noMatches() {
                        event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.NO + " No results for query: `" + query + "`").build()).queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        if (e.severity == FriendlyException.Severity.COMMON) {
                            throw new BotException("Something broke while loading", e);
                        }
                        else {
                            throw new BotException("Something broke while loading");
                        }
                    }
                });
            }
            case "music/clear" -> {
                Kvintakord.isPlayingElseThrowException(guild);

                Kvintakord.clearQueue(guild);

                event.replyEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Cleared the queue").build()).setEphemeral(Kvintakord.lastQueueMessageExists(guild)).queue();

                Kvintakord.updateLastQueueMessage(guild);
            }
        }
    }

    @Override
    public String getName() {
        return "music";
    }

    @Override
    public String getDesc() {
        return "A command to control music";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("play", "Play music")
                        .addOption(OptionType.STRING, "query", "URL or Youtube title of the track", true),
                new SubcommandData("stop", "Stop playing music and disconnect"),
                new SubcommandData("pause", "Pause playback"),
                new SubcommandData("resume", "Resume playback"), //Alias of pause
                new SubcommandData("loop", "Loop the current queue")
                        .addOption(OptionType.BOOLEAN, "single", "Loop the current track"),
                new SubcommandData("volume", "Get/set the volume")
                        .addOption(OptionType.INTEGER, "volume", "Volume to set"),
                new SubcommandData("queue", "What is queued"),
                new SubcommandData("skip", "Skip the current track")
                        .addOption(OptionType.INTEGER, "to", "Location in the queue to skip to"),
                new SubcommandData("seek", "Skip to the given time")
                        .addOption(OptionType.STRING, "time", "Time to play from (m:ss / mm:ss / h:mm:ss / hh:mm:ss)", true),
                new SubcommandData("remove", "Remove a track from the queue")
                        .addOption(OptionType.INTEGER, "location", "Location of the track", true),
                new SubcommandData("playnext", "Queue a track to play next")
                        .addOption(OptionType.STRING, "query", "Youtube/Spotify URL or Youtube title of the track", true),
                new SubcommandData("move", "Move a track to another position in the queue")
                        .addOption(OptionType.INTEGER, "from", "From where to move", true)
                        .addOption(OptionType.INTEGER, "to", "Where to move", true),
                new SubcommandData("lyrics", "Get lyrics of a track (current by default)")
                        .addOption(OptionType.STRING, "name", "Name of a track"),
                new SubcommandData("search", "Youtube search")
                        .addOption(OptionType.STRING, "query", "Search query", true),
                new SubcommandData("clear", "Clear the entire queue")
        };
    }
}
