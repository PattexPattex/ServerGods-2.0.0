package com.pattexpattex.servergods2.listeners.interactions;

import com.pattexpattex.servergods2.commands.interactions.selection.BotSelection;
import com.pattexpattex.servergods2.commands.interactions.selection.roles.GetRolesSelect;
import com.pattexpattex.servergods2.commands.interactions.selection.roles.SetRolesSelect;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SelectionEventListener extends ListenerAdapter {

    private static final List<BotSelection> selectionList = new ArrayList<>();

    /**
     * This listener registers amd executes the code of {@code BotSelections} in this bot.
     *
     * @since 2.1.0
     * @see BotSelection
     * @see ListenerAdapter
     */
    public SelectionEventListener() {

        // Registers the BotSelections
        selectionList.add(new GetRolesSelect());
        selectionList.add(new SetRolesSelect());
    }

    @Override
    public void onSelectionMenu(@Nonnull SelectionMenuEvent event) {

        // Acknowledges the event
        event.deferEdit().complete();

        for (BotSelection selection : selectionList) {

            if (Objects.requireNonNull(Objects.requireNonNull(event.getComponent()).getId()).equals(selection.getId())) {

                try {
                    selection.run(event);
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

                break;
            }
        }
    }

    public static BotSelection addSelection(BotSelection selection) {
        selectionList.add(selection);

        return selection;
    }
}
