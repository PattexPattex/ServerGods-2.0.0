package com.pattexpattex.servergods2.core.kvintakord;

import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.AudioChannel;
import se.michaelthelin.spotify.model_objects.specification.Track;

public record LoadHandler(AudioChannel channel, GuildMusicManager musicManager,
                          Track spotifyTrack, String identifier,
                          boolean first, Kvintakord kvintakord)
        implements AudioLoadResultHandler {

    @Override
    public void trackLoaded(AudioTrack track) {
        if (spotifyTrack != null) {
            track.setUserData(new TrackMetadata(spotifyTrack));
        } else {
            track.setUserData(new TrackMetadata(track));
        }

        if (first) {
            kvintakord.playFirst(channel, musicManager, track);
        } else {
            kvintakord.play(channel, musicManager, track);
        }

        AudioEventDispatcher.onTrackLoad(channel.getGuild(), track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {

        if (!playlist.isSearchResult()) {
            for (AudioTrack track : playlist.getTracks()) {
                if (track != null) {
                    kvintakord.loadAndPlay(channel, track.getInfo().uri, false);
                }
            }
        } else {
            AudioTrack firstTrack = playlist.getSelectedTrack();

            if (firstTrack == null) {
                firstTrack = playlist.getTracks().get(0);
            }

            trackLoaded(firstTrack);
        }
    }

    @Override
    public void noMatches() {
        AudioEventDispatcher.onTrackNoMatches(channel.getGuild(), identifier);
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        AudioEventDispatcher.onTrackLoadFail(channel.getGuild(), exception);
    }
}
