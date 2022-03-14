package com.pattexpattex.servergods2.config;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class GuildConfig {

    private final GuildConfigManager manager;
    protected JSONArray funRoles;
    protected JSONArray commands;
    protected String mutedId, giveawayId, pollId, welcomeId;
    protected String invite;
    protected Integer volume;
    protected boolean singleLoop, loop;

    public GuildConfig(GuildConfigManager manager,
                       JSONArray funRoles, JSONArray commands,
                       String mutedId, String giveawayId, String pollId, String welcomeId, String invite,
                       Integer volume,
                       boolean singleLoop, boolean loop)
    {
        this.manager = manager;
        this.funRoles = funRoles;
        this.commands = commands;
        this.invite = invite;

        this.mutedId = mutedId;
        this.giveawayId = giveawayId;
        this.pollId = pollId;
        this.welcomeId = welcomeId;
        this.volume = volume;
        this.singleLoop = singleLoop;
        this.loop = loop;
    }


    //Get() methods
    public @NotNull List<String> getFunRoles() {
        List<String> roles = new ArrayList<>();

        if (this.funRoles != null) {
            for (Object id : this.funRoles.toList()) {
                roles.add((String) id);
            }
        }

        return roles;
    }

    public @NotNull List<String> getCommands() {
        List<String> commands = new ArrayList<>();

        if (this.commands != null) {
            for (Object cmd : this.commands.toList()) {
                commands.add((String) cmd);
            }
        }
        else {
            for (Object cmd : this.manager.defaultCommands.toList()) {
                commands.add((String) cmd);
            }
        }

        return commands;
    }

    public @Nullable Role getMuted(Guild guild) {
        return guild == null || mutedId == null ? null : guild.getRoleById(mutedId);
    }

    public @Nullable Role getGiveaway(Guild guild) {
        return guild == null || giveawayId == null ? null : guild.getRoleById(giveawayId);
    }

    public @Nullable Role getPoll(Guild guild) {
        return guild == null || pollId == null ? null : guild.getRoleById(pollId);
    }

    public @Nullable TextChannel getWelcome(Guild guild) {
        return guild == null || welcomeId == null ? null : guild.getTextChannelById(welcomeId);
    }

    public @Nullable String getInvite() {
        return invite;
    }

    public int getVolume() {
        return volume != null ? volume : 100;
    }

    public boolean getSingleLoop() {
        return singleLoop;
    }

    public boolean getLoop() {
        return loop;
    }

    //Set() methods
    public void setFunRoles(@NotNull List<String> roles) {
        if (roles.isEmpty()) {
            this.funRoles = null;
        }
        else {
            this.funRoles = new JSONArray(roles.toArray());
        }

        this.manager.writeGuildConfig();
    }

    public void setCommands(@NotNull List<String> commands) {
        List<String> list = new ArrayList<>();

        for (Object cmd : this.manager.defaultCommands.toList()) {
            list.add((String) cmd);
        }

        if (commands.containsAll(list) && commands.size() == list.size()) {
            this.commands = null;
        }
        else {
            this.commands = new JSONArray(commands.toArray());
        }

        this.manager.writeGuildConfig();
    }

    public void setMuted(@Nullable Role role) {
        if (role != null && !role.isPublicRole()) {
            this.mutedId = role.getId();
        }
        else {
            this.mutedId = null;
        }

        this.manager.writeGuildConfig();
    }

    public void setGiveaway(@Nullable Role role) {
        if (role != null && !role.isPublicRole()) {
            this.giveawayId = role.getId();
        }
        else {
            this.giveawayId = null;
        }

        this.manager.writeGuildConfig();
    }

    public void setPoll(@Nullable Role role) {
        if (role != null && !role.isPublicRole()) {
            this.pollId = role.getId();
        }
        else {
            this.pollId = null;
        }

        this.manager.writeGuildConfig();
    }

    public void setWelcome(@Nullable TextChannel channel) {
        this.welcomeId = channel == null ? null : channel.getId();
        this.manager.writeGuildConfig();
    }

    public void setInvite(@Nullable String invite) {
        this.invite = invite;
        this.manager.writeGuildConfig();
    }

    public void setVolume(int volume) {
        if (volume != 100) {
            this.volume = Math.min(1000, Math.max(0, volume));
        }
        else {
            this.volume = null;
        }

        this.manager.writeGuildConfig();
    }

    public void setSingleLoop(boolean loop) {
        this.singleLoop = loop;

        this.manager.writeGuildConfig();
    }

    public void setLoop(boolean queueLoop) {
        this.loop = queueLoop;

        this.manager.writeGuildConfig();
    }
}
