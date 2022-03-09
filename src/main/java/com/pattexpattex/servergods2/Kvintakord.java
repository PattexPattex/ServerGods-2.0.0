package com.pattexpattex.servergods2;

import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;
import com.neovisionaries.i18n.CountryCode;
import com.pattexpattex.servergods2.config.Config;
import com.pattexpattex.servergods2.config.GuildConfig;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumsTracksRequest;
import se.michaelthelin.spotify.requests.data.artists.GetArtistsTopTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Everything music related.
 * I'm very proud of this.
 * */
//@SuppressWarnings("unused")
public class Kvintakord {

    /* Don't let anyone instantiate this class */
    private Kvintakord() {}

    //Some fields
    private static final AudioPlayerManager playerManager;
    private static final AloneInVoiceHandler aloneInVoiceHandler;

    //Maps
    private static final Map<Long, GuildMusicManager> musicManagers;
    private static final Map<Long, AudioEventListener> audioEventListeners;
    private static final Map<AudioTrackInfo, Track> spotifyTrackReferences;
    private static final Map<Long, Message> lastQueueMessages;
    private static final Map<Long, Message> lastTrackQueuedMessages;

    //Spotify, lyrics, logging
    private static final SpotifyApi spotifyApi;
    private static boolean useSpotify;
    private static final LyricsClient lyricsClient;
    private static final Logger log;

    static {
        log = LoggerFactory.getLogger(Kvintakord.class);

        musicManagers = new ConcurrentHashMap<>();
        audioEventListeners = new ConcurrentHashMap<>();
        spotifyTrackReferences = new ConcurrentHashMap<>();
        lastQueueMessages = new HashMap<>();
        lastTrackQueuedMessages = new HashMap<>();

        lyricsClient = new LyricsClient(Bot.getConfig().getLyricsProvider());
        playerManager = new DefaultAudioPlayerManager();
        aloneInVoiceHandler = new AloneInVoiceHandler();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        String id = Bot.getConfig().getConfigValue(Config.ConfigValues.SPOTIFY_APP_ID);
        String secret = Bot.getConfig().getConfigValue(Config.ConfigValues.SPOTIFY_APP_SECRET);

        useSpotify = true;

        if (id == null || id.isBlank() || id.isEmpty()) {
            log.warn("Spotify application ID is invalid ({})\nYou must create a new Spotify Web API application and paste its Client ID into \"config.json\" in the field \"spotify_app_id\".\nFor more info, see https://developer.spotify.com/documentation/general/guides/authorization/app-settings/", id);
            useSpotify = false;
        }

        if (secret == null || secret.isBlank() || secret.isEmpty()) {
            log.warn(" Spotify application secret is invalid ({})\nYou must create a new Spotify Web API application and paste its Client Secret into \"config.json\" in the field \"spotify_app_secret\".\nFor more info, see https://developer.spotify.com/documentation/general/guides/authorization/app-settings/", secret);
            useSpotify = false;
        }

        if (useSpotify) {
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(id)
                    .setClientSecret(secret)
                    .build();

            startSpotifyApi();
        }
        else {
            spotifyApi = null;
        }
    }


    /* ---- Audio Playback Core ---- */

