package com.pattexpattex.servergods2.config;

import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class Config {

    //Constructor
    public Config() {
        config = readConfig();
    }

    private final JSONObject config;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //Template config.json in resources
    private final JSONObject defaultConf = new JSONObject(Objects.requireNonNull(OtherUtil.loadResource(this, "/config.json")));

    private JSONObject readConfig() {

        JSONObject obj = null;

        try {
            String filename = "config.json";
            File file = new File(filename);

            //If it created a new file
            if (file.createNewFile()) {
                try {
                    //Write the default to the file
                    Files.write(OtherUtil.getPath(filename), defaultConf.toString(4).getBytes());

                    log.info("Created default config file, now you must fill it out");
                }
                //Oops
                catch (IOException e) {
                    log.error("Failed to write to \"" + filename + "\"", e);
                    System.exit(-1);
                }

                System.exit(0);
            }
            //If the file already exists
            else {
                obj = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath(filename))));
            }
        }
        //Oops
        catch (Exception e) {
            log.error("Something is broken with the config", e);
            System.exit(-1);
        }

        return obj;
    }

    public String getConfigValue(@NotNull ConfigValues cv) throws IllegalArgumentException {
        //Config key
        String key = cv.toString().toLowerCase();

        //Searches for the key
        if (config.has(key)) {
            return config.getString(key);
        }

        //No results
        else throw new IllegalArgumentException("The key \"" + key + "\" is missing");
    }

    public boolean isCmdEnabled(@NotNull Commands co) throws IllegalArgumentException {

        //Config key
        String key = co.toString().toLowerCase();

        //Commands config block
        JSONObject cmds = config.getJSONObject("commands");

        //Searches for the key
        if (cmds.has(key)) return cmds.getBoolean(key);

        //No results
        else throw new IllegalArgumentException("The key \"" + key + "\" is missing");
    }

    public Activity getActivity() throws IllegalArgumentException {

        //Get the config values
        String activity_type = config.getString(ConfigValues.ACTIVITY_TYPE.toString().toLowerCase());
        String activity_text = config.getString(ConfigValues.ACTIVITY_TEXT.toString().toLowerCase());

        Activity ac;

        switch (activity_type) {
            case "PLAYING" -> ac = Activity.playing(activity_text);
            case "WATCHING" -> ac = Activity.watching(activity_text);
            case "STREAMING" -> ac = Activity.streaming(activity_text, "https://www.youtube.com/watch?v=dQw4w9WgXcQ%22"); //get rickrolled lol
            case "LISTENING" -> ac = Activity.listening(activity_text);
            case "COMPETING" -> ac = Activity.competing(activity_text);

            default -> throw new IllegalArgumentException("Broken config value \"" + activity_type + "\"");
        }

        return ac;
    }

    public OnlineStatus getStatus() {

        //Get the config values
        String status = config.getString(ConfigValues.STATUS.toString().toLowerCase());

        switch (status) {
            case "online", "idle", "dnd", "invisible" -> {
                return OnlineStatus.fromKey(status);
            }

            default -> {
                log.warn("Broken config value \"status\", defaulting to \"online\"");
                return OnlineStatus.ONLINE;
            }
        }
    }

    public String getLyricsProvider() {

        String provider = config.getString(ConfigValues.LYRICS_PROVIDER.toString().toLowerCase());

        switch (provider) {
            case "A-Z Lyrics", "MusixMatch", "Genius", "LyricsFreak" -> {
                return provider;
            }
            default -> {
                log.warn("Broken config value \"lyrics_provider\", defaulting to \"A-Z Lyrics\"");
                return "A-Z Lyrics";
            }
        }
    }

    public long getAloneTimeUntilStop() {
        return config.getLong(ConfigValues.ALONE_TIME_UNTIL_STOP.toString().toLowerCase());
    }

    public boolean enabledDebugInfo() {
        return config.getBoolean(ConfigValues.DEBUG_INFO_IN_MESSAGES.name().toLowerCase());
    }

    public enum ConfigValues {
        TOKEN,
        ACTIVITY_TYPE,
        ACTIVITY_TEXT,
        STATUS,
        COLOR,
        BOT_OWNER,
        ALONE_TIME_UNTIL_STOP,
        PREFIX,
        LYRICS_PROVIDER,
        DEBUG_INFO_IN_MESSAGES,
        SPOTIFY_APP_ID,
        SPOTIFY_APP_SECRET
    }

    /**
     * Because of the implementation, the enum referencing a command must be
     * an all-uppercase variation of the return value of {@link BotSlash#getName()}.
     */
    @SuppressWarnings("unused")
    public enum Commands {
        CONFIG,
        ENABLE,
        ABOUT,

        BAN,
        KICK,
        MUTE,
        WAKE,

        ROLES,
        PING,
        INVITE,
        GIVEAWAY,
        POLL,
        RICKROLL,
        MUSIC,
        EMOTE
    }
}