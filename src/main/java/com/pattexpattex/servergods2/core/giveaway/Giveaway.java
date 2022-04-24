package com.pattexpattex.servergods2.core.giveaway;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Giveaway {

    private static final String REACTION = "\uD83C\uDF89";
    private static final Logger log = LoggerFactory.getLogger(Giveaway.class);

    /* Saved as a JSON key */
    private final long id;

    /* Saved inside a JSONObject */
    private int winners;
    private final long end;
    private final long channelId;
    private final long hostId;
    private final String reward;
    private final AtomicBoolean completed;


    /* Not saved */
    private final long guildId;
    private final Member host;
    private final GuildMessageChannel channel;
    private Message message;
    private final GiveawayManager manager;
    private ScheduledFuture<?> removalFuture;
    private ScheduledFuture<?> endingFuture;

    private Giveaway(GiveawayManager manager, String reward,
                       Message message, Member host,
                       int winners, long end, boolean completed) {
        this.manager = manager;
        this.reward = reward;
        this.winners = winners;
        this.end = end;
        this.message = message;
        this.id = message.getIdLong();
        this.channel = message.getGuildChannel();
        this.channelId = channel.getIdLong();
        this.guildId = channel.getGuild().getIdLong();
        this.host = host;
        this.hostId = host.getIdLong();
        this.completed = new AtomicBoolean(completed);

        manager.addGiveaway(this);
        manager.writeGiveaways();

        if (end <= OtherUtil.epoch() || completed) {
            endingFuture = Bot.getScheduledExecutor().schedule(() -> null, 0, TimeUnit.MILLISECONDS);
            scheduleRemoval();
            this.completed.set(true);
            return;
        }

        log.info("Scheduling giveaway with id {} to end at {}", id, end);
        endingFuture = Bot.getScheduledExecutor().schedule(() -> end(false), end - OtherUtil.epoch(), TimeUnit.SECONDS);
    }

    /* ---- Functional methods ---- */
    public final void end(boolean reroll) {
        if (!completed.compareAndSet(false, true) && !reroll) return;
        endingFuture.cancel(false);
        if (reroll) log.info("Rerolling giveaway with id {}", id);
        else log.info("Ending giveaway with id {}", id);

        try {
            message = message.getChannel().retrieveMessageById(id).complete();
        } catch (RuntimeException e) {
            return;
        }

        message.getReactions().stream().filter(reaction -> reaction.getReactionEmote().getEmoji().equals(REACTION))
                .findAny().ifPresent(reaction ->
                {
                    List<User> reactors = new ArrayList<>(reaction.retrieveUsers().complete());
                    reactors.remove(Bot.getJDA().getSelfUser());

                    if (!reactors.isEmpty()) {
                        List<User> users = new ArrayList<>(reactors);
                        List<User> winners = new ArrayList<>();
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; this.winners > i && reactors.size() > i; i++) {
                            User user = users.get((int) (new Random().nextDouble() * users.size()));
                            winners.add(user);
                            users.remove(user);
                        }

                        winners.forEach(user -> sb.append(String.format("%s ", user.getAsMention())));
                        notifySuccessfulEnd(sb.toString(), reroll);
                    }
                    else
                        notifyFailedEnd(reroll);
                });

        scheduleRemoval();
    }

    public void cancel() {
        if (completed.get()) return;
        log.info("Cancelling giveaway with id {}", id);

        manager.removeGiveaway(id);
        manager.writeGiveaways();
        if (removalFuture != null) removalFuture.cancel(false);
        endingFuture.cancel(false);
        message.delete().queue();
    }

    public void reroll() {
        reroll(winners);
    }

    public void reroll(int winners) {
        if (!completed.get()) return;

        if (this.winners != winners)
            this.winners = winners;

        end(true);
    }

    /* ---- Get methods ---- */
    public boolean isCompleted() {
        return completed.get();
    }

    public long getEnd() {
        return end;
    }

    public int getWinners() {
        return winners;
    }

    public Member getHost() {
        return host;
    }

    public long getId() {
        return id;
    }

    public String getReward() {
        return reward;
    }

    public GuildMessageChannel getChannel() {
        return channel;
    }

    public long getGuildId() {
        return guildId;
    }

    /* ---- Private methods ---- */
    private void scheduleRemoval() {
        manager.writeGiveaways();
        if (removalFuture != null) removalFuture.cancel(false);

        log.info("Scheduling giveaway with id {} for deletion in 48 hours", id);
        removalFuture = Bot.getScheduledExecutor().schedule(() -> {
            log.info("Deleting giveaway with id {} from cache", id);
            manager.removeGiveaway(id);
            manager.writeGiveaways();
        }, 48, TimeUnit.HOURS);
    }

    private void notifySuccessfulEnd(String winners, boolean reroll) {
        Role role = Bot.getGuildConfig(message.getGuild()).getGiveaway(message.getGuild());
        String mention = role == null ? "@everyone" : role.getAsMention();
        MessageEmbed embed = FormatUtil.endedGiveawayEmbed(id, winners, host, reward).build();

        if (!reroll)
            message.editMessage(Emotes.TADA + " **GIVEAWAY ENDED** " + Emotes.TADA).setEmbeds(embed).queue();

        message.getChannel()
                .sendMessage(String.format("%s %s\n%s %s", Emotes.BELL, mention, Emotes.BELL, host.getAsMention()))
                .setActionRows(FormatUtil.jumpButton(message)).setEmbeds(embed).queue();
    }

    private void notifyFailedEnd(boolean reroll) {
        Role role = Bot.getGuildConfig(message.getGuild()).getGiveaway(message.getGuild());
        String mention = role == null ? "@everyone" : role.getAsMention();
        MessageEmbed embed = FormatUtil.noWinnersGiveawayEmbed(id, reward).build();

        if (!reroll)
            message.editMessage(Emotes.TADA + " **GIVEAWAY ENDED** " + Emotes.TADA).setEmbeds(embed).queue();

        message.getChannel()
                .sendMessage(Emotes.BELL + mention + "\n" + Emotes.BELL + host.getAsMention())
                .setActionRows(FormatUtil.jumpButton(message)).setEmbeds(embed).queue();
    }


    /* ---- Static methods ---- */
    public static Giveaway build(GiveawayManager manager, GuildMessageChannel channel,
                                 String reward, Member host, int winners, long end) {
        log.info("Building new giveaway (channel {}, guild {}, host {})", channel.getIdLong(), channel.getGuild().getIdLong(), host.getIdLong());

        return new Giveaway(manager, reward, sendStartMessage(channel, reward, host, winners, end), host, winners, end, end <= OtherUtil.epoch());
    }

    public static Giveaway ofJSON(GiveawayManager manager, long id, JSONObject object) {
        log.info("Loading giveaway from JSON ({})", object.toString());

        String reward = object.getString("reward");
        int winners  = object.optInt("winners", 1);
        long channelId = object.getLong("channel");
        long hostId = object.getLong("host");
        long end = object.getLong("end");
        boolean completed = object.getBoolean("completed");

        JDA jda = Bot.getJDA();
        GuildMessageChannel channel = (GuildMessageChannel) jda.getGuildChannelById(channelId);
        Objects.requireNonNull(channel, "Giveaway channel not found");

        Member host = channel.getGuild().retrieveMemberById(hostId).complete();
        Objects.requireNonNull(host, "Host not found");

        Message message;
        try {
            message = channel.retrieveMessageById(id).complete();
        }
        catch (RuntimeException e) {
            throw new NullPointerException("Message not found");
        }

        return new Giveaway(manager, reward, message, host, winners, end, completed);
    }

    public static JSONObject toJSON(Giveaway giveaway) {
        JSONObject object = new JSONObject();

        object.put("reward", giveaway.reward);
        object.put("end", giveaway.end);
        object.put("host", giveaway.hostId);
        object.put("channel", giveaway.channelId);
        if (giveaway.winners != 1) object.put("winners", giveaway.winners);
        object.put("completed", giveaway.completed.get());

        return object;
    }

    private static Message sendStartMessage(GuildMessageChannel channel, String reward,
                                            Member host, int winners, long end) {
        Role r = Bot.getGuildConfig(channel.getGuild()).getGiveaway(channel.getGuild());
        String role = r == null ? "@everyone" : r.getAsMention();
        MessageBuilder builder = new MessageBuilder();

        builder.append(String.format("%s\n\n%s **GIVEAWAY** %s", role, Emotes.TADA, Emotes.TADA));
        builder.setEmbeds(FormatUtil.runningGiveawayEmbed(winners, end, reward, host).build());

        Message message = channel.sendMessage(builder.build()).complete();
        message.addReaction(REACTION).queue();
        return message;
    }
}
