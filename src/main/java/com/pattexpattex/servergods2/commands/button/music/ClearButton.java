package com.pattexpattex.servergods2.commands.button.music;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Emoji;
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
        Bot.getKvintakord().getDiscordManager().checkAllConditions(Objects.requireNonNull(event.getMember()));

        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Bot.getKvintakord().getDiscordManager().isNotLastQueueMessage(guild, event.getMessage()) || !Bot.getKvintakord().isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).setActionRows(Collections.emptyList()).queue();
            return;
        }

        Bot.getKvintakord().clearQueue(guild);

        Bot.getKvintakord().getDiscordManager().updateLastQueueMessage(guild, 0);
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.DANGER;
    }

    @Override
    public @Nullable Emoji getEmoji() {
        return Emoji.fromUnicode("\uD83E\uDDF9");
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.clear";
    }
}
