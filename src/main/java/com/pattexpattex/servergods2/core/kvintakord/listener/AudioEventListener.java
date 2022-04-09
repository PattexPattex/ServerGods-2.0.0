package com.pattexpattex.servergods2.core.kvintakord.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.AudioChannel;

@SuppressWarnings("unused")
public abstract class AudioEventListener {
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
