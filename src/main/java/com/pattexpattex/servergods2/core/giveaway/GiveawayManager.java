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
import java.util.List;
import java.util.Map;

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

                giveaways.put(Long.parseLong(id), new Giveaway(this, o));
            });
        }
        catch (Exception e) {
            log.warn("Failed reading from \"" + file + "\"", e);
        }
    }

    public Map<Long, Giveaway> getGiveaways(long guildId) {
        List<Map.Entry<Long, Giveaway>> list = giveaways.entrySet().stream().filter((Map.Entry<Long, Giveaway> entry) -> entry.getValue().guildId == guildId).toList();
        Map<Long, Giveaway> map = new HashMap<>();

        list.forEach((Map.Entry<Long, Giveaway> entry) -> {
            map.put(entry.getKey(), entry.getValue());
        });

        return map;
    }

    public @Nullable Giveaway getGiveaway(long id) {
        return giveaways.get(id);
    }

    protected void addGiveaway(Giveaway giveaway) {
        giveaways.putIfAbsent(giveaway.id, giveaway);
    }

    protected void removeGiveaway(Giveaway giveaway) {
        giveaways.remove(giveaway.id);
    }

    public void removeGiveaway(long id) {
        giveaways.remove(id);
    }

    public void writeGiveaways() {
        JSONObject o = new JSONObject();

        this.giveaways.keySet().forEach((id) -> {
            Giveaway giveaway = this.giveaways.get(id);

            JSONObject ob = giveaway.toJSON();

            o.put(Long.toString(id), ob);
        });

        try {
            Files.write(OtherUtil.getPath(file), o.toString(4).getBytes());
        }
        catch (IOException e) {
            log.warn("Failed writing to \"" + file + "\"", e);
        }
    }

    public void resumeActiveGiveaways() {
        for (Giveaway giveaway : giveaways.values()) {
            try {
                giveaway.start();
                //log.info("Resumed giveaway with id {}", giveaway.id);
            }
            catch (IllegalThreadStateException e) {
                log.warn("Giveaway with id {} already started", giveaway.id, e);

            }
        }
    }
}
