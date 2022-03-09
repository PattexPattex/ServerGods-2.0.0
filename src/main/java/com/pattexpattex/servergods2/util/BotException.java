package com.pattexpattex.servergods2.util;

import com.pattexpattex.servergods2.commands.interactions.button.BotButton;
import com.pattexpattex.servergods2.commands.interactions.selection.BotSelection;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;

/**
 * This exception is designed to be thrown by instances of {@link BotSlash},
 * {@link BotButton} and {@link BotSelection}. When this exception is thrown,
 * the throwing class determines whether to set {@code overwriteReply} to
 * {@code true} (default value) of {@code false}.
 * If it is set to {@code true}, the {@code ListenerAdapter} instance that caught this exception
 * will attempt to edit any reply to the command that that was already created by the throwing class.
 * If it fails (no reply was made yet), it will simply create a new reply.
 *
 * @implNote {@code overwriteReply} defaults to true, override this value using {@link BotException#setOverwriteReply(boolean)}.
 */
public class BotException extends RuntimeException {

    private boolean overwriteReply;

    public BotException() {
        super();

        overwriteReply = true;
    }

    public BotException(String message) {
        super(message);

        overwriteReply = true;
    }

    public BotException(String message, Throwable cause) {
        super(message, cause);

        overwriteReply = true;
    }

    public BotException(Throwable cause) {
        super(cause);

        overwriteReply = true;
    }

    protected BotException(String message, Throwable cause,
                           boolean enableSuppression,
                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

        overwriteReply = true;
    }

    public boolean canOverwriteReply() {
        return overwriteReply;
    }

    public BotException setOverwriteReply(boolean val) {
        overwriteReply = val;

        return this;
    }
}
