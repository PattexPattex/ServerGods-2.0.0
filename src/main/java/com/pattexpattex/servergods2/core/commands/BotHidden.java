package com.pattexpattex.servergods2.core.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public abstract class BotHidden {

    public abstract void run(@NotNull MessageReceivedEvent event, @NotNull String[] args);

}
