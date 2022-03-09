package com.pattexpattex.servergods2.commands.interactions.slash;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.listeners.interactions.SlashEventListener;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AboutCmd implements BotSlash {

    /* If you are editing the code, could you not change this string?
     * Give some credit to the developer's hard work, or change it,
     * I'm just a comment... */
    private static final String Developer = "<@!714406547161350155>";

    /* You can put your name here, then both you and the developer are credited for this!
     * To get a mention like the one above, send \@username in a message channel in Discord.
     * To list multiple people, separate them by commas. */
    private static final String OtherDevelopers = "";

    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        EmbedBuilder builder = FormatUtil.defaultEmbed("Hi! I am " + event.getJDA().getSelfUser().getAsMention() + " and I was developed by " + Developer +
                (!OtherDevelopers.isBlank() ? " and additionally by " + OtherDevelopers : "") + ". Here is what I can do!", "Help & Info", "DEFAULT", null);

        Guild guild = Objects.requireNonNull(event.getGuild());
        List<String> guildCommands = Bot.getGuildConfig(guild).getCommands();
        List<BotSlash> enabledCommands = SlashEventListener.getEnabledCommands();
        Map<String, String> commandNames = new HashMap<>();

        for (BotSlash command : enabledCommands) {
            if (guildCommands.contains(command.getName())) {
                commandNames.putIfAbsent(command.getName(), command.getDesc());
            }
        }

        commandNames.forEach((name, description) -> builder.addField(name, description, false));

        event.getHook().editOriginalEmbeds(builder.build()).complete().editMessageComponents(ActionRow.of(
                Button.link("https://github.com/PattexPattex/ServerGods", "Github").withEmoji(Emoji.fromEmote("github", 934755405802930176L, false)),
                OtherUtil.getInviteButton())).queue();
    }

    public String getName() {
        return "about";
    }

    public String getDesc() {
        return "A page about the bot";
    }
}
