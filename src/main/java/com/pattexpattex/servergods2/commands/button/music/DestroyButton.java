package com.pattexpattex.servergods2.commands.button.music;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class DestroyButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {

        event.deferEdit().queue();

        Guild guild = Objects.requireNonNull(event.getGuild());

        Bot.getKvintakord().getDiscordManager().removeLastQueueMessage(guild);

        event.getHook().editOriginalComponents(Collections.emptyList()).setEmbeds(FormatUtil.kvintakordEmbed(Emotes.YES + " Interaction ended").build()).queue();
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.DANGER;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "End interaction";
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.destroy";
    }
}
