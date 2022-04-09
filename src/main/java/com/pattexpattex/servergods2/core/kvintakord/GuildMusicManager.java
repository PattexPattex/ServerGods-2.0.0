package com.pattexpattex.servergods2.core.kvintakord;

import com.pattexpattex.servergods2.core.kvintakord.discord.AudioSendHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

public class GuildMusicManager {

    public final AudioPlayer player;
    public final TrackScheduler scheduler;

    public GuildMusicManager(Guild guild, AudioPlayerManager manager) {

        player = manager.createPlayer();
        scheduler = new TrackScheduler(guild, player);
        player.addListener(scheduler);
    }

    public AudioSendHandler getSendHandler() {
        return new AudioSendHandler(player);
    }
}