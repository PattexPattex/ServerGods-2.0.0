package com.pattexpattex.servergods2.commands.hidden;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class StopCmd implements BotHidden {

    public void run(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        event.getChannel().sendMessageEmbeds(FormatUtil.defaultEmbed(BotEmoji.YES + " Stopping!").build())
                .queue((success) -> Bot.shutdown());
    }
}
