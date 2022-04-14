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

public class RefreshButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Bot.getKvintakord().getDiscordManager().updateLastQueueMessage(guild, event, Bot.getKvintakord().getDiscordManager().currentPage(guild)) && Bot.getKvintakord().isPlaying(guild)) return;

        event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(Emotes.YES + " Interaction ended").build()).setActionRows(Collections.emptyList()).queue();
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.PRIMARY;
    }

    @Override
    public @Nullable net.dv8tion.jda.api.entities.Emoji getEmoji() {
        return net.dv8tion.jda.api.entities.Emoji.fromUnicode("\uD83D\uDD04");
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.update";
    }
}
