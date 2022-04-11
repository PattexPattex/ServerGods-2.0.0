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

public class NextPageButton extends BotButton {

    @Override
    public void run(@NotNull ButtonClickEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());

        event.deferEdit().queue();

        if (Bot.getKvintakord().getDiscordManager().updateLastQueueMessage(guild, event, Bot.getKvintakord().getDiscordManager().currentPage(guild) + 1) && Bot.getKvintakord().isPlaying(guild)) return;

        event.getHook().editOriginalEmbeds(FormatUtil.kvintakordEmbed(BotEmoji.YES + " Interaction ended").build()).setActionRows(Collections.emptyList()).queue();
    }

    public static NextPageButton getInstance(Guild guild) {
        if (Bot.getKvintakord().getDiscordManager().isNextPage(guild)) {
            return new NextPageButton();
        }

        return new Disabled();
    }

    @Nullable
    @Override
    public String getId() {
        return "button:music.nextpage";
    }

    @Override
    public @NotNull ButtonStyle getStyle() {
        return ButtonStyle.SECONDARY;
    }

    @Override
    public @Nullable Emoji getEmoji() {
        return Emoji.fromUnicode("\u25B6\uFE0F");
    }

    public static class Disabled extends NextPageButton {
        public boolean isDisabled() {
            return true;
        }
    }
}
