package com.pattexpattex.servergods2.core.config;

import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

public class GuildConfigManager {

    private final String file = "server_config.json";
    private final HashMap<Long, GuildConfig> guildConfig;

    public final JSONArray defaultCommands = new JSONArray(Arrays.asList(
            "enable", "config", "about"
    ));

    public GuildConfigManager() {
        this.guildConfig = new HashMap<>();

        try {
            JSONObject loadedGuildConfig = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath(file))));

            loadedGuildConfig.keySet().forEach((id) -> {
                JSONObject obj = loadedGuildConfig.getJSONObject(id);

                {
                    guildConfig.put(Long.parseLong(id), new GuildConfig(this,
                            obj.has("fun_roles")    ? obj.getJSONArray("fun_roles") : null,
                            obj.has("commands")     ? obj.getJSONArray("commands")  : defaultCommands,
                            obj.has("muted_id")     ? obj.getString("muted_id")     : null,
                            obj.has("giveaway_id")  ? obj.getString("giveaway_id")  : null,
                            obj.has("poll_id")      ? obj.getString("poll_id")      : null,
                            obj.has("welcome_id")   ? obj.getString("welcome_id")   : null,
                            obj.has("invite")       ? obj.getString("invite")       : null,
                            obj.has("loop")         ? obj.getString("loop")         : "off",
                            obj.has("volume")       ? obj.getInt("volume")          : null
                    ));
                }
            });
        }
        catch (Exception e) {
            LoggerFactory.getLogger(GuildConfigManager.class).warn("Failed reading from " + file + " (normal if there are no values)", e);
        }
    }

    public GuildConfig getConfig(@NotNull Guild guild) {
        return getConfig(guild.getIdLong());
    }

    public GuildConfig getConfig(long guildId) {
        return guildConfig.computeIfAbsent(guildId, id -> createDefaultGuildConfig());
    }

    private @NotNull GuildConfig createDefaultGuildConfig() {
        return new GuildConfig(this, null, null, null, null, null, null, null, "off", null);
    }

    protected void writeGuildConfig() {
        JSONObject obj = new JSONObject();

        this.guildConfig.keySet().forEach(key -> {
            JSONObject obj1 = new JSONObject();
            GuildConfig guildConfig = this.guildConfig.get(key);

            {
                if (guildConfig.funRoles != null)       obj1.put("fun_roles", guildConfig.funRoles);
                if (guildConfig.commands != null)       obj1.put("commands", guildConfig.commands);
                if (guildConfig.mutedId != null)        obj1.put("muted_id", guildConfig.mutedId);
                if (guildConfig.giveawayId != null)     obj1.put("giveaway_id", guildConfig.giveawayId);
                if (guildConfig.pollId != null)         obj1.put("poll_id", guildConfig.pollId);
                if (guildConfig.welcomeId != null)      obj1.put("welcome_id", guildConfig.welcomeId);
                if (guildConfig.invite != null)         obj1.put("invite", guildConfig.invite);
                if (guildConfig.volume != null)         obj1.put("volume", guildConfig.volume);
                if (!guildConfig.loop.equals("off"))    obj1.put("loop", guildConfig.loop);
            }

            obj.put(Long.toString(key), obj1);
        });

        try {
            Files.write(OtherUtil.getPath(file), obj.toString(4).getBytes());
        }
        catch (IOException e) {
            LoggerFactory.getLogger(GuildConfigManager.class).warn("Failed to write to " + file, e);
        }
    }
}
