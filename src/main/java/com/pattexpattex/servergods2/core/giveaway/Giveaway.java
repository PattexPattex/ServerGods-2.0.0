package com.pattexpattex.servergods2.core.giveaway;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Giveaway extends Thread {

    protected int winners;
    protected long end;
    protected boolean completed;
    protected final long id, hostId;
    protected final String reward;

    private String mention;
    private Member host;
    private Message message;
    private ScheduledFuture<?> future;
    private final long now;
    private final GiveawayManager manager;

    private static final Logger log = LoggerFactory.getLogger(Giveaway.class);

    public Giveaway(GiveawayManager manager, long id, String reward, int winners, long end, long hostId) {
        this.manager = manager;
        this.id = id;
        this.reward = reward;
        this.winners = winners;
        this.hostId = hostId;
        this.now = Instant.now().getEpochSecond();
        this.end = end;

        checkIsCompleted();
    }

    Giveaway(GiveawayManager manager, JSONObject o) {
        this(manager, o.getLong("id"), o.getString("reward"), o.getInt("winners"), o.getLong("end"), o.getLong("host_id"));
    }

    @Override
    public void run() {
        init();

        try {
            message.editMessage(mention + "\n\n\uD83C\uDF89 **GIVEAWAY** \uD83C\uDF89").complete();
            message.editMessageEmbeds(FormatUtil.runningGiveawayEmbed(winners, end, reward, host).build()).queue();
        }
        catch (RuntimeException e) {
            failed(e);
        }

        waitUntilEnd();
    }

    public void end(boolean reroll) {
        if (completed) return;
        init();

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

                message.getChannel().sendMessage(BotEmoji.MENTION + sb).complete().editMessageComponents(FormatUtil.jumpButton(message)).queue();

                if (reroll) {
                    message.getChannel().sendMessageEmbeds(FormatUtil.rerollGiveawayEmbed(sb.toString(), host, reward).build()).queue();
                }
                else {
                    message.editMessage("\uD83C\uDF89 **GIVEAWAY ENDED** \uD83C\uDF89").queue();
                    message.editMessageEmbeds(FormatUtil.endedGiveawayEmbed(sb.toString(), host, reward).build()).queue();
                }

            }
            else {
                message.getChannel().sendMessage(BotEmoji.MENTION + host.getAsMention() + "\n" + BotEmoji.MENTION + mention).complete()
                        .editMessageComponents(FormatUtil.jumpButton(message)).queue();

                if (reroll) {
                    message.getChannel().sendMessageEmbeds(FormatUtil.noWinnersGiveawayEmbed(reward).build()).queue();
                }
                else {
                    message.editMessage("\uD83C\uDF89 **GIVEAWAY ENDED** \uD83C\uDF89").queue();
                    message.editMessageEmbeds(FormatUtil.noWinnersGiveawayEmbed(reward).build()).queue();
                }
            }
        });

        completed = true;

        if (future != null) future.cancel(false);

        future = Bot.getExecutor().schedule(() -> {
            manager.removeGiveaway(this);
            manager.writeGiveaways();
        }, 24, TimeUnit.HOURS);
    }

    public void cancel() {
        manager.removeGiveaway(this);
        manager.writeGiveaways();
        if (future != null) future.cancel(false);

        if (completed) return;
        init();

        completed = true;

        message.editMessage("_was a giveaway once..._").queue();
        message.editMessageEmbeds(FormatUtil.defaultEmbed("Giveaway canceled").build()).queue();
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

    protected void waitUntilEnd() {
        if (completed) return;

        try {
            Thread.sleep(((int) (end <= now ? 0 : end - now)) * 1000L);
        }
        catch (InterruptedException e) {
            log.warn("Thread did not sleep properly", e);
        }

        end(false);
    }

    protected JSONObject toJSON() {
        JSONObject o = new JSONObject();

        o.put("id", id);
        o.put("reward", reward);
        o.put("winners", winners);
        o.put("end", end);
        o.put("host_id", hostId);

        return o;
    }

    private void failed(Throwable t) throws RuntimeException {
        manager.removeGiveaway(this);
        manager.writeGiveaways();
        if (future != null) future.cancel(false);

        if (completed) return;

        completed = true;

        log.warn("Giveaway with id \"" + id + "\" failed", t);

        throw new RuntimeException(t);
    }

    private void init() {
        if (message == null) message = OtherUtil.findMessageById(id);
        if (host == null) host = OtherUtil.findMemberById(hostId);

        if (message == null || host == null) failed(null);

        Role role = Bot.getGuildConfig(message.getGuild()).getGiveaway(message.getGuild());
        mention = (role == null ? "@everyone": role.getAsMention());
    }

    private void checkIsCompleted() {
        if (end < now) {
            completed = true;

            if (future != null) {
                future.cancel(false);
            }
            future = Bot.getExecutor().schedule(() -> {
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
}