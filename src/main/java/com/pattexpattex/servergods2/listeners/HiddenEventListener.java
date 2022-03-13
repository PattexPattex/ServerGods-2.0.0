package com.pattexpattex.servergods2.listeners;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.commands.hidden.BotHidden;
import com.pattexpattex.servergods2.commands.hidden.ClearCmd;
import com.pattexpattex.servergods2.commands.hidden.StopCmd;
import com.pattexpattex.servergods2.config.Config;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Here are registered commands that are hidden to the end-user
 * or designed to be executed only by the bots' owner.
 * These commands include, but are not limited to {@link StopCmd} and {@link ClearCmd}.
 * @since 1.0.0, but modified
 * @see BotHidden
 * */
public class HiddenEventListener extends ListenerAdapter {

    private static final Map<String, BotHidden> cmdList = new HashMap<>();
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s");
    private final String prefix;

    /**
     * @param prefix The prefix used for the hidden commands ($$$, !, s!, etc.)
     */
    public HiddenEventListener(String prefix) {

        // Sets the prefix
        this.prefix = prefix;

        // Registers the commands
        cmdList.put("stop", new StopCmd());
        cmdList.put("clear", new ClearCmd());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        // Some formatting
        Message msg = event.getMessage();
        Message reply;
        String raw = msg.getContentRaw();

        // Some checks
        if (msg.getAuthor().isBot()) return;
        if (!raw.startsWith(prefix)) return;

        // More formatting
        String msgWithoutPrefix = raw.substring(prefix.length());
        String[] split = SPACE_PATTERN.split(msgWithoutPrefix);
        String cmdName = split[0];
        BotHidden cmd = cmdList.get(cmdName);

        // Some more checks
        if (cmd == null) return;
        if (msg.getAuthor().getId().equals(Bot.getConfig().getConfigValue(Config.ConfigValues.BOT_OWNER))) {
            // Actually tries to execute the command

            try {
                // Creates the array of command arguments
                String[] args = Stream.of(split).skip(1).toArray(String[]::new);

                cmd.run(event, args);
            } catch (Exception e) {
                reply = msg.replyEmbeds(FormatUtil.errorEmbed(e, this.getClass()).build()).complete();
                FormatUtil.deleteAfter(reply, 30);
            }

            // Deletes the command message if it was created in a guild
            if (event.isFromGuild()) FormatUtil.delete(msg);
        }
    }
}
