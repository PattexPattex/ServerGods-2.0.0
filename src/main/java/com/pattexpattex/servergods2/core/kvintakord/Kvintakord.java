package com.pattexpattex.servergods2.core.kvintakord;

import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.config.GuildConfig;
import com.pattexpattex.servergods2.core.kvintakord.discord.AloneInVoiceHandler;
import com.pattexpattex.servergods2.core.kvintakord.discord.KvintakordDiscordManager;
import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import com.pattexpattex.servergods2.core.kvintakord.spotify.SpotifyManger;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Everything music related.
 * */
public class Kvintakord {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final LyricsClient lyricsClient;
    private final SpotifyManger spotifyManger;
    private final AloneInVoiceHandler aloneInVoiceHandler;

    private static final Logger log = LoggerFactory.getLogger(Kvintakord.class);

    public Kvintakord() {
        log.info("Starting Kvintakord and its dependencies");

        musicManagers = new ConcurrentHashMap<>();
        lyricsClient = new LyricsClient(Bot.getConfig().getLyricsProvider());
        spotifyManger = new SpotifyManger();
        aloneInVoiceHandler = new AloneInVoiceHandler();

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }


    /* ---- Audio Playback Core ---- */

    public synchronized @NotNull GuildMusicManager getGuildMusicManager(@NotNull Guild guild) {

        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(guild, playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    //Player manager
    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    //Shutdown everything
    public void shutdown() {
        log.info("Shutting down {} AudioPlayer(s)", musicManagers.size());

        Bot.getJDA().getGuilds().forEach(this::stop);
        musicManagers.forEach((id, manager) -> manager.player.destroy());
        playerManager.shutdown();
        spotifyManger.shutdown();

        log.info("Successfully shut down Kvintakord");
    }

    /* ---- General Playback ---- */

    //Load and play a track
    public void loadAndPlay(final AudioChannel channel, final String identifier, boolean playFirst) {

        GuildMusicManager musicManager = getGuildMusicManager(channel.getGuild());
        Track spotifyTrack = null;

        String id = identifier;

        if (spotifyManger.isSpotifyInUrl(identifier)) {
            if (identifier.startsWith("https://open.spotify.com/track/")) {
                spotifyTrack = spotifyManger.getTrack(identifier);

                id = "ytsearch: " + spotifyTrack.getArtists()[0].getName() + " " + spotifyTrack.getName();
            } else if (identifier.startsWith("https://open.spotify.com/playlist/")) {
                Paging<PlaylistTrack> spotifyPlaylist = spotifyManger.getPlaylistTracks(identifier);

                for (PlaylistTrack playlistTrack : spotifyPlaylist.getItems()) {
                    loadAndPlay(channel, "https://open.spotify.com/track/" + spotifyManger.getIdFromUrl(playlistTrack.getTrack().getHref()));
                }

                return;
            } else if (identifier.startsWith("https://open.spotify.com/artist/")) {
                Track[] spotifyTracks = spotifyManger.getArtistTracks(identifier);

                for (Track track : spotifyTracks) {
                    loadAndPlay(channel, track.getExternalUrls().get("spotify"));
                }

                return;
            } else if (identifier.startsWith("https://open.spotify.com/album")) {
                Paging<TrackSimplified> spotifyTracks = spotifyManger.getAlbumTracks(identifier);

                for (TrackSimplified trackSimplified : spotifyTracks.getItems()) {
                    loadAndPlay(channel, trackSimplified.getExternalUrls().get("spotify"));
                }

                return;
            }
        }

        if (identifier.startsWith("https://open.spotify.com/") && !spotifyManger.useSpotify()) {
            throw new UnsupportedOperationException("Spotify is not supported");
        }
        else if (identifier.startsWith("https://soundcloud.com/")) {
            throw new UnsupportedOperationException("Soundcloud is not supported");
        }
        else if (!identifier.startsWith("C:\\\\") && !identifier.startsWith("https://") && !identifier.startsWith("http://")) {
            id = "ytsearch: " + identifier;
        }

        Track finalSpotifyTrack = spotifyTrack;
        playerManager.loadItemOrdered(musicManager, id, new AudioLoadResultHandler() {

            @Override public void trackLoaded(AudioTrack track) {
                if (finalSpotifyTrack != null) {
                    track.setUserData(new TrackMetadata(finalSpotifyTrack));
                }
                else {
                    track.setUserData(new TrackMetadata(track));
                }

                if (playFirst) {
                    playFirst(channel, musicManager, track);
                }
                else {
                    play(channel, musicManager, track);
                }

                AudioEventDispatcher.onTrackLoad(channel.getGuild(), track);
            }

            @Override public void playlistLoaded(AudioPlaylist playlist) {

                if (!playlist.isSearchResult()) {
                    for (AudioTrack track : playlist.getTracks()) {
                        if (track != null) {
                            loadAndPlay(channel, track.getInfo().uri, false);
                        }
                    }
                }
                else {
                    AudioTrack firstTrack = playlist.getSelectedTrack();

                    if (firstTrack == null) {
                        firstTrack = playlist.getTracks().get(0);
                    }

                    trackLoaded(firstTrack);
                }
            }

            @Override public void noMatches() {
                AudioEventDispatcher.onTrackNoMatches(channel.getGuild(), identifier);
            }

            @Override public void loadFailed(FriendlyException exception) {
                AudioEventDispatcher.onTrackLoadFail(channel.getGuild(), exception);
            }
        });
    }

    public void loadAndPlay(AudioChannel channel, String identifier) {
        loadAndPlay(channel, identifier, false);
    }

    //Playback
    public void play(AudioChannel channel, GuildMusicManager musicManager, AudioTrack track) {
        KvintakordDiscordManager.connectToVoice(channel, channel.getGuild().getAudioManager());

        musicManager.player.setVolume(Bot.getGuildConfig(channel.getGuild()).getVolume());
        musicManager.scheduler.queue(track);
    }

    public void playFirst(AudioChannel channel, GuildMusicManager musicManager, AudioTrack track) {
        KvintakordDiscordManager.connectToVoice(channel, channel.getGuild().getAudioManager());

        musicManager.player.setVolume(Bot.getGuildConfig(channel.getGuild()).getVolume());
        musicManager.scheduler.queueFirst(track);
    }

    public boolean isPlaying(Guild guild) {
        return getCurrentTrack(guild) != null;
    }

    public boolean pause(Guild guild) {
        AudioPlayer player = getGuildMusicManager(guild).player;
        player.setPaused(!player.isPaused());

        return player.isPaused();
    }

    public boolean isPaused(Guild guild) {
        return getGuildMusicManager(guild).player.isPaused();
    }

    //Stop and clean-up
    public void stop(Guild guild) {
        GuildMusicManager musicManager = getGuildMusicManager(guild);

        musicManager.player.stopTrack();
        musicManager.scheduler.queueClear();

        KvintakordDiscordManager.updateLastQueueMessage(guild);
        KvintakordDiscordManager.removeLastQueueMessage(guild);
        KvintakordDiscordManager.removeLastTrackQueuedMessage(guild);

        KvintakordDiscordManager.disconnectFromVoice(guild.getAudioManager());

        AudioEventDispatcher.onPlaybackEnd(guild, musicManager.player);
    }

    //Volume
    public void setVolume(Guild guild, int vol) throws IndexOutOfBoundsException {
        if (vol < 0 || vol > 1000) {
            throw new IndexOutOfBoundsException("Given " + vol + ", expected between 1000 and 0");
        }

        AudioPlayer player = getGuildMusicManager(guild).player;
        player.setVolume(vol);

        Bot.getGuildConfig(guild).setVolume(vol);

        AudioEventDispatcher.onPlaybackVolumeChange(guild, player);
    }

    public int getVolume(Guild guild) {
        return getGuildMusicManager(guild).player.getVolume();
    }


    /* ---- Queue Related Methods ---- */

    /**
     * @return An immutable list of {@code AudioTrack}s currently in the guild's queue.
     */
    @Contract("_ -> new")
    public @NotNull List<AudioTrack> getQueue(Guild guild) {
        return getGuildMusicManager(guild).scheduler.queueGet();
    }

    public AudioTrack getTrack(int pos, Guild guild) {
        List<AudioTrack> queue = getQueue(guild);

        if (pos < 0 || pos > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + pos + ", expected between " + queue.size() + " and 0");
        }

        return queue.get(pos);
    }

    public void clearQueue(Guild guild) {
        getGuildMusicManager(guild).scheduler.queueClear();
    }

    //Track skipping
    public boolean skipToTrack(int location, Guild guild) throws IndexOutOfBoundsException {
        TrackScheduler scheduler = getGuildMusicManager(guild).scheduler;
        List<AudioTrack> queue = getQueue(guild);

        if (location < 0 || location > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + location + ", expected between " + queue.size() + " and 0");
        }

        if (location != 0) {
            if (scheduler.loop == LoopMode.ALL) {
                scheduler.queueSilent(Objects.requireNonNull(getCurrentTrack(guild)).makeClone());

                for (int i = 0; location > i; i++) {
                    scheduler.queueSilent(scheduler.queueRemove());
                }
            }
            else {
                for (int i = 0; location > i; i++) {
                    scheduler.queueRemove();
                }
            }

        }
        else {
            if (scheduler.loop == LoopMode.ALL) {
                scheduler.queueSilent(Objects.requireNonNull(getCurrentTrack(guild)).makeClone());

            }

        }

        return scheduler.trackNext();
    }

