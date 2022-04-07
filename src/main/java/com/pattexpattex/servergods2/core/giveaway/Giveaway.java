package com.pattexpattex.servergods2.core.giveaway;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Giveaway extends Thread {

    protected int winners;
    protected long end;
    protected boolean completed;
    protected final long id, hostId, guildId;
    protected final String reward;

    private String mention;
    private Member host;
    private Guild guild;
    private Message message;
    private ScheduledFuture<?> future;
    private final GiveawayManager manager;

    private static final Logger log = LoggerFactory.getLogger(Giveaway.class);

    public Giveaway(GiveawayManager manager, long id, long hostId, long guildId, String reward, int winners, long end) {
        this.setName("GiveawayThread-" + id);

        this.manager = manager;
        this.id = id;
        this.hostId = hostId;
        this.guildId = guildId;
        this.reward = reward;
        this.winners = winners;
        this.end = end;

        check();
    }

    protected Giveaway(GiveawayManager manager, JSONObject o) {
        this(manager,
                o.getLong("id"),
                o.getLong("host"),
                o.getLong("guild"),
                o.getString("reward"),
                (o.has("winners") ? o.getInt("winners") : 1),
                o.getLong("end"));
    }

    protected JSONObject toJSON() {
        JSONObject o = new JSONObject();

        o.put("id", id);
        o.put("host", hostId);
        o.put("guild", guildId);
        o.put("reward", reward);
        if (winners != 1) o.put("winners", winners);
        o.put("end", end);

        return o;
    }

    @Override
    public void run() {
        if (completed) return;
        init();

        log.info("Starting giveaway with id {}", id);

        try {
            message.editMessage(mention + "\n\n\uD83C\uDF89 **GIVEAWAY** \uD83C\uDF89").complete();
            message.editMessageEmbeds(FormatUtil.runningGiveawayEmbed(winners, end, reward, host).build()).queue();
        }
        catch (RuntimeException e) {
            failed(e);
        }

        log.info("Scheduled giveaway with id {} to end at {} (in {} seconds)", id, end, end - OtherUtil.epoch());
        Bot.getExecutor().schedule(() -> {
            end(false);
        }, end - OtherUtil.epoch(), TimeUnit.SECONDS);
    }

    public void end(boolean reroll) {
        if (completed) return;
        init();

        if (reroll) log.info("Rerolling giveaway with id {}", id);
        else log.info("Ending giveaway with id {}", id);

        try {
            message = message.getChannel().retrieveMessageById(id).complete();
        }
        catch (RuntimeException e) {
            failed(e);
        }

        Objects.requireNonNull(message).getReactions().stream().filter((mr) -> mr.getReactionEmote().getEmoji().equals("\uD83C\uDF89")).findAny().ifPresent((mr) ->
        {
            List<User> users = new ArrayList<>(mr.retrieveUsers().complete());
            users.remove(Bot.getJDA().getSelfUser());

            if (!users.isEmpty()) {
                List<User> users1 = new ArrayList<>(users);
                List<User> winnersList = new ArrayList<>();
                StringBuilder sb = new StringBuilder();

                for (int i = 0; winners > i && users.size() > i; i++) {
                    User user = users1.get((int) (Math.random() * users1.size()));
                    winnersList.add(user);
                    users1.remove(user);
                }

                winnersList.forEach((user) -> sb.append(user.getAsMention()).append(" "));

                MessageAction temp = message.getChannel()
                        .sendMessage(BotEmoji.MENTION + sb + "\n" + BotEmoji.MENTION + host.getAsMention())
                        .setActionRows(FormatUtil.jumpButton(message));

                if (reroll) {
                    temp.setEmbeds(FormatUtil.rerollGiveawayEmbed(id, sb.toString(), host, reward).build()).queue();
                }
                else {
                    message.editMessage("\uD83C\uDF89 **GIVEAWAY ENDED** \uD83C\uDF89")
                            .setEmbeds(FormatUtil.endedGiveawayEmbed(id, sb.toString(), host, reward).build()).queue();

                    temp.setEmbeds(FormatUtil.endedGiveawayEmbed(id, sb.toString(), host, reward).build()).queue();
                }

            }
            else {
                MessageAction temp = message.getChannel()
                        .sendMessage(BotEmoji.MENTION + mention + "\n" + BotEmoji.MENTION + host.getAsMention())
                        .setActionRows(FormatUtil.jumpButton(message));

                if (reroll) {
                    temp.setEmbeds(FormatUtil.noWinnersGiveawayEmbed(id, reward).build()).queue();
                }
                else {
                    message.editMessage("\uD83C\uDF89 **GIVEAWAY ENDED** \uD83C\uDF89")
                            .setEmbeds(FormatUtil.noWinnersGiveawayEmbed(id, reward).build()).queue();

                    temp.setEmbeds(FormatUtil.noWinnersGiveawayEmbed(id, reward).build()).queue();
                }
            }
        });

        completed = true;

        if (future != null) future.cancel(false);

        log.info("Scheduling to delete giveaway with id {} from cache in 24hrs", id);

        future = Bot.getExecutor().schedule(() -> {
            log.info("Deleted giveaway with id {} from cache", id);
            manager.removeGiveaway(this);
            manager.writeGiveaways();
        }, 24, TimeUnit.HOURS);
    }

    public void cancel() {
        log.info("Cancelling giveaway with id {}", id);

        manager.removeGiveaway(this);
        manager.writeGiveaways();
        if (future != null) future.cancel(false);

        if (completed) return;
        init();

        completed = true;

        message.editMessage("_was a giveaway once..._").queue();
        message.editMessageEmbeds(FormatUtil.defaultEmbed("Giveaway canceled").build()).queue();
        message.removeReaction("\uD83C\uDF89").queue();
    }

    public void reroll() {
        reroll(winners);
    }

    public void reroll(int winners) {
        if (!completed) return;

        completed = false;

        if (this.winners != winners) {
            this.winners = winners;
        }

        end(true);
    }

    private void failed(Throwable t) {
        log.warn("Giveaway with id {} failed", id, t);

        if (future != null) future.cancel(false);
        log.info("Deleted giveaway with id {} from cache", id);
        manager.removeGiveaway(this);
        manager.writeGiveaways();

        if (completed) return;

        completed = true;
    }

    private void init() {
        if (guild == null) guild = Bot.getJDA().getGuildById(guildId);

        if (guild == null) failed(new NullPointerException("guild is null"));

        if (message == null) message = OtherUtil.findMessageById(id, guild);
        if (host == null) host = guild.getMemberById(hostId);

        if (message == null || host == null) failed(new NullPointerException("message / host is null"));

        Role role = Bot.getGuildConfig(message.getGuild()).getGiveaway(message.getGuild());
        mention = (role == null ? "@everyone": role.getAsMention());
    }

    private void check() {
        if (end < OtherUtil.epoch()) {
            completed = true;

            log.info("Giveaway with id {} finished, scheduling to delete from cache in 24hrs", id);

            future = Bot.getExecutor().schedule(() -> {
                log.info("Deleted giveaway with id {} from cache", id);
                manager.removeGiveaway(this);
                manager.writeGiveaways();
            }, 24, TimeUnit.HOURS);
        }
        else {
            completed = false;

            manager.addGiveaway(this);
            manager.writeGiveaways();
        }
    }

    public boolean isCompleted() {
        return completed;
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

    public long getGiveawayId() {
        return id;
    }

    public String getReward() {
        return reward;
    }

    public Guild getGuild() {
        return guild;
    }
}