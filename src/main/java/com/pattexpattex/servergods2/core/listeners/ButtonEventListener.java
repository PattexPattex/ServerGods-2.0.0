package com.pattexpattex.servergods2.core.listeners;

import com.pattexpattex.servergods2.commands.button.music.*;
import com.pattexpattex.servergods2.core.commands.BotButton;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * All {@link BotButton button} interactions are registered and executed in from this class.
 * @since 2.1.0
 * @see BotButton
 * */
public class ButtonEventListener extends ListenerAdapter {

    private static final List<BotButton> buttons = new ArrayList<>();

    public ButtonEventListener() {

        /*
         Register the buttons
         (if there are any lol)
        */
        buttons.add(new PauseButton());
        buttons.add(new SkipButton());
        buttons.add(new LoopButton());
        buttons.add(new StopButton());
        buttons.add(new RefreshButton());
        buttons.add(new DestroyButton());
        buttons.add(new ClearButton());
        buttons.add(new LyricsButton());
        buttons.add(new PreviousPageButton());
        buttons.add(new NextPageButton());
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {

        // Iterates through the registered buttons
        for (BotButton button : buttons) {

            // If the click event ID equals the button's ID, executes the run() method
            if (Objects.requireNonNull(Objects.requireNonNull(event.getButton()).getId()).equals(button.getId())) {

                try {
                    button.run(event);
                }
                catch (Exception e) {
                    MessageEmbed embed = FormatUtil.errorEmbed(e, this.getClass()).build();

                    if (e instanceof BotException && ((BotException) e).canOverwriteReply()) {
                        event.replyEmbeds(embed).setEphemeral(true).queue(null,
                                (f) -> event.getHook().editOriginalEmbeds(embed).queue((msg) ->
                                {
                                    msg.editMessageComponents(Collections.emptyList()).queue();

                                    if (!msg.isEphemeral()) {
                                        msg.clearReactions().queue();
                                    }
                                }));
                    }
                    else {
                        event.replyEmbeds(embed).setEphemeral(true).queue(null,
                                (f) -> event.getChannel().sendMessageEmbeds(embed).queue());
                    }

                    OtherUtil.handleBotException(event, e);
                }

                // After completion breaks the loop
                break;
            }
        }
    }
}
