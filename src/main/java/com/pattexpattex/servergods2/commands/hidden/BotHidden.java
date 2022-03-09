package com.pattexpattex.servergods2.commands.hidden;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public interface BotHidden {

    void run(@NotNull MessageReceivedEvent event, @NotNull String[] args);

}
