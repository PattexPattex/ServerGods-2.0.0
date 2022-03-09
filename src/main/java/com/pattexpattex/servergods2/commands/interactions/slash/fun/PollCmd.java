package com.pattexpattex.servergods2.commands.interactions.slash.fun;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PollCmd implements BotSlash {

    private static final String[] reactions = {"\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3", "\u0035\u20E3"}; //One, two, three, four, five

    @Override
    public void run(@NotNull SlashCommandEvent event) {

        event.deferReply(true).queue();

        String question = Objects.requireNonNull(event.getOption("question")).getAsString();
        List<String> options = new ArrayList<>(List.of(
                Objects.requireNonNull(event.getOption("option-1")).getAsString(),
                Objects.requireNonNull(event.getOption("option-2")).getAsString()));

        if (event.getOption("option-3") != null) options.add(Objects.requireNonNull(event.getOption("option-3")).getAsString());
        if (event.getOption("option-4") != null) options.add(Objects.requireNonNull(event.getOption("option-4")).getAsString());
        if (event.getOption("option-5") != null) options.add(Objects.requireNonNull(event.getOption("option-5")).getAsString());

        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Created a new poll").build()).complete();

        new Poll(event, question, options).start();
    }

    protected record Poll(SlashCommandEvent event, String question, List<String> options) {

        public void start() {
            Guild guild = event.getGuild();
            EmbedBuilder builder = FormatUtil.defaultEmbed(null, question);

            Role role = Bot.getGuildConfig(guild).getPoll(guild);
            String mention = (role != null ? role.getAsMention() : "@everyone") + "\n\n";

            for (int i = 0; options.size() > i; i++) {
                builder.appendDescription(reactions[i] + " " + options.get(i) + "\n");
            }

            event.getChannel().sendMessage(mention + "\u2753 **POLL** \u2754").queue((msg) -> {
                msg.editMessageEmbeds(builder.build()).queue();

                for (int i = 0; options.size() > i; i++) {
                    msg.addReaction(reactions[i]).queue();
                }
            });
        }
    }

    @Override
    public String getName() {
        return "poll";
    }

    @Override
    public String getDesc() {
        return "Creates a poll";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "question", "The poll's question", true),

                new OptionData(OptionType.STRING, "option-1", "First option", true),
                new OptionData(OptionType.STRING, "option-2", "Second option", true),
                new OptionData(OptionType.STRING, "option-3", "Third option"),
                new OptionData(OptionType.STRING, "option-4", "Fourth option"),
                new OptionData(OptionType.STRING, "option-5", "Fifth option")
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }
}
