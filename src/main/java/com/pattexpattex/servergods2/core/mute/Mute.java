package com.pattexpattex.servergods2.core.mute;

import com.pattexpattex.servergods2.core.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Mute extends Thread {

    public final boolean infinite;
    public final long id, moderator, guildId, start, end;
    public final String reason;

    private Role role;
    private Guild guild;
    private Member member, modMember;
    private final MuteManager manager;

    private static final Logger log = LoggerFactory.getLogger(Mute.class);

    public Mute(MuteManager manager, long id, long moderator, long guildId, long start, long end, String reason) {
        this.manager = manager;

        this.id = id;
        this.moderator = moderator;
        this.guildId = guildId;
        this.start = start;
        this.end = end;
        this.reason = reason;

        this.infinite = end <= start;

        manager.addMute(this);
        manager.writeMutes();
    }

    public Mute(MuteManager manager, JSONObject o) {
        this(manager,
                o.getLong("id"),
                o.getLong("by"),
                o.getLong("guild"),
                o.getLong("start"),
                (o.has("end") ? o.getLong("end") : -1),
                (o.has("reason") ? o.getString("reason") : null));
    }

    @Override
    public void run() {
        init();

        guild.addRoleToMember(member, role).reason("Mute: " + reason).queue(null, this::failed);

        if (infinite) return;

        try {
            guild.removeRoleFromMember(member, role).reason("Mute ended").completeAfter(end - Instant.now().getEpochSecond(), TimeUnit.SECONDS);
        }
        catch (RuntimeException ok) {
            guild.removeRoleFromMember(member, role).reason("Mute ended").queue();
        }
        finally {
            manager.removeMute(this);
            manager.writeMutes();
        }
    }

    public void end() {
        init();

        if (infinite) {
            guild.removeRoleFromMember(member, role).reason("Mute ended").queue();

            manager.removeMute(this);
            manager.writeMutes();
        }
        else this.interrupt();
    }

    protected JSONObject toJSON() {
        JSONObject o = new JSONObject();

        o.put("id", id);
        o.put("by", moderator);
        o.put("guild", guildId);
        o.put("start", start);
        if (end > start) o.put("end", end);
        if (reason != null) o.put("reason", reason);

        return o;
    }

    private void failed(Throwable t) {
        manager.removeMute(this);
        manager.writeMutes();

        log.warn("Mute with id \"" + id + "\" failed", t);
    }

    private void init() {
        if (guild == null) guild = Bot.getJDA().getGuildById(guildId);

        if (guild == null) failed(new NullPointerException());

        if (member == null) member = guild.getMemberById(id);
        if (modMember == null) modMember = guild.getMemberById(moderator);

        if (member == null || modMember == null) failed(new NullPointerException());

        role = MuteManager.updateMutedRole(guild);
    }
}
