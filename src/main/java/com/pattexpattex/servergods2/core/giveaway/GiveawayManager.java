package com.pattexpattex.servergods2.core.giveaway;

import com.pattexpattex.servergods2.core.config.GuildConfigManager;
import com.pattexpattex.servergods2.util.OtherUtil;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GiveawayManager {

    private final static String file = "cache\\giveaways.json";
    private final static Logger log = LoggerFactory.getLogger(GuildConfigManager.class);
    private final Map<Long, Giveaway> giveaways;

    public GiveawayManager() {
        giveaways = new HashMap<>();

        try {
            JSONObject loadedGiveaways = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath(file))));

            loadedGiveaways.keySet().forEach((id) -> {
                JSONObject o = loadedGiveaways.getJSONObject(id);

                giveaways.put(Long.parseLong(id), Giveaway.ofJSON(this, Long.parseLong(id), o));
            });
        }
        catch (Exception e) {
            log.warn("Failed reading from \"" + file + "\"", e);
        }
    }

    public Map<Long, Giveaway> getGiveaways(long guildId) {
        return giveaways.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getGuildId() == guildId)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public @Nullable Giveaway getGiveaway(long id) {
        return giveaways.get(id);
    }

    public void addGiveaway(Giveaway giveaway) {
        giveaways.put(giveaway.getId(), giveaway);
    }

    public void removeGiveaway(long id) {
        giveaways.remove(id);
    }

    public void writeGiveaways() {
        JSONObject o = new JSONObject();

        giveaways.forEach((id, giveaway) -> o.put(Long.toString(id), Giveaway.toJSON(giveaway)));

        try {
            Files.write(OtherUtil.getPath(file), o.toString(4).getBytes());
        }
        catch (IOException e) {
            log.warn("Failed writing to \"" + file + "\"", e);
        }
    }
}
