package com.pattexpattex.servergods2.core.kvintakord.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioEventDispatcher {

    private AudioEventDispatcher() {}

    private static final Map<Long, AudioEventListener> audioEventListeners;

    static {
        audioEventListeners = new ConcurrentHashMap<>();
    }

    public static void registerAudioEventListener(@NotNull Guild guild, AudioEventListener audioEventListener) {
        audioEventListeners.put(guild.getIdLong(), audioEventListener);
    }

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
