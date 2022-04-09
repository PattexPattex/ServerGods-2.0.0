package com.pattexpattex.servergods2.core.kvintakord.discord;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KvintakordDiscordManager {

    private static final Map<Long, Message> lastQueueMessages;
    private static final Map<Long, Message> lastTrackQueuedMessages;

    static {
        lastQueueMessages = new HashMap<>();
        lastTrackQueuedMessages = new HashMap<>();
    }
    public static void isPlayingElseFail(Guild guild) {
        if (!Bot.getKvintakord().isPlaying(guild)) throw new BotException("Nothing is currently playing");
    }

    /**
     * This throws an exception if this bot is not connected to a voice channel,
     * nothing is currently playing, the member is self-deafened or the member
     * and this bot are not in the same voice channel.
     */
    public static void checkAllConditions(Member member) {
        GuildVoiceState state = Objects.requireNonNull(member.getVoiceState());
        Guild guild = member.getGuild();

        if (currentVoiceChannel(guild) == null) throw new BotException("I am not connected to a voice channel");
        if (!state.inAudioChannel()) throw new BotException("You are not connected to a voice channel");
        if (state.getChannel() != currentVoiceChannel(guild)) throw new BotException("We are not in the same voice channel");
        if (state.isSelfDeafened()) throw new BotException("Un-deafen yourself to control music");
        isPlayingElseFail(guild);
    }

    /* -- Queue Command Message -- */
    public static boolean updateLastQueueMessage(@NotNull Guild guild, @NotNull ButtonClickEvent event) {
        Message message = getLastQueueMessage(guild);

        if (message == null) {
            return false;
        }

        return updateLastQueueMessage(guild) && message.getIdLong() == event.getMessage().getIdLong();
    }
    public static boolean updateLastQueueMessage(@NotNull Guild guild) {
        Message message = getLastQueueMessage(guild);

        if (message != null && Bot.getKvintakord().isPlaying(guild)) {
            message.editMessageEmbeds(FormatUtil.getQueueEmbed(guild)).queue();
            return true;
        }
        else if (message != null && !Bot.getKvintakord().isPlaying(guild)) {
            message.editMessageEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Playback ended").build()).queue();
            message.editMessageComponents(Collections.emptyList()).queue();

            removeLastQueueMessage(guild);
            return true;
        }

        return false;
    }
    public static boolean lastQueueMessageExists(@NotNull Guild guild) {
        return getLastQueueMessage(guild) != null;
    }
    public static boolean isNotLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        Message message1 = getLastQueueMessage(guild);

        return message1 == null || message1.getIdLong() != message.getIdLong();
    }

    public static void setLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        lastQueueMessages.put(guild.getIdLong(), message);
    }
    public static @Nullable Message removeLastQueueMessage(@NotNull Guild guild) {
        return lastQueueMessages.remove(guild.getIdLong());
    }
    public synchronized static @Nullable Message getLastQueueMessage(@NotNull Guild guild) {
        Message message = lastQueueMessages.get(guild.getIdLong());

        if (message != null) {
            try {
                message = message.getChannel().retrieveMessageById(message.getId()).complete();
            }
            catch (Exception e) {
                message = null;
            }
        }

        if (message != null) {
            setLastQueueMessage(guild, message);
        }
        else {
            removeLastQueueMessage(guild);
        }

        return message;
    }

    /* -- Track Queued Message -- */
    public static boolean updateLastTrackQueuedMessage(@NotNull Guild guild, @NotNull MessageEmbed embed) {
        Message message = getLastTrackQueuedMessage(guild);

        if (message != null) {
            message.editMessageEmbeds(embed).queue();
            return true;
        }

        return false;
    }

    public static void setLastTrackQueuedMessage(@NotNull Guild guild, @NotNull Message message) {
        lastTrackQueuedMessages.put(guild.getIdLong(), message);
    }
    public static void removeLastTrackQueuedMessage(@NotNull Guild guild) {
        lastTrackQueuedMessages.remove(guild.getIdLong());
    }
    public synchronized static @Nullable Message getLastTrackQueuedMessage(@NotNull Guild guild) {
        Message message = lastTrackQueuedMessages.get(guild.getIdLong());

        if (message != null) {
            try {
                message = message.getChannel().retrieveMessageById(message.getId()).complete();
            }
            catch (Exception e) {
                message = null;
            }
        }

        if (message != null) {
            setLastTrackQueuedMessage(guild, message);
        }
        else {
            removeLastTrackQueuedMessage(guild);
        }

        return message;
    }

    //Voice channel logic
    public static void connectToVoice(AudioChannel channel, AudioManager audioManager) {
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);

            Bot.getKvintakord().getGuildMusicManager(channel.getGuild()).player.setPaused(false);

            AudioEventDispatcher.onConnectToAudioChannel(channel.getGuild(), channel);
        }
    }
    public static void disconnectFromVoice(AudioManager audioManager) {
        if (audioManager.isConnected()) {
            AudioChannel channel = audioManager.getConnectedChannel();
            audioManager.closeAudioConnection();

            AudioEventDispatcher.onDisconnectFromAudioChannel(audioManager.getGuild(), channel);
        }
    }
    public static @Nullable AudioChannel currentVoiceChannel(Guild guild) {
        return guild.getAudioManager().getConnectedChannel();
    }
}
