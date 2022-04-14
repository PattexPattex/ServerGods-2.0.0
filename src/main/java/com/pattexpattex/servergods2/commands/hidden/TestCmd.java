package com.pattexpattex.servergods2.commands.hidden;

import com.pattexpattex.servergods2.core.commands.BotHidden;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class TestCmd extends BotHidden {

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        event.getChannel().sendMessage("test? no").queue();
    }
}
