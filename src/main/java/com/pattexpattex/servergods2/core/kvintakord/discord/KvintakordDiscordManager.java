package com.pattexpattex.servergods2.core.kvintakord.discord;

import com.pattexpattex.servergods2.commands.button.music.*;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.core.kvintakord.listener.AudioEventDispatcher;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class KvintakordDiscordManager {

    private final Kvintakord kvintakord;
    private final Map<Long, Message> lastQueueMessages;
    private final Map<Long, Message> lastTrackQueuedMessages;
    private final Map<Long, Integer> currentPages;

    public KvintakordDiscordManager(Kvintakord kvintakord) {
        this.kvintakord = kvintakord;
        lastQueueMessages = new HashMap<>();
        lastTrackQueuedMessages = new HashMap<>();
        currentPages = new HashMap<>();
    }

    public void isPlayingElseFail(Guild guild) {
        if (!kvintakord.isPlaying(guild)) throw new BotException("Nothing is currently playing");
    }

    public ActionRow[] getActionRows(Guild guild) {
        List<Component> firstRow = List.of(new PauseButton(), new SkipButton(), new LoopButton(), new StopButton(), new LyricsButton());
        List<Component> secondRow = new ArrayList<>(List.of(new RefreshButton(), new ClearButton(), new DestroyButton()));

        boolean addExtras = multiPaged(guild);

        if (addExtras) {
            secondRow.add(PreviousPageButton.getInstance(guild));
            secondRow.add(NextPageButton.getInstance(guild));
        }

        return new ActionRow[]{
                ActionRow.of(firstRow), ActionRow.of(secondRow)
        };
    }

    /**
     * This throws an exception if this bot is not connected to a voice channel,
     * nothing is currently playing, the member is self-deafened or the member
     * and this bot are not in the same voice channel.
     */
    public void checkAllConditions(@NotNull Member member) {
        GuildVoiceState state = Objects.requireNonNull(member.getVoiceState());
        Guild guild = member.getGuild();

        if (currentVoiceChannel(guild) == null) throw new BotException("I am not connected to a voice channel");
        if (!state.inAudioChannel()) throw new BotException("You are not connected to a voice channel");
        if (state.getChannel() != currentVoiceChannel(guild)) throw new BotException("We are not in the same voice channel");
        if (state.isSelfDeafened()) throw new BotException("Un-deafen yourself to control music");
        isPlayingElseFail(guild);
    }

    /* -- Queue Command Message -- */

    public int currentPage(@NotNull Guild guild) {
        int queueSize = kvintakord.getQueue(guild).size();

        if (queueSize <= 20) {
            currentPages.remove(guild.getIdLong());
            return 0;
        }
        return currentPages.getOrDefault(guild.getIdLong(), 0);
    }

    public boolean multiPaged(@NotNull Guild guild) {
        int queueSize = kvintakord.getQueue(guild).size();
        return queueSize > 20;
    }

    public boolean isNextPage(@NotNull Guild guild) {
        int queueSize = kvintakord.getQueue(guild).size();
        int currentPage = currentPage(guild);

        return queueSize > 20 * (currentPage + 1);
    }

    public boolean isPreviousPage(@NotNull Guild guild) {
        return currentPage(guild) != 0;
    }

    public synchronized boolean updateLastQueueMessage(@NotNull Guild guild, @NotNull ButtonClickEvent event, int page) {
        Message message = getLastQueueMessage(guild);

        if (message == null) {
            return false;
        }

        return updateLastQueueMessage(guild, page) && message.getIdLong() == event.getMessage().getIdLong();
    }

    public synchronized boolean updateLastQueueMessage(@NotNull Guild guild, int page) {
        Message message = getLastQueueMessage(guild);

        if (message != null && kvintakord.isPlaying(guild)) {
            currentPages.put(guild.getIdLong(), page);
            message.editMessageEmbeds(FormatUtil.getQueueEmbed(guild, page)).setActionRows(getActionRows(guild)).queue();
            return true;
        }
        else if (message != null && !kvintakord.isPlaying(guild)) {
            message.editMessageEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Playback ended").build()).setActionRows(Collections.emptyList()).queue();

            removeLastQueueMessage(guild);
            return true;
        }

        return false;
    }

    public synchronized boolean lastQueueMessageExists(@NotNull Guild guild) {
        return getLastQueueMessage(guild) != null;
    }

    public synchronized boolean isNotLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        Message message1 = getLastQueueMessage(guild);

        return message1 == null || message1.getIdLong() != message.getIdLong();
    }

    public synchronized void setLastQueueMessage(@NotNull Guild guild, @NotNull Message message) {
        lastQueueMessages.put(guild.getIdLong(), message);
    }

    public synchronized @Nullable Message removeLastQueueMessage(@NotNull Guild guild) {
        currentPages.remove(guild.getIdLong());
        return lastQueueMessages.remove(guild.getIdLong());
    }

    public synchronized @Nullable Message getLastQueueMessage(@NotNull Guild guild) {
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
    public synchronized boolean updateLastTrackQueuedMessage(@NotNull Guild guild, @NotNull MessageEmbed embed) {
        Message message = getLastTrackQueuedMessage(guild);

        if (message != null) {
            message.editMessageEmbeds(embed).queue();
            return true;
        }

        return false;
    }

    public synchronized void setLastTrackQueuedMessage(@NotNull Guild guild, @NotNull Message message) {
        lastTrackQueuedMessages.put(guild.getIdLong(), message);
    }

    public synchronized void removeLastTrackQueuedMessage(@NotNull Guild guild) {
        lastTrackQueuedMessages.remove(guild.getIdLong());
    }

    public synchronized @Nullable Message getLastTrackQueuedMessage(@NotNull Guild guild) {
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
    public void connectToVoice(AudioChannel channel, @NotNull AudioManager audioManager) {
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);

            kvintakord.getGuildMusicManager(channel.getGuild()).player.setPaused(false);

            AudioEventDispatcher.onConnectToAudioChannel(channel.getGuild(), channel);
        }
    }

    public void disconnectFromVoice(@NotNull AudioManager audioManager) {
        if (audioManager.isConnected()) {
            AudioChannel channel = audioManager.getConnectedChannel();
            audioManager.closeAudioConnection();

            AudioEventDispatcher.onDisconnectFromAudioChannel(audioManager.getGuild(), channel);
        }
    }

    public @Nullable AudioChannel currentVoiceChannel(@NotNull Guild guild) {
        return guild.getAudioManager().getConnectedChannel();
    }
}