    //Track moving
    public void moveTrack(int from, int to, Guild guild) throws IndexOutOfBoundsException {
        List<AudioTrack> queue = new ArrayList<>(getQueue(guild));
        TrackScheduler scheduler = getGuildMusicManager(guild).scheduler;

        if (from < 0 || from >= queue.size()) {
            throw new IndexOutOfBoundsException("Given " + from + ", expected between " + (queue.size() + 1) + " and 0");
        }
        if (to < 0 || to > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + to + ", expected between " + queue.size() + " and 0");
        }

        AudioTrack track = queue.remove(from);
        queue.add(to, track);

        scheduler.queueSet(queue);
    }

    //Track remove
    public boolean removeTrack(int location, Guild guild) throws IndexOutOfBoundsException {
        List<AudioTrack> queue = getQueue(guild);

        if (location < 0 || location > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + location + ", expected between " + queue.size() + " and 0");
        }

        return getGuildMusicManager(guild).scheduler.queueRemove(queue.get(location));
    }

    //Loop
    public void setLoop(Guild guild, LoopMode mode) {
        TrackScheduler scheduler = getGuildMusicManager(guild).scheduler;
        GuildConfig config = Bot.getGuildConfig(guild);

        scheduler.loop = mode;

        config.setLoop(scheduler.loop);
    }

    /**
     * @implNote Loop mode {@code ALL} is superior to {@code SINGLE}
     */
    public enum LoopMode {
        OFF,
        ALL,
        SINGLE
    }

    public LoopMode getLoop(Guild guild) {
        return Bot.getGuildConfig(guild).getLoop();
    }


    /* ---- Track Related Methods ---- */

    //Current track
    public @Nullable AudioTrack getCurrentTrack(Guild guild) {
        return getGuildMusicManager(guild).player.getPlayingTrack();
    }

    //Track seeking
    public boolean seekTo(long pos, Guild guild) throws IndexOutOfBoundsException {
        AudioTrack track = getCurrentTrack(guild);
        long trackDuration = Objects.requireNonNull(track).getDuration();
        long posInMillis = TimeUnit.SECONDS.toMillis(pos);

        if (track.isSeekable()) {
            if (posInMillis > trackDuration || posInMillis < 0) {
                throw new IndexOutOfBoundsException("Given " + posInMillis + ", expected between " + trackDuration + " and 0");
            }
            else {
                track.setPosition(posInMillis);
                return true;
            }
        }
        else {
            return false;
        }
    }

    //Lyrics
    public synchronized @Nullable Lyrics lyricsFor(String name) throws Exception {
        return lyricsClient.getLyrics(name).get();
    }

    public SpotifyManger getSpotifyManger() {
        return spotifyManger;
    }

    public AloneInVoiceHandler getAloneInVoiceHandler() {
        return aloneInVoiceHandler;
    }
}
