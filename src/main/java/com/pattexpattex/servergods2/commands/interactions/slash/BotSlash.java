package com.pattexpattex.servergods2.commands.interactions.slash;

import com.pattexpattex.servergods2.Bot;
import com.pattexpattex.servergods2.config.Config;
import com.pattexpattex.servergods2.listeners.interactions.SlashEventListener;
import com.pattexpattex.servergods2.util.BotException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * This interface is used to simplify the creation of {@code Slash} commands.
 *
 * @since 1.0.0, but strongly modified
 * @see SlashCommandEvent
 * */
public interface BotSlash {

    /**
     * This method adds or removes a command from a {@code Guild}
     * or the global environment (DMs with the bot).
     * The following case should never actually be executed because
     * no command is enabled globally.
     *
     * @param cmd the {@code BotSlash} instance that is currently being set up
     * @since 2.1.0
     * */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    default void setup(@NotNull BotSlash cmd) {

        if (!cmd.isEnabledGlobal()) {

            // Sets up a command in all guilds
            Bot.getJDA().getGuilds().forEach((guild) -> {

                // Adds a command to a guild
                if (isEnabledInGuild(guild)) {
                    CommandCreateAction action = guild.upsertCommand(cmd.getName(), cmd.getDesc());

                    if (getSubcommands() != null || getSubcommandGroups() != null) {
                        if (getSubcommands() != null) action.addSubcommands(getSubcommands());
                        if (getSubcommandGroups() != null) action.addSubcommandGroups(getSubcommandGroups());
                    }
                    else if (getOptions() != null) {
                        action.addOptions(getOptions());
                    }

                    action.queue();
                }

                // The command is not enabled in a guild
                else {
                    List<Command> cmdList = guild.retrieveCommands().complete();

                    for (Command cmd1 : cmdList) {

                        // Deletes the command in a guild
                        if (cmd1.getName().equals(cmd.getName())) {
                            cmd1.delete().queue();
                            break;
                        }
                    }
                }
            });
        }
        // This should never happen, but if it does, oops
        else {
            Bot.getJDA().upsertCommand(cmd.getName(), cmd.getDesc())
                        .addOptions(getOptions())
                        .complete();
        }
    }

    /**
     * @param event a {@link SlashCommandEvent}
     * @since 2.1.0
     * */
    void run(@NotNull SlashCommandEvent event) throws Exception;

    /**
     * Because of the implementation, this string must be identical to the command reference in
     * {@link Config.Commands}, {@code config.json} and {@code server_config.json}.
     *
     * @return the command's name
     * @since 2.1.0
     * */
    String getName();

    /**
     * @return the command's description
     * @since 2.1.0
     * */
    String getDesc();

    /**
     * This method checks whether this command is enabled in the specified guild.
     *
     * @param guild the {@code Guild} to check for
     * @return {@code true} if it is enabled, {@code false} instead
     * @since 2.1.0
     * */
    default boolean isEnabledInGuild(@NotNull Guild guild) {
        boolean b = false;

        for (Object cmd : Bot.getGuildConfig(guild).getCommands()) {
            if (cmd.equals(getName())) {
                b = true;
                break;
            }
        }

        return b;
    }

    /**
     * @return should always return {@code false}
     * */
    default boolean isEnabledGlobal() {
        return false;
    }

    /**
     * This method checks the global config ({@code config.json}) if the command is enabled.
     *
     * @param cmd the {@code BotSlash} instance that is being checked
     * @return {@code true} if enabled, else {@code false}
     * */
    default boolean isEnabled(@NotNull BotSlash cmd) {
        return Bot.getConfig().isCmdEnabled(Config.Commands.valueOf(cmd.getName().toUpperCase()));
    }

    /**
     * @return the command's options
     * @see OptionData
     * */
    default OptionData[] getOptions() {
        return null;
    }

    /**
     * @return the command's subcommand groups
     * @see Command.SubcommandGroup
     */
    default SubcommandGroupData[] getSubcommandGroups() {
        return null;
    }

    /**
     * @return the commands subcommands
     * @see Command.Subcommand
     */
    default SubcommandData[] getSubcommands() {
        return null;
    }

    /**
     * @return the {@link Permission Permissions} required for this command
     */
    default Permission[] getPermissions() {
        return new Permission[]{};
    }

    /**
     * @return the {@link Permission Permissions} required for the bot to succeed at this command
     */
    default Permission[] getSelfPermissions() {
        return Bot.getRecommendedPermissions();
    }

    /**
     * @param throwable the {@code Throwable} returned by {@link net.dv8tion.jda.api.requests.RestAction#queue(Consumer, Consumer) RestAction.queue()}
     * @throws BotException an exception that will be caught by {@link SlashEventListener}
     */
    @Contract("_ -> fail")
    default void rethrow(Throwable throwable) {
        throw new BotException(throwable);
    }
}
