package com.pattexpattex.servergods2.commands.button.music;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

public class SkipButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {
        Bot.getKvintakord().getDiscordManager().checkAllConditions(Objects.requireNonNull(event.getMember()));

        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Bot.getKvintakord().getDiscordManager().isNotLastQueueMessage(guild, event.getMessage()) || !Bot.getKvintakord().isPlaying(guild)) {
            event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(Emotes.YES + " Interaction ended").build()).setActionRows(Collections.emptyList()).queue();
            return;
        }

        if (Bot.getKvintakord().getQueue(guild).isEmpty() && Bot.getKvintakord().getLoop(guild) == Kvintakord.LoopMode.OFF) {
            Bot.getKvintakord().stop(guild);
        }
        else {
            Bot.getKvintakord().skipToTrack(0, guild);
        }

        Bot.getKvintakord().getDiscordManager().updateLastQueueMessage(guild, Bot.getKvintakord().getDiscordManager().currentPage(guild));
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.SECONDARY;
    }

    @Override
    public @Nullable net.dv8tion.jda.api.entities.Emoji getEmoji() {
        return net.dv8tion.jda.api.entities.Emoji.fromUnicode("\u23ED\uFE0F");
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.skip";
    }
}