    public static synchronized @NotNull GuildMusicManager getGuildAudioPlayer(@NotNull Guild guild) {

        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(guild, playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }
    private static class AudioPlayerSendHandler implements AudioSendHandler {

        private final AudioPlayer audioPlayer;
        private final ByteBuffer buffer;
        private final MutableAudioFrame frame;

        public AudioPlayerSendHandler(AudioPlayer audioPlayer) {

            this.audioPlayer = audioPlayer;
            this.buffer = ByteBuffer.allocate(1024);
            this.frame = new MutableAudioFrame();

            this.frame.setBuffer(buffer);
        }

        @Override
        public boolean canProvide() {
            return audioPlayer.provide(frame);
        }

        @Nullable
        @Override
        public ByteBuffer provide20MsAudio() {
            ((Buffer) buffer).flip();
            return buffer;
        }

        @Override
        public boolean isOpus() {
            return true;
        }
    }
    private static class GuildMusicManager {

        public final AudioPlayer player;
        public final TrackScheduler scheduler;

        public GuildMusicManager(Guild guild, AudioPlayerManager manager) {

            player = manager.createPlayer();
            scheduler = new TrackScheduler(guild, player);
            player.addListener(scheduler);
        }

        public Kvintakord.AudioPlayerSendHandler getSendHandler() {
            return new AudioPlayerSendHandler(player);
        }
    }
    private static class TrackScheduler extends AudioEventAdapter {

        private final AudioPlayer player;
        private final BlockingQueue<AudioTrack> queue;
        private final Guild guild;
        private int trackFails;

        public boolean loop;
        public boolean queueLoop;

        public TrackScheduler(Guild guild, AudioPlayer player) {

            this.guild = guild;
            this.player = player;
            this.queue = new LinkedBlockingQueue<>();
            this.trackFails = 0;

            this.loop = Bot.getGuildConfig(guild).getLoop();
            this.queueLoop = Bot.getGuildConfig(guild).getQueueLoop();
        }

        //Methods
        @SuppressWarnings("ResultOfMethodCallIgnored")
        protected void queue(AudioTrack track) {
            if (!player.startTrack(track, true)) {
                queue.offer(track);

                AudioEventDispatcher.onTrackQueue(guild, track);
            }
        }

        protected void queueFirst(AudioTrack track) {
            if (!player.startTrack(track, true)) {

                BlockingQueue<AudioTrack> oldQueue = new LinkedBlockingQueue<>(List.of(track));

                queue.drainTo(oldQueue);
                queue.addAll(oldQueue);

                AudioEventDispatcher.onTrackQueue(guild, track);
            }
        }

        protected void clearQueue() {
            queue.clear();
        }

        @Contract("null -> fail")
        protected void setQueue(List<AudioTrack> list) {
            clearQueue();
            queue.addAll(list);
        }

        protected boolean nextTrack() {
            return playTrack(queue.poll());
        }

        protected boolean playTrack(AudioTrack track) {
            return player.startTrack(track, false);
        }

        //AudioEventAdapter overrides
        @Override public void onPlayerPause(AudioPlayer player) {
            AudioEventDispatcher.onPlaybackPause(guild, player);}

        @Override public void onPlayerResume(AudioPlayer player) {
            AudioEventDispatcher.onPlaybackResume(guild, player);}

        @Override public void onTrackStart(AudioPlayer player, AudioTrack track) {
            AudioEventDispatcher.onTrackStart(guild, track);}

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

            if (endReason.mayStartNext) {
                AudioTrack trackClone = track.makeClone();

                if (endReason == AudioTrackEndReason.LOAD_FAILED && trackFails < 3) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.warn("Thread did not sleep properly", e);
                    }

                    playTrack(trackClone);

                    trackFails++;
                }
                else {
                    trackFails = 0;
                }

                if (loop) {
                    playTrack(trackClone);
                }
                else if (queueLoop) {
                    queue.offer(trackClone);

                    nextTrack();
                }
                else if (queue.size() != 0) {
                    nextTrack();

                    forgetSpotifyTrackReference(track);
                }
                else {
                    for (Guild guild : Bot.getJDA().getGuilds()) {
                        if (getGuildAudioPlayer(guild).player == player) {
                            stop(guild);
                            break;
                        }
                    }

                    forgetSpotifyTrackReference(track);
                }
            }
            else if (endReason != AudioTrackEndReason.REPLACED) {
                for (Guild guild : Bot.getJDA().getGuilds()) {
                    if (getGuildAudioPlayer(guild).player == player) {
                        stop(guild);
                        break;
                    }
                }

                forgetSpotifyTrackReference(track);
            }
        }

        @Override public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            AudioEventDispatcher.onTrackException(guild, track, exception);

            if (trackFails < 3) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Thread did not sleep properly", e);
                }

                playTrack(track.makeClone());

                trackFails++;
            }
            else {
                trackFails = 0;
            }
        }
    }

    //Player manager
    public static AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    //Shutdown everything
    public static void shutdown() {
        log.info("Shutting down {} AudioPlayers", musicManagers.size());

        Bot.getJDA().getGuilds().forEach(Kvintakord::stop);
        musicManagers.forEach((id, manager) -> manager.player.destroy());
        playerManager.shutdown();

        log.info("Successfully shut down Kvintakord");
    }


    /* ---- Internal Event Listeners ---- */

    @SuppressWarnings("unused")
    public static abstract class AudioEventListener {
        //Track
        public void onTrackLoad(AudioTrack track) {}
        public void onTrackStart(AudioTrack track) {}
        public void onTrackQueue(AudioTrack track) {}
        public void onTrackException(AudioTrack track, FriendlyException exception) {}
        public void onTrackNoMatches(String id) {}
        public void onTrackLoadFail(FriendlyException exception) {}

        //Connect / disconnect
        public void onConnectToAudioChannel(AudioChannel channel) {}
        public void onDisconnectFromAudioChannel(AudioChannel channel) {}
        public void onDisconnectFromAudioChannelBecauseEmpty(AudioChannel channel) {}

        //Playback
        public void onPlaybackPause(AudioPlayer player) {}
        public void onPlaybackResume(AudioPlayer player) {}
        public void onPlaybackEnd(AudioPlayer player) {}
        public void onPlaybackVolumeChange(AudioPlayer player) {}
    }
    private static final class AudioEventDispatcher {
        public static void onTrackLoad(Guild guild, AudioTrack track) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackLoad(track);
            });
        }
        public static void onTrackStart(Guild guild, AudioTrack track) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackStart(track);
            });
        }
        public static void onTrackQueue(Guild guild, AudioTrack track) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackQueue(track);
            });
        }
        public static void onTrackException(Guild guild, AudioTrack track, FriendlyException exception) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackException(track, exception);
            });
        }
        public static void onTrackNoMatches(Guild guild, String id) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackNoMatches(id);
            });
        }
        public static void onTrackLoadFail(Guild guild, FriendlyException exception) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onTrackLoadFail(exception);
            });
        }
        public static void onConnectToAudioChannel(Guild guild, AudioChannel channel) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onConnectToAudioChannel(channel);
            });
        }
        public static void onDisconnectFromAudioChannel(Guild guild, AudioChannel channel) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onDisconnectFromAudioChannel(channel);
            });
        }
        public static void onDisconnectFromAudioChannelBecauseEmpty(Guild guild, AudioChannel channel) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onDisconnectFromAudioChannelBecauseEmpty(channel);
            });
        }
        public static void onPlaybackPause(Guild guild, AudioPlayer player) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onPlaybackPause(player);
            });
        }
        public static void onPlaybackResume(Guild guild, AudioPlayer player) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onPlaybackResume(player);
            });
        }
        public static void onPlaybackEnd(Guild guild, AudioPlayer player) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onPlaybackEnd(player);
            });
        }
        public static void onPlaybackVolumeChange(Guild guild, AudioPlayer player) {
            audioEventListeners.keySet().forEach((key) -> {
                if (key == guild.getIdLong()) audioEventListeners.get(key).onPlaybackVolumeChange(player);
            });
        }
    }

    public static void registerAudioEventListener(@NotNull Guild guild, AudioEventListener listener) {
        audioEventListeners.putIfAbsent(guild.getIdLong(), listener);
    }
    public static void removeAudioEventListener(@NotNull Guild guild, AudioEventListener listener) {
        audioEventListeners.remove(guild.getIdLong(), listener);
    }


    /* ---- General Playback ---- */

    //Load and play a track
    public static void loadAndPlay(final AudioChannel channel, final String id, boolean playFirst) {

        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        Track spotifyTrack = null;

        String trackId = id;

        if (useSpotify && id.startsWith("https://open.spotify.com/track/")) {
                spotifyTrack = getSpotifyTrack(id);

                trackId = "ytsearch: " + spotifyTrack.getArtists()[0].getName() + " " + spotifyTrack.getName();
            }
        else if (useSpotify && id.startsWith("https://open.spotify.com/playlist/")) {
                Paging<PlaylistTrack> spotifyPlaylist = getSpotifyPlaylistTracks(id);

                for (PlaylistTrack playlistTrack : spotifyPlaylist.getItems()) {
                    loadAndPlay(channel, "https://open.spotify.com/track/" + getTrackIdFromSpotifyUrl(playlistTrack.getTrack().getHref()));
                }

                return;
            }
        else if (useSpotify && id.startsWith("https://open.spotify.com/artist/")) {
                Track[] spotifyTracks = getSpotifyArtistTracks(id);

                for (Track track : spotifyTracks) {
                    loadAndPlay(channel, track.getExternalUrls().get("spotify"));
                }

                return;
            }
        else if (useSpotify && id.startsWith("https://open.spotify.com/album")) {
                Paging<TrackSimplified> spotifyTracks = getSpotifyAlbumTracks(id);

                for (TrackSimplified trackSimplified : spotifyTracks.getItems()) {
                    loadAndPlay(channel, trackSimplified.getExternalUrls().get("spotify"));
                }

                return;
            }
        else if (id.startsWith("https://soundcloud.com/")) {
            throw new IllegalArgumentException("Soundcloud is not supported");
        }
        else if (!id.startsWith("C:\\\\") && !id.startsWith("https://") && !id.startsWith("http://")) {
            trackId = "ytsearch: " + id;
        }

        Track finalSpotifyTrack = spotifyTrack;
        playerManager.loadItemOrdered(musicManager, trackId, new AudioLoadResultHandler() {

            @Override public void trackLoaded(AudioTrack track) {
                if (finalSpotifyTrack != null) {
                    makeSpotifyTrackReference(track, finalSpotifyTrack);
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
                AudioEventDispatcher.onTrackNoMatches(channel.getGuild(), id);
            }
            @Override public void loadFailed(FriendlyException exception) {
                AudioEventDispatcher.onTrackLoadFail(channel.getGuild(), exception);
            }
        });
    }
    public static void loadAndPlay(final AudioChannel channel, final String id) {
        loadAndPlay(channel, id, false);
    }

    //Playback
    public static void play(AudioChannel channel, GuildMusicManager musicManager, AudioTrack track) {
        connectToVoiceChannel(channel, channel.getGuild().getAudioManager());

        musicManager.player.setVolume(Bot.getGuildConfig(channel.getGuild()).getVolume());
        musicManager.scheduler.queue(track);
    }
    public static void playFirst(AudioChannel channel, GuildMusicManager musicManager, AudioTrack track) {
        connectToVoiceChannel(channel, channel.getGuild().getAudioManager());

        musicManager.player.setVolume(Bot.getGuildConfig(channel.getGuild()).getVolume());
        musicManager.scheduler.queueFirst(track);
    }
    public static boolean isPlaying(Guild guild) {
        return getCurrentTrack(guild) != null;
    }
    public static boolean pause(Guild guild) {
        AudioPlayer player = getGuildAudioPlayer(guild).player;
        player.setPaused(!player.isPaused());

        return player.isPaused();
    }
    public static boolean isPaused(Guild guild) {
        return getGuildAudioPlayer(guild).player.isPaused();
    }

    //Stop and clean-up
    public static void stop(Guild guild) {
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);

        forgetSpotifyTrackReference(musicManager.player.getPlayingTrack());
        getQueue(guild).forEach(Kvintakord::forgetSpotifyTrackReference);

        musicManager.player.stopTrack();
        musicManager.scheduler.clearQueue();

        updateLastQueueMessage(guild);
        removeLastQueueMessage(guild);
        removeLastTrackQueuedMessage(guild);

        disconnectFromVoiceChannel(guild.getAudioManager());

        AudioEventDispatcher.onPlaybackEnd(guild, musicManager.player);
    }

    //Volume
    public static void setVolume(Guild guild, int vol) throws IndexOutOfBoundsException {
        if (vol < 0 || vol > 1000) {
            throw new IndexOutOfBoundsException("Given " + vol + ", expected between 1000 and 0");
        }

        AudioPlayer player = getGuildAudioPlayer(guild).player;
        player.setVolume(vol);

        Bot.getGuildConfig(guild).setVolume(vol);

        AudioEventDispatcher.onPlaybackVolumeChange(guild, player);
    }
    public static int getVolume(Guild guild) {
        return getGuildAudioPlayer(guild).player.getVolume();
    }


    /* ---- Queue Related Methods ---- */

    /**
     * @return A new mutable {@link ArrayList} containing the elements of the guild's queue.
     */
    @Contract("_ -> new")
    public static @NotNull List<AudioTrack> getQueue(Guild guild) {
        return new ArrayList<>(getGuildAudioPlayer(guild).scheduler.queue);
    }
    public static AudioTrack getTrackAt(int pos, Guild guild) {
        List<AudioTrack> queue = getQueue(guild);

        if (pos < 0 || pos > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + pos + ", expected between " + queue.size() + " and 0");
        }

        return queue.get(pos);
    }
    public static void clearQueue(Guild guild) {
        getGuildAudioPlayer(guild).scheduler.clearQueue();
    }

    //Track skipping
    public static boolean skipToTrack(int location, Guild guild) throws IndexOutOfBoundsException {
        TrackScheduler scheduler = getGuildAudioPlayer(guild).scheduler;
        List<AudioTrack> queue = getQueue(guild);

        if (location < 0 || location > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + location + ", expected between " + queue.size() + " and 0");
        }

        if (location != 0) {
            if (scheduler.queueLoop) {
                scheduler.queue.add(Objects.requireNonNull(getCurrentTrack(guild)).makeClone());

                for (int i = 0; location > i; i++) {
                    scheduler.queue.add(scheduler.queue.remove());
                }
            }
            else {
                for (int i = 0; location > i; i++) {
                    scheduler.queue.poll();
                }
            }

        }
        else {
            if (scheduler.queueLoop) {
                scheduler.queue.add(Objects.requireNonNull(getCurrentTrack(guild)).makeClone());

            }

        }

        return scheduler.nextTrack();
    }

    //Track moving
    public static void moveTrack(int from, int to, Guild guild) throws IndexOutOfBoundsException {
        List<AudioTrack> queue = getQueue(guild);
        TrackScheduler scheduler = getGuildAudioPlayer(guild).scheduler;

        if (from < 0 || from >= queue.size()) {
            throw new IndexOutOfBoundsException("Given " + from + ", expected between " + (queue.size() + 1) + " and 0");
        }
        if (to < 0 || to > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + to + ", expected between " + queue.size() + " and 0");
        }

        AudioTrack track = queue.remove(from);
        queue.add(to, track);

        scheduler.setQueue(queue);
    }

    //Track remove
    public static boolean removeTrack(int location, Guild guild) throws IndexOutOfBoundsException {
        BlockingQueue<AudioTrack> queue = getGuildAudioPlayer(guild).scheduler.queue;

        if (location < 0 || location > queue.size()) {
            throw new IndexOutOfBoundsException("Given " + location + ", expected between " + queue.size() + " and 0");
        }

        AudioTrack track = getQueue(guild).get(location);
        return queue.remove(track);
    }

    //Loop
    public static void loopTrack(Guild guild, LoopMode mode) {
        TrackScheduler scheduler = getGuildAudioPlayer(guild).scheduler;
        GuildConfig config = Bot.getGuildConfig(guild);

        switch (mode) {
            case OFF -> {
                scheduler.loop = false;
                scheduler.queueLoop = false;
            }
            case ALL -> {
                scheduler.loop = false;
                scheduler.queueLoop = true;
            }
            case SINGLE -> {
                scheduler.loop = true;
                scheduler.queueLoop = false;
            }
        }

        config.setLoop(scheduler.loop);
        config.setQueueLoop(scheduler.queueLoop);

    }

    public enum LoopMode {
        OFF,
        SINGLE,
        ALL
    }

    /**
     * @implNote Queue loop is superior to single track loop
     */
    public static LoopMode isLooping(Guild guild) {
        boolean loop = Bot.getGuildConfig(guild).getLoop();
        boolean queueLoop = Bot.getGuildConfig(guild).getQueueLoop();

        if (queueLoop) {
            return LoopMode.ALL;
        }
        else if (loop) {
            return LoopMode.SINGLE;
        }
        else {
            return LoopMode.OFF;
        }
    }


    /* ---- Track Related Methods ---- */

    //Current track
    public static @Nullable AudioTrack getCurrentTrack(Guild guild) {
        return getGuildAudioPlayer(guild).player.getPlayingTrack();
    }

    //Track seeking
    public static boolean seekTo(long pos, Guild guild) throws IndexOutOfBoundsException {
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
    public static synchronized @Nullable Lyrics lyricsFor(String name) throws Exception {
        return lyricsClient.getLyrics(name).get();
    }


    /* ---- Discord-end Management ---- */

    //JDA events listener
    public static class DiscordAudioEventListener extends ListenerAdapter {

        @Override public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
            if (event.getMember() == event.getGuild().getSelfMember()) {
                Kvintakord.stop(event.getGuild());
            }
        }

        @Override public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
            aloneInVoiceHandler.onVoiceUpdate(event);
        }
    }

    @Contract("null -> fail")
    public static void isPlayingElseThrowException(Guild guild) throws IllegalStateException {
        if (!isPlaying(guild)) throw new BotException("Nothing is currently playing");
    }

        /* -- Queue Command Message -- */
    public static boolean updateLastQueueMessage(@NotNull Guild guild, @NotNull ButtonClickEvent event) {
        Message message = getLastQueueMessage(guild);

        if (message == null) {
            return false;
        }

        return updateLastQueueMessage(guild) && message.getIdLong() == event.getMessage().getIdLong();
    }
    public static boolean updateLastQueueMessage(@NotNull Guild guild) {
        Message message = getLastQueueMessage(guild);

        if (message != null && isPlaying(guild)) {
            message.editMessageEmbeds(FormatUtil.getQueueEmbed(guild)).queue();
            return true;
        }
        else if (message != null && !isPlaying(guild)) {
            message.editMessageEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Playback ended").build()).complete()
                    .editMessageComponents(Collections.emptyList()).queue();

            removeLastQueueMessage(guild);
            return true;
        }

        return false;
    }
    public static boolean lastQueueMessageExists(@NotNull Guild guild) {
        return getLastQueueMessage(guild) != null;
    }
    public static boolean isNotLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        Message message1 = getLastQueueMessage(guild);

        return message1 == null || message1.getIdLong() != message.getIdLong();
    }

    public static void setLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        lastQueueMessages.put(guild.getIdLong(), message);
    }
    public static @Nullable Message removeLastQueueMessage(@NotNull Guild guild) {
        return lastQueueMessages.remove(guild.getIdLong());
    }
    public synchronized static @Nullable Message getLastQueueMessage(@NotNull Guild guild) {
        Message message = lastQueueMessages.get(guild.getIdLong());

        if (message != null) {
            try {
                message = message.getChannel().retrieveMessageById(message.getId()).complete();
            }
            catch (Exception ignored) {}
        }

        if (message != null) {
            setLastQueueMessage(guild, message);
        }
        else {
            removeLastQueueMessage(guild);
        }

        return message;
    }

        /* -- Track Queued Message -- */
    public static boolean updateLastTrackQueuedMessage(@NotNull Guild guild, @NotNull MessageEmbed embed) {
        Message message = getLastTrackQueuedMessage(guild);

        if (message != null) {
            message.editMessageEmbeds(embed).queue();
            return true;
        }

        return false;
    }

    public static void setLastTrackQueuedMessage(@NotNull Guild guild, @NotNull Message message) {
        lastTrackQueuedMessages.put(guild.getIdLong(), message);
    }
    public static void removeLastTrackQueuedMessage(@NotNull Guild guild) {
        lastTrackQueuedMessages.remove(guild.getIdLong());
    }
    public synchronized static @Nullable Message getLastTrackQueuedMessage(@NotNull Guild guild) {
        Message message = lastTrackQueuedMessages.get(guild.getIdLong());

        if (message != null) {
            try {
                message = message.getChannel().retrieveMessageById(message.getId()).complete();
            }
            catch (Exception ignored) {}
        }

        if (message != null) {
            setLastTrackQueuedMessage(guild, message);
        }
        else {
            removeLastTrackQueuedMessage(guild);
        }

        return message;
    }

    //Handling alone time in voice channel
    private static class AloneInVoiceHandler {

        private final HashMap<Long, Instant> aloneSince;
        private final long aloneTimeUntilStop;

        public AloneInVoiceHandler() {
            aloneSince = new HashMap<>();
            aloneTimeUntilStop = Bot.getConfig().getAloneTimeUntilStop();

            if (aloneTimeUntilStop > 0) {
                Bot.getScheduledExecutor().scheduleWithFixedDelay(this::check, 0, 5, TimeUnit.SECONDS);
            }
        }

        private void check() {
            Set<Long> toRemove = new HashSet<>();

            for (Map.Entry<Long, Instant> entrySet : aloneSince.entrySet()) {
                if (entrySet.getValue().getEpochSecond() > Instant.now().getEpochSecond() - aloneTimeUntilStop) continue;

                Guild guild = Bot.getJDA().getGuildById(entrySet.getKey());

                if (guild == null) {
                    toRemove.add(entrySet.getKey());
                    continue;
                }

                AudioEventDispatcher.onDisconnectFromAudioChannelBecauseEmpty(guild, currentVoiceChannel(guild));

                Kvintakord.stop(guild);

                toRemove.add(entrySet.getKey());
            }
            toRemove.forEach(aloneSince::remove);
        }

        public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
            if (aloneTimeUntilStop <= 0) return;

            Guild guild = event.getGuild();
            if (guild.getAudioManager().getSendingHandler() == null) return;

            boolean alone = isAlone(guild);
            boolean inList = aloneSince.containsKey(guild.getIdLong());

            if (!alone && inList) {
                aloneSince.remove(guild.getIdLong());
            }
            else if (alone && !inList) {
                aloneSince.put(guild.getIdLong(), Instant.now());
            }
        }

        private boolean isAlone(Guild guild) {
            if (guild.getAudioManager().getConnectedChannel() == null) {
                return false;
            }
            else {
                return guild.getAudioManager().getConnectedChannel().getMembers().stream()
                        .noneMatch((member) ->
                                !Objects.requireNonNull(member.getVoiceState()).isDeafened() && !member.getUser().isBot());
            }
        }
    }

    //Voice channel logic
    public static void connectToVoiceChannel(AudioChannel channel, AudioManager audioManager) {
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);

            getGuildAudioPlayer(channel.getGuild()).player.setPaused(false);

            AudioEventDispatcher.onConnectToAudioChannel(channel.getGuild(), channel);
        }
    }
    public static void disconnectFromVoiceChannel(AudioManager audioManager) {
        if (audioManager.isConnected()) {
            AudioChannel channel = audioManager.getConnectedChannel();
            audioManager.closeAudioConnection();

            AudioEventDispatcher.onDisconnectFromAudioChannel(audioManager.getGuild(), channel);
        }
    }
    public static @Nullable AudioChannel currentVoiceChannel(Guild guild) {
        return guild.getAudioManager().getConnectedChannel();
    }


    /* ---- Spotify Track Playback Support ---- */

    public static String getTrackAuthor(AudioTrack track) {
        if (track == null) return "";

        Track spotifyTrack = getSpotifyTrackReference(track);

        if (spotifyTrack != null) {
            return spotifyTrack.getArtists()[0].getName();
        }

        return track.getInfo().author;
    }

    public static String getTrackAuthorUrl(AudioTrack track) {
        if (track == null) return null;

        Track spotifyTrack = getSpotifyTrackReference(track);

        if (spotifyTrack != null) {
            return spotifyTrack.getArtists()[0].getExternalUrls().get("spotify");
        }

        return null;
    }

    public static String getTrackName(AudioTrack track) {
        if (track == null) return "";

        Track spotifyTrack = getSpotifyTrackReference(track);

        if (spotifyTrack != null) {
            return spotifyTrack.getName();
        }

        return track.getInfo().title;
    }

    public static String getTrackUri(AudioTrack track) {
        if (track == null) return "";

        Track spotifyTrack = getSpotifyTrackReference(track);

        if (spotifyTrack != null) {
            return spotifyTrack.getExternalUrls().get("spotify");
        }

        return track.getInfo().uri;
    }

    public static String getTrackImage(AudioTrack track) {
        if (track == null) return null;

        Track spotifyTrack = getSpotifyTrackReference(track);

        if (spotifyTrack != null) {
            return spotifyTrack.getAlbum().getImages()[0].getUrl();
        }
        else {
            return "https://img.youtube.com/vi/" + track.getInfo().identifier + "/mqdefault.jpg";
        }
    }

    public static boolean isSpotifyTrack(AudioTrack track) {
        return getSpotifyTrackReference(track) != null;
    }

    //Spotify track reference management
    private static @Nullable Track getSpotifyTrackReference(@NotNull AudioTrack track) {
        return spotifyTrackReferences.get(track.getInfo());
    }
    private static void makeSpotifyTrackReference(AudioTrack track, Track spotifyTrack) {
        if (track != null && spotifyTrack != null) {
            spotifyTrackReferences.putIfAbsent(track.getInfo(), spotifyTrack);
        }
    }
    private static void forgetSpotifyTrackReference(AudioTrack track) {
        if (track != null) {
            spotifyTrackReferences.remove(track.getInfo());
        }
    }

    //Spotify web API requests
    private synchronized static Track getSpotifyTrack(@NotNull String url) throws RuntimeException {
        final GetTrackRequest request = spotifyApi.getTrack(getTrackIdFromSpotifyUrl(url)).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new RuntimeException(e);
        }
    }
    private synchronized static Paging<PlaylistTrack> getSpotifyPlaylistTracks(@NotNull String url) throws RuntimeException {
        final GetPlaylistsItemsRequest request = spotifyApi.getPlaylistsItems(getTrackIdFromSpotifyUrl(url)).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new RuntimeException(e);
        }
    }
    private synchronized static Track[] getSpotifyArtistTracks(@NotNull String url) throws RuntimeException {
        final GetArtistsTopTracksRequest request = spotifyApi.getArtistsTopTracks(getTrackIdFromSpotifyUrl(url), CountryCode.getByLocale(Locale.getDefault())).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new RuntimeException(e);
        }
    }
    private synchronized static Paging<TrackSimplified> getSpotifyAlbumTracks(@NotNull String url) throws RuntimeException {
        final GetAlbumsTracksRequest request = spotifyApi.getAlbumsTracks(getTrackIdFromSpotifyUrl(url)).limit(50).offset(0).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new RuntimeException(e);
        }
    }

    //Spotify ID formatting
    private static @NotNull String getTrackIdFromSpotifyUrl(@NotNull String url) throws IllegalArgumentException {
        if (url.startsWith("https://open.spotify.com/track/") || url.startsWith("https://open.spotify.com/album/")) return url.substring(31, 53);
        else if (url.startsWith("https://open.spotify.com/playlist/") || url.startsWith("https://api.spotify.com/v1/tracks/")) return url.substring(34, 56);
        else if (url.startsWith("https://open.spotify.com/artist/")) return url.substring(32, 54);

        else throw new IllegalArgumentException("Unknown URL");
    }

    //Spotify web API auth
    private synchronized static void startSpotifyApi() {
        ClientCredentialsRequest request = spotifyApi.clientCredentials().build();

        try {
            final ClientCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());

            Bot.getScheduledExecutor().schedule(Kvintakord::startSpotifyApi, credentials.getExpiresIn() - 60, TimeUnit.SECONDS);
            log.info("Retrieved Spotify access token, expires in: " + credentials.getExpiresIn());
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
        }
    }
}
