package com.pattexpattex.servergods2.commands.button.music;

import com.pattexpattex.servergods2.core.Kvintakord;
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

public class StopButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {
        Kvintakord.checkAllConditions(Objects.requireNonNull(event.getMember()));

        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Kvintakord.isNotLastQueueMessage(guild, event.getMessage()) || !Kvintakord.isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).complete()
                    .editMessageComponents(Collections.emptyList()).queue();
            return;
        }

        Kvintakord.stop(guild);
    }

    @Override
    public @Nullable Emoji getEmoji() {
        return Emoji.fromUnicode("\u23F9\uFE0F");
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.SECONDARY;
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.stop";
    }
}
