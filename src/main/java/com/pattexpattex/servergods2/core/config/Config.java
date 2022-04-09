package com.pattexpattex.servergods2.core.config;

import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class Config {

    private final static Logger log = LoggerFactory.getLogger(Config.class);

    private final JSONObject defaultConfig;
    private final JSONObject config;

    public Config() {
        defaultConfig = new JSONObject(Objects.requireNonNull(OtherUtil.loadResource(this.getClass(), "/config.json")));
        config = readConfig();
    }

    private JSONObject readConfig() {

        JSONObject obj = null;

        try {
            String filename = "config.json";
            File file = new File(filename);

            //If it created a new file
            if (file.createNewFile()) {
                try {
                    //Write the default to the file
                    Files.write(OtherUtil.getPath(filename), defaultConfig.toString(4).getBytes());

                    log.info("Created new config file, please fill it out");
                }
                //Oops
                catch (IOException e) {
                    log.error("Failed to write to \"{}\"", filename, e);
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
        catch (IOException e) {
            log.error("Something is broken with the config", e);
            System.exit(-1);
        }

        return obj;
    }

    public String getValue(String key) throws IllegalArgumentException {

        //Searches for the key
        if (config.has(key)) {
            return config.getString(key);
        }

        //No results
        else throw new IllegalArgumentException("Config value \"" + key + "\" is missing, please fix");
    }

    public boolean isCmdEnabled(String key) {

        //Commands config block
        JSONObject cmds = config.getJSONObject("commands");

        //Searches for the key
        if (cmds.has(key)) return cmds.getBoolean(key);

        //No results
        else {
            log.warn("Config value \"{}\" is missing, please fix", key);
            return false;
        }
    }

    public Activity getActivity() throws IllegalArgumentException {

        //Get the config values
        String activity_type = config.getString("activity_type");
        String activity_text = config.getString("activity_text");

        Activity ac;

        switch (activity_type) {
            case "playing" -> ac = Activity.playing(activity_text);
            case "watching" -> ac = Activity.watching(activity_text);
            case "streaming" -> ac = Activity.streaming(activity_text, "https://www.youtube.com/watch?v=dQw4w9WgXcQ%22"); //lol
            case "listening" -> ac = Activity.listening(activity_text);
            case "competing" -> ac = Activity.competing(activity_text);

            default -> {
                log.warn("Broken config value \"activity_type\", please fix");
                ac = Activity.playing(activity_text);
            }
        }

        return ac;
    }

    public OnlineStatus getStatus() {

        //Get the config values
        String status = config.getString("status");

        switch (status) {
            case "online", "idle", "dnd", "invisible" -> {
                return OnlineStatus.fromKey(status);
            }

            default -> {
                log.warn("Broken config value \"status\", please fix");
                return OnlineStatus.ONLINE;
            }
        }
    }

    public boolean enabledEval() {
        return config.getBoolean("eval");
    }

    public String getLyricsProvider() {

        String provider = config.getString("lyrics_provider");

        switch (provider) {
            case "A-Z Lyrics", "MusixMatch", "Genius", "LyricsFreak" -> {
                return provider;
            }
            default -> {
                log.warn("Broken config value \"lyrics_provider\", please fix");
                return "A-Z Lyrics";
            }
        }
    }

    public long getAloneTimeUntilStop() {
        return config.getLong("alone_time_until_stop");
    }

    public boolean enabledDebugInfo() {
        return config.getBoolean("debug_info_in_messages");
    }
}