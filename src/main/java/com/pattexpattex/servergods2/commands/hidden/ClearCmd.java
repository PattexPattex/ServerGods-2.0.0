package com.pattexpattex.servergods2.commands.hidden;

import com.pattexpattex.servergods2.core.commands.BotHidden;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class ClearCmd extends BotHidden {

    public void run(@NotNull MessageReceivedEvent event, @NotNull String[] args) {

        event.getChannel().sendMessageEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Cleared all commands!").build()).queue();

        event.getJDA().updateCommands().queue();
        event.getJDA().getGuilds().forEach((guild) -> guild.updateCommands().queue());
    }
}
