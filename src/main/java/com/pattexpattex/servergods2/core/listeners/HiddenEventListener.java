package com.pattexpattex.servergods2.core.listeners;

import com.pattexpattex.servergods2.commands.hidden.ClearCmd;
import com.pattexpattex.servergods2.commands.hidden.GetCmd;
import com.pattexpattex.servergods2.commands.hidden.StopCmd;
import com.pattexpattex.servergods2.commands.hidden.TestCmd;
import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotHidden;
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

        //Sets the prefix
        this.prefix = prefix;

        //Registers the commands
        cmdList.put("stop", new StopCmd());
        cmdList.put("clear", new ClearCmd());
        cmdList.put("get", new GetCmd());
        cmdList.put("test", new TestCmd());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        //Some formatting
        Message msg = event.getMessage();
        String raw = msg.getContentRaw();

        //Some checks
        if (msg.getAuthor().isBot()) return;
        if (!raw.startsWith(prefix)) return;

        //More formatting
        String msgWithoutPrefix = raw.substring(prefix.length());
        String[] split = SPACE_PATTERN.split(msgWithoutPrefix);
        String cmdName = split[0];
        BotHidden cmd = cmdList.get(cmdName);
        String owner = Bot.getConfig().getConfigValue("bot_owner");

        //Some more checks
        if (cmd == null) return;
        if (owner.isBlank() && !msg.getAuthor().getAsMention().equals(Bot.Developer)) return;
        else if (!msg.getAuthor().getId().equals(owner)) return;

        //Creates the array of command arguments, without the command name
        String[] args = Stream.of(split).skip(1).toArray(String[]::new);

        try {
            cmd.run(event, args);
        }
        catch (Exception e) {
            event.getChannel().sendMessageEmbeds(FormatUtil.errorEmbed(e, this.getClass()).build()).queue();
        }
        finally {
            //Deletes the command message if it was created in a guild
            if (event.isFromGuild()) FormatUtil.delete(msg);
        }
    }
}
