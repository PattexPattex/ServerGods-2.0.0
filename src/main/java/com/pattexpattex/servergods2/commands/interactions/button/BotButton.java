package com.pattexpattex.servergods2.commands.interactions.button;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * This interface is used to construct {@code Buttons} used for bot interactions.
 *
 * @since 2.1.0
 * @see net.dv8tion.jda.api.interactions.components.Button
 * */
public interface BotButton extends Button {

    /**
     * @param event a {@link ButtonClickEvent}
     * @since 2.1.0
     * */
    void run(@NotNull ButtonClickEvent event) throws Exception;

    default @NotNull ButtonStyle getStyle() {
        return ButtonStyle.PRIMARY;
    }

    default @NotNull Type getType() {
        return Type.BUTTON;
    }

    default @Nullable String getUrl() {
        return null;
    }

    default @Nullable Emoji getEmoji() {
        return null;
    }

    default @Nonnull String getLabel() {
        return "";
    }

    default @Nonnull DataObject toData() {
        DataObject json = DataObject.empty();
        json.put("type", 2);
        json.put("label", getLabel());
        json.put("style", getStyle().getKey());
        json.put("disabled", isDisabled());
        if (getEmoji() != null) json.put("emoji", getEmoji());
        if (getUrl() != null)
            json.put("url", getUrl());
        else
            json.put("custom_id", getId());
        return json;
    }

    default boolean isDisabled() {
        return false;
    }
}
