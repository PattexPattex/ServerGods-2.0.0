package com.pattexpattex.servergods2.core.mute;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionContainer;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class MuteManager {

    private static final String file = "cache\\mutes.json";
    private static final Logger log = LoggerFactory.getLogger(MuteManager.class);
    private final Map<Long, Mute> mutes;

    private static final EnumSet<Permission> permissions = EnumSet.of(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_ADD_REACTION,
            Permission.VOICE_SPEAK,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_VAD,
            Permission.CREATE_INSTANT_INVITE,
            Permission.NICKNAME_CHANGE);

    public MuteManager() {
        mutes = new HashMap<>();

        try {
            JSONObject loadedMutes = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath(file))));

            loadedMutes.keySet().forEach((id) -> {
                JSONObject o = loadedMutes.getJSONObject(id);

                mutes.put(Long.parseLong(id), new Mute(this, o));
            });
        }
        catch (Exception e) {
            log.warn("Failed reading from \"" + file + "\"", e);
        }
    }

    public @Nullable Mute getMute(long id) {
        return mutes.get(id);
    }

    protected void addMute(Mute mute) {
        mutes.putIfAbsent(mute.id, mute);
    }

    protected void removeMute(Mute mute) {
        mutes.remove(mute.id);
    }

    public void writeMutes() {
        JSONObject o = new JSONObject();

        this.mutes.keySet().forEach((id) -> {
            Mute mute = this.mutes.get(id);

            JSONObject ob = mute.toJSON();

            o.put(Long.toString(id), ob);
        });

        try {
            Files.write(OtherUtil.getPath(file), o.toString(4).getBytes());
        }
        catch (IOException e) {
            log.warn("Failed writing to \"" + file + "\"", e);
        }
    }

    public void resumeActiveMutes() {
        for (Mute mute : mutes.values()) {
            try {
                mute.start();
            }
            catch (IllegalThreadStateException e) {
                log.warn("Mute \"" + mute.id + "\" already started", e);
            }
        }
    }

    public static Role updateMutedRole(Guild guild) {
        Role role = Bot.getGuildConfig(guild).getMuted(guild);

        if (role == null) {
            if (guild.getRolesByName("muted", true).size() != 0) {
                role = guild.getRolesByName("muted", true).get(0);
            }
            else if (guild.getRolesByName("mute", true).size() != 0) {
                role = guild.getRolesByName("mute", true).get(0);
            }
            else {
                role = guild.createRole().setPermissions(Permission.EMPTY_PERMISSIONS).setName("Muted").setColor(new Color(0x5c676f)).reason("Create mute role").complete();
                Bot.getGuildConfig(guild).setMuted(role);

            }
        }

        for (GuildChannel channel : guild.getChannels(true)) {
            IPermissionContainer container = channel.getPermissionContainer();

            if (container.getPermissionOverride(role) != null) continue;
            channel.getPermissionContainer().createPermissionOverride(role).setDeny(permissions).reason("Update channel for effective mutes").queue();
        }

        return role;
    }
}
