package com.pattexpattex.servergods2.core.kvintakord;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.Nullable;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.Comparator;

public final class TrackMetadata {

    public final String name, author, url;
    @Nullable public final String image, album, authorUrl;

    public TrackMetadata(Track track) {
        this.name = track.getName();
        this.author = track.getArtists()[0].getName();
        this.url = track.getExternalUrls().get("spotify");
        this.image = Arrays.stream(track.getAlbum().getImages()).max(Comparator.comparingInt((Image i) -> i.getWidth() * i.getHeight())).map(Image::getUrl).orElse("");

        this.album = track.getAlbum().getName();
        this.authorUrl = track.getArtists()[0].getExternalUrls().get("spotify");
    }

    public TrackMetadata(AudioTrack track) {
        this.name = track.getInfo().title;
        this.author = track.getInfo().author;
        this.url = track.getInfo().uri;

        if (track instanceof YoutubeAudioTrack) {
            this.image = "https://img.youtube.com/vi/" + track.getInfo().identifier + "/mqdefault.jpg";
        }
        else this.image = null;

        this.album = null;
        this.authorUrl = null;
    }

    public TrackMetadata(String name, String author, String url, @Nullable String image, @Nullable String album, @Nullable String authorUrl) {
        this.name = name;
        this.author = author;
        this.url = url;
        this.image = image;
        this.album = album;
        this.authorUrl = authorUrl;
    }

    public static String getTrackImage(AudioTrack track) {
        if (track == null) return null;

        if (track.getUserData(TrackMetadata.class) == null) return null;
        return track.getUserData(TrackMetadata.class).image;
    }

    public static String getTrackAuthor(AudioTrack track) {
        if (track == null) return null;

        if (track.getUserData(TrackMetadata.class) == null) return null;
        return track.getUserData(TrackMetadata.class).author;
    }

    public static String getTrackAuthorUrl(AudioTrack track) {
        if (track == null) return null;

        if (track.getUserData(TrackMetadata.class) == null) return null;
        return track.getUserData(TrackMetadata.class).authorUrl;
    }

    public static String getTrackName(AudioTrack track) {
        if (track == null) return null;

        if (track.getUserData(TrackMetadata.class) == null) return track.getInfo().title;
        return track.getUserData(TrackMetadata.class).name;
    }

    public static String getTrackUri(AudioTrack track) {
        if (track == null) return null;

        if (track.getUserData(TrackMetadata.class) == null) return track.getInfo().uri;
        return track.getUserData(TrackMetadata.class).url;
    }
}
