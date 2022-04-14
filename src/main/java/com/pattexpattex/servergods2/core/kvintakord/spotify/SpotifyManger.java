package com.pattexpattex.servergods2.core.kvintakord.spotify;

import com.neovisionaries.i18n.CountryCode;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.SpotifyException;
import com.pattexpattex.servergods2.core.kvintakord.TrackMetadata;
import com.pattexpattex.servergods2.util.OtherUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jetbrains.annotations.NotNull;
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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyManger {

    private static final Logger log = LoggerFactory.getLogger(SpotifyManger.class);

    private static final Pattern AUTH_PATTERN = Pattern.compile("[a-z0-9]{32}");
    public static final Pattern URL_PATTERN = Pattern.compile("^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])(track|artist|playlist|album)\\1([a-zA-Z0-9]+)");

    private static final String MUST_CREATE_SPOTIFY_APP = "You must create a new Spotify Web API application and paste its ";
    private static final String AUTH_MORE_INFO = "For more info, see https://developer.spotify.com/documentation/general/guides/authorization/app-settings/";

    private long authExpiresOn;
    private boolean useSpotify;
    private SpotifyApi api;

    public SpotifyManger() {

        String id = Bot.getConfig().getValue("spotify_app_id");
        String secret = Bot.getConfig().getValue("spotify_app_secret");

        useSpotify = true;

        if (!AUTH_PATTERN.matcher(id).matches()) {
            log.warn("Spotify application ID is invalid ({})\n" + MUST_CREATE_SPOTIFY_APP +
                    "Client ID into \"config.json\" in the field \"spotify_app_id\".\n" + AUTH_MORE_INFO, id);
            useSpotify = false;
        }

        if (!AUTH_PATTERN.matcher(secret).matches()) {
            log.warn(" Spotify application secret is invalid ({})\n" + MUST_CREATE_SPOTIFY_APP +
                    "Client Secret into \"config.json\" in the field \"spotify_app_secret\".\n" + AUTH_MORE_INFO, secret);
            useSpotify = false;
        }

        if (useSpotify) {
            api = new SpotifyApi.Builder()
                    .setClientId(id)
                    .setClientSecret(secret)
                    .build();

            //loginToSpotify();
        }
        else {
            api = null;
        }
    }

    public void shutdown() {
        useSpotify = false;
        api = null;
    }

    public synchronized void loginToSpotify() {
        ClientCredentialsRequest request = api.clientCredentials().build();

        try {
            ClientCredentials credentials = request.execute();

            api.setAccessToken(credentials.getAccessToken());

            log.info("Retrieved Spotify access token, expires in: " + credentials.getExpiresIn());
            authExpiresOn = OtherUtil.epoch() + credentials.getExpiresIn() - 5;

            useSpotify = true;
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            log.warn("Failed retrieving token, disabling Spotify support");

            useSpotify = false;
        }
    }

    public boolean isSpotifyInUrl(String url) {
        try {
            return getIdFromUrl(url) != null;
        }
        catch (SpotifyException e) {
            return false;
        }
    }

    private void ensureAuth() {
        if (notUseSpotify()) {
            throw new SpotifyException(new UnsupportedOperationException("Spotify is not enabled"));
        }

        //Should happen only if shutdown() was called
        if (api == null) {
            throw new SpotifyException(new NullPointerException("SpotifyApi is null"));
        }

        if (OtherUtil.epoch() >= authExpiresOn) {
            loginToSpotify();
        }
    }

    public SpotifyApi getApi() {
        return api;
    }

    public boolean notUseSpotify() {
        return !useSpotify;
    }

    public boolean isSpotifyTrack(AudioTrack track) {
        return track.getUserData(TrackMetadata.class).album != null;
    }

    public String getIdFromUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);

        if (!matcher.find()) throw new SpotifyException(String.format("Url \"%s\" is not a valid Spotify URL", url));

        return matcher.group(3);
    }

    //Spotify web API requests
    public synchronized Track getTrack(@NotNull String url) throws RuntimeException {
        ensureAuth();
        GetTrackRequest request = api.getTrack(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new SpotifyException(e);
        }
    }

    public synchronized Paging<PlaylistTrack> getPlaylistTracks(@NotNull String url) throws RuntimeException {
        ensureAuth();
        GetPlaylistsItemsRequest request = api.getPlaylistsItems(getIdFromUrl(url)).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new SpotifyException(e);
        }
    }

    public synchronized Track[] getArtistTracks(@NotNull String url) throws RuntimeException {
        ensureAuth();
        GetArtistsTopTracksRequest request = api.getArtistsTopTracks(getIdFromUrl(url), CountryCode.getByLocale(Locale.getDefault())).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new SpotifyException(e);
        }
    }

    public synchronized Paging<TrackSimplified> getAlbumTracks(@NotNull String url) throws RuntimeException {
        ensureAuth();
        GetAlbumsTracksRequest request = api.getAlbumsTracks(getIdFromUrl(url)).limit(50).offset(0).build();

        try {
            return request.execute();
        }
        catch (Exception e) {
            log.warn("Something broke when executing request " + request.getUri().toString(), e);
            throw new SpotifyException(e);
        }
    }
}
