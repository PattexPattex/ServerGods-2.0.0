package com.pattexpattex.servergods2.commands.button.music;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.core.kvintakord.discord.KvintakordDiscordManager;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class ClearButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {
        KvintakordDiscordManager.checkAllConditions(Objects.requireNonNull(event.getMember()));

        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (KvintakordDiscordManager.isNotLastQueueMessage(guild, event.getMessage()) || !Bot.getKvintakord().isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).complete()
                    .editMessageComponents(Collections.emptyList()).queue();
            return;
        }

        Bot.getKvintakord().clearQueue(guild);

        KvintakordDiscordManager.updateLastQueueMessage(guild);
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.DANGER;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Clear queue";
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.clear";
    }
}
