package com.pattexpattex.servergods2.core.kvintakord.discord;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AloneInVoiceHandler {

    private final HashMap<Long, Instant> aloneSince;
    private final long aloneTimeUntilStop;
    private final Kvintakord kvintakord;

    public AloneInVoiceHandler(Kvintakord kvintakord) {
        aloneSince = new HashMap<>();
        aloneTimeUntilStop = Bot.getConfig().getAloneTimeUntilStop();
        this.kvintakord = kvintakord;

        if (aloneTimeUntilStop > 0) {
            Bot.getScheduledExecutor().scheduleWithFixedDelay(this::check, 0, 5, TimeUnit.SECONDS);
        }
    }

    public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (aloneTimeUntilStop <= 0) return;

        Guild guild = event.getGuild();
        if (guild.getAudioManager().getSendingHandler() == null) return;

        boolean alone = isAlone(guild);
        boolean inList = aloneSince.containsKey(guild.getIdLong());

        if (!alone && inList) {
            aloneSince.remove(guild.getIdLong());
        }
        else if (alone && !inList) {
            aloneSince.put(guild.getIdLong(), Instant.now());
        }
    }

    private void check() {
        Set<Long> toRemove = new HashSet<>();

        for (Map.Entry<Long, Instant> entrySet : aloneSince.entrySet()) {
            if (entrySet.getValue().getEpochSecond() > Instant.now().getEpochSecond() - aloneTimeUntilStop) continue;

            Guild guild = Bot.getJDA().getGuildById(entrySet.getKey());

            if (guild == null) {
                toRemove.add(entrySet.getKey());
                continue;
            }

            AudioEventDispatcher.onDisconnectFromAudioChannelBecauseEmpty(guild, kvintakord.getDiscordManager().currentVoiceChannel(guild));

            kvintakord.stop(guild);

            toRemove.add(entrySet.getKey());
        }
        toRemove.forEach(aloneSince::remove);
    }

    private boolean isAlone(Guild guild) {
        if (guild.getAudioManager().getConnectedChannel() == null) {
            return false;
        }
        else {
            return guild.getAudioManager().getConnectedChannel().getMembers().stream()
                    .noneMatch((member) ->
                            !Objects.requireNonNull(member.getVoiceState()).isDeafened() && !member.getUser().isBot());
        }
    }
}
