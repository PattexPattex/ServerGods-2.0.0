package com.pattexpattex.servergods2.commands.interactions.button.music;

import com.pattexpattex.servergods2.Kvintakord;
import com.pattexpattex.servergods2.commands.interactions.button.BotButton;
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

public class SkipButton implements BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {

        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Kvintakord.isNotLastQueueMessage(guild, event.getMessage()) || !Kvintakord.isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).complete()
                    .editMessageComponents(Collections.emptyList()).queue();
            return;
        }

        if (Kvintakord.getQueue(guild).isEmpty() && Kvintakord.getLoop(guild) == Kvintakord.LoopMode.OFF) {
            Kvintakord.stop(guild);
        }
        else {
            Kvintakord.skipToTrack(0, guild);
        }

        Kvintakord.updateLastQueueMessage(guild);
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.SECONDARY;
    }

    @Override
    public @Nullable Emoji getEmoji() {
        return Emoji.fromUnicode("\u23ED\uFE0F");
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.skip";
    }
}
