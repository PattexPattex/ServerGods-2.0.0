package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.giveaway.Giveaway;
import com.pattexpattex.servergods2.core.giveaway.GiveawayManager;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class GiveawayCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {

        String subcommand = event.getCommandPath();
        Guild guild = event.getGuild();
        event.deferReply(true).queue();

        GiveawayManager manager = Bot.getGiveawayManager();

        switch (subcommand) {
            case "giveaway/new" -> {
                User host = event.getOption("host") != null ? Objects.requireNonNull(event.getOption("host")).getAsUser() : event.getUser();
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

                long end = Instant.now().getEpochSecond() + time;

                Role role = Bot.getGuildConfig(guild).getGiveaway(guild);
                String mention = (role != null ? role.getAsMention() : "@everyone");

                event.getChannel().sendMessage(Emotes.HOURGLASS + " `Setting up a new giveaway...`\n\n" + mention).queue((msg) ->
                {
                    msg.addReaction("\uD83C\uDF89").queue();
                    try {
                        new Giveaway(Bot.getGiveawayManager(), msg.getIdLong(), host.getIdLong(), Objects.requireNonNull(guild).getIdLong(), reward, winners, end).start();
                    }
                    catch (RuntimeException e) {
                        throw new BotException(e);
                    }
                });

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Started a giveaway").build()).queue();
            }
            case "giveaway/reroll" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();
                Integer winners = event.getOption("winners") != null ? (int) Objects.requireNonNull(event.getOption("winners")).getAsLong() : null;

                Giveaway giveaway = manager.getGiveaway(giveawayId);
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

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Rerolled the giveaway").build()).queue();
            }
            case "giveaway/end" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();

                Giveaway giveaway = manager.getGiveaway(giveawayId);
                if (giveaway == null) {
                    throw new BotException("Giveaway not found");
                }

                try {
                    giveaway.end(false);
                }
                catch (RuntimeException e) {
                    throw new BotException(e);
                }

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Ended the giveaway").build()).queue();
            }
            case "giveaway/cancel" -> {
                long giveawayId = Objects.requireNonNull(event.getOption("message-id")).getAsLong();

                Giveaway giveaway = manager.getGiveaway(giveawayId);
                if (giveaway == null) {
                    throw new BotException("Giveaway not found");
                }

                giveaway.cancel();

                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Cancelled the giveaway").build()).queue();
            }
            case "giveaway/active" -> {
                long messageId = event.getOption("message-id") != null ? Objects.requireNonNull(event.getOption("message-id")).getAsLong() : -1;

                if (messageId == -1) {
                    Map<Long, Giveaway> giveaways = Bot.getGiveawayManager().getGiveaways(guild.getIdLong());

                    if (giveaways.isEmpty()) throw new BotException("No active giveaways");

                    EmbedBuilder builder = FormatUtil.defaultEmbed(null, "Active Giveaways");
                    int i = 0;

                    for (Giveaway giveaway : giveaways.values()) {
                        i++;

                        builder.appendDescription(String.format(
                                "**%d.** **%s** (%d winners) by %s, %s, id: _`%d`_",
                                i,
                                giveaway.getReward(),
                                giveaway.getWinners(),
                                giveaway.getHost().getAsMention(),
                                (giveaway.isCompleted() ? "ended on " : "ends on ") + FormatUtil.epochTimestamp(giveaway.getEnd()),
                                giveaway.getGiveawayId()
                        ));
                    }

                    event.getHook().editOriginalEmbeds(builder.build()).queue();
                }
                else {
                    Giveaway giveaway = Bot.getGiveawayManager().getGiveaway(messageId);

                    if (giveaway == null) {
                        throw new BotException("No giveaway with id " + messageId);
                    }

                    EmbedBuilder builder = FormatUtil.defaultEmbed(null, giveaway.getReward());

                    builder.appendDescription(String.format(
                            "%d winners\nBy %s\n%s\nId: _`%d`_",
                            giveaway.getWinners(),
                            giveaway.getHost().getAsMention(),
                            (giveaway.isCompleted() ? "ended on " : "ends on ") + FormatUtil.epochTimestamp(giveaway.getEnd()),
                            giveaway.getGiveawayId()
                    ));

                    event.getHook().editOriginalEmbeds(builder.build()).queue();
                }
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
                        .addOption(OptionType.STRING, "message-id", "Message ID of the giveaway to cancel", true),
                new SubcommandData("active", "Retrieve active giveaways")
                        .addOption(OptionType.STRING, "message-id", "Retrieve info only for a giveaway with the given ID", false)
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
