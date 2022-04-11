package com.pattexpattex.servergods2.core.kvintakord;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {

    private static final Logger log = LoggerFactory.getLogger(TrackScheduler.class);

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final Guild guild;
    private int trackFails;

    public Kvintakord.LoopMode loop;

    public TrackScheduler(Guild guild, AudioPlayer player) {

        this.guild = guild;
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.trackFails = 0;

        this.loop = Bot.getGuildConfig(guild).getLoop();
    }

    //Methods
    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.add(track);

            AudioEventDispatcher.onTrackQueue(guild, track);
        }
    }

    public void queueFirst(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            BlockingQueue<AudioTrack> oldQueue = new LinkedBlockingQueue<>(List.of(track));

            queue.drainTo(oldQueue);
            queue.addAll(oldQueue);

            AudioEventDispatcher.onTrackQueue(guild, track);
        }
    }

    public void queueSilent(AudioTrack track) {
        queue.add(track);
    }

    public List<AudioTrack> queueGet() {
        return queue.stream().toList();
    }

    protected void queueSet(List<AudioTrack> list) {
        queueClear();
        queue.addAll(list);
    }

    public AudioTrack queueRemove() {
        return queue.remove();
    }

    protected boolean queueRemove(AudioTrack track) {
        //Kvintakord.forgetSpotifyTrackReference(track);
        return queue.remove(track);
    }

    protected void queueClear() {
        queue.clear();
    }

    protected boolean trackPlay(AudioTrack track) {
        return player.startTrack(track, false);
    }

    protected boolean trackNext() {
        return trackPlay(queue.poll());
    }

    //AudioEventAdapter overrides
    @Override public void onPlayerPause(AudioPlayer player) {
        AudioEventDispatcher.onPlaybackPause(guild, player);}

    @Override public void onPlayerResume(AudioPlayer player) {
        AudioEventDispatcher.onPlaybackResume(guild, player);}

    @Override public void onTrackStart(AudioPlayer player, AudioTrack track) {
        AudioEventDispatcher.onTrackStart(guild, track);}

    @Override public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

        if (endReason.mayStartNext) {
            AudioTrack trackClone = track.makeClone();

            if (endReason == AudioTrackEndReason.LOAD_FAILED && trackFails < 3) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Thread did not sleep properly", e);
                }

                trackPlay(trackClone);
                trackFails++;

                return;
            }
            else {
                trackFails = 0;
            }

            if (loop == Kvintakord.LoopMode.SINGLE) {
                trackPlay(trackClone);
            }
            else if (loop == Kvintakord.LoopMode.ALL) {
                queue.add(trackClone);

                trackNext();
            }
            else if (queue.size() != 0) {
                trackNext();

                //Kvintakord.forgetSpotifyTrackReference(track);
            }
            else {
                for (Guild guild : Bot.getJDA().getGuilds()) {
                    if (Bot.getKvintakord().getGuildMusicManager(guild).player == player) {
                        Bot.getKvintakord().stop(guild);
                        break;
                    }
                }

                //Kvintakord.forgetSpotifyTrackReference(track);
            }
        }
        else if (endReason != AudioTrackEndReason.REPLACED) {
            for (Guild guild : Bot.getJDA().getGuilds()) {
                if (Bot.getKvintakord().getGuildMusicManager(guild).player == player) {
                    Bot.getKvintakord().stop(guild);
                    break;
                }
            }

            //Kvintakord.forgetSpotifyTrackReference(track);
        }
    }

    @Override public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        AudioEventDispatcher.onTrackException(guild, track, exception);

        if (trackFails < 3) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                log.warn("Thread did not sleep properly", e);
            }

            trackPlay(track.makeClone());

            trackFails++;
        }
        else {
            trackFails = 0;
        }
    }
}
