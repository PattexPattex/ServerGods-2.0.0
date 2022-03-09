package com.pattexpattex.servergods2.commands.interactions.slash.fun;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GiveawayCmd implements BotSlash {

    private static final Map<Long, Giveaway> giveaways = new HashMap<>();

    @Override
    public void run(@NotNull SlashCommandEvent event) throws Exception {

        String subcommand = event.getCommandPath();
        Guild guild = event.getGuild();
        event.deferReply(true).queue();

        switch (subcommand) {
            case "giveaway/new" -> {
                Member host = event.getOption("host") != null ? Objects.requireNonNull(event.getOption("host")).getAsMember() : event.getMember();
                int winners = event.getOption("winners") != null ? (int) Objects.requireNonNull(event.getOption("winners")).getAsLong() : 1;
                String reward = Objects.requireNonNull(event.getOption("reward")).getAsString();
                String timeRaw = Objects.requireNonNull(event.getOption("time")).getAsString();

                int time;
                try {
                    time = (int) FormatUtil.decodeTimeAlternate(timeRaw);
                }
                catch (NumberFormatException | IndexOutOfBoundsException e) {
                    throw new BotException(e);
                }

                Role role = Bot.getGuildConfig(guild).getGiveaway(guild);
                String mention = (role != null ? role.getAsMention() : "@everyone");

                event.getChannel().sendMessage(BotEmoji.LOADING + " `Setting up a new giveaway...`\n\n" + mention).queue((msg) ->
                {
                    msg.addReaction("\uD83C\uDF89").queue();
                    try {
                        giveaways.put(msg.getIdLong(), new Giveaway(msg, reward, winners, time, host).begin());
                    }
                    catch (RuntimeException e) {
                        throw new BotException(e);
                    }
                });

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Started a giveaway").build()).queue();
            }
            case "giveaway/reroll" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();
                Integer winners = event.getOption("winners") != null ? (int) Objects.requireNonNull(event.getOption("winners")).getAsLong() : null;

                Giveaway giveaway = giveaways.get(giveawayId);
                if (giveaway == null) {
                    throw new BotException("Giveaway not found");
                }

                try {
                    if (winners == null) {
                        giveaway.reroll();
                    } else {
                        giveaway.reroll(winners);
                    }
                }
                catch (RuntimeException e) {
                    throw new BotException(e);
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Rerolled the giveaway").build()).queue();
            }
            case "giveaway/end" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();

                Giveaway giveaway = giveaways.get(giveawayId);
                if (giveaway == null) {
                    throw new BotException("Giveaway not found");
                }

                try {
                    giveaway.end();
                }
                catch (RuntimeException e) {
                    throw new BotException(e);
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Ended the giveaway").build()).queue();
            }
            case "giveaway/cancel" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();

                Giveaway giveaway = giveaways.remove(giveawayId);
                if (giveaway == null) {
                    throw new BotException("Giveaway not found");
                }

                giveaway.cancel();

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Cancelled the giveaway").build()).queue();
            }
        }
    }

    private static class Giveaway extends Thread {

        protected Message message;
        protected final long messageId;

        protected String reward;
        protected int winners;
        protected int time;
        protected Member host;
        protected boolean completed;

        private final String mention;

        private static final Logger log = LoggerFactory.getLogger(Giveaway.class);

        public Giveaway(Message message, String reward, int winners, int time, Member host) {
            this.message = message;
            this.reward = reward;
            this.winners = winners;
            this.time = time;
            this.host = host;

            this.completed = false;
            this.messageId = message.getIdLong();

            Role role = Bot.getGuildConfig(message.getGuild()).getGiveaway(message.getGuild());
            mention = (role != null ? role.getAsMention() : "@everyone");
        }

        @Override
        @SuppressWarnings("BusyWait")
        public void run() {
            try {
                message.editMessage(mention + "\n\n\uD83C\uDF89 **GIVEAWAY** \uD83C\uDF89").complete();
            }
            catch (RuntimeException e) {
                completed = true;
                throw e;
            }


            while (time > 0) {
                if (time > 5) {
                    message.editMessageEmbeds(FormatUtil.runningGiveawayEmbed(winners, time, reward).build())
                            .queue((msg) -> {
                                time -= 5;
                                message = msg;
                            }, (f) -> time = -1);

                    if (time == -1) {
                        completed = true;
                        return;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.warn("Thread did not sleep properly", e);
                    }
                }
                else {
                    message.editMessageEmbeds(FormatUtil.runningGiveawayEmbed(winners, time, reward).build())
                            .queue((msg) -> {
                                time--;
                                message = msg;
                            }, (f) -> time = -1);

                    if (time == -1) {
                        completed = true;
                        return;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.warn("Thread did not sleep properly", e);
                    }
                }
            }

            end();
        }

        public void end() {
            end(false);
        }

        public void end(boolean reroll) {
            if (!completed) {

                time = 0;

                try {
                    message = message.getChannel().retrieveMessageById(messageId).complete();
                }
                catch (RuntimeException e) {
                    completed = true;
                    throw e;
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
            }

            completed = true;
        }

        public void cancel() {
            if (!completed) {
                completed = true;
                time = -1;
                message.delete().queue();
            }
        }

        public Giveaway begin() {
            completed = false;
            start();
            return this;
        }

        public void reroll() {
            reroll(winners);
        }

        public void reroll(int winners) {
            if (completed) {
                completed = false;

                if (winners != this.winners) {
                    this.winners = winners;
                }

                end(true);
            }
        }
    }

    @Override
    public String getName() {
        return "giveaway";
    }

    @Override
    public String getDesc() {
        return "A command about giveaways";
    }

    @Override
    public SubcommandData[] getSubcommands() {
        return new SubcommandData[]{
                new SubcommandData("new", "Create a new giveaway")
                        .addOption(OptionType.STRING, "reward", "Reward of the giveaway", true)
                        .addOption(OptionType.STRING, "time", "Time of the giveaway, e.g.: 1d 2h 3m 4s", true)
                        .addOption(OptionType.USER, "host", "Host of the giveaway (you by default)", false)
                        .addOption(OptionType.INTEGER, "winners", "Amount of winners (1 by default)", false),
                new SubcommandData("reroll", "Re-roll a giveaway")
                        .addOption(OptionType.STRING, "message-id", "Message ID of the giveaway to reroll", true)
                        .addOption(OptionType.INTEGER, "winners", "New amount of winners", false),
                new SubcommandData("end", "End a giveaway now")
                        .addOption(OptionType.STRING, "message-id", "Message ID of the giveaway to end", true),
                new SubcommandData("cancel", "Cancel a giveaway")
                        .addOption(OptionType.STRING, "message-id", "Message ID of the giveaway to cancel", true)
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
