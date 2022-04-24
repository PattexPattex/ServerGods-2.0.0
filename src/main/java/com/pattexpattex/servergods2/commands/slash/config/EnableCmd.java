package com.pattexpattex.servergods2.commands.slash.config;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.listeners.SlashEventListener;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * This command enables other commands in a guild.
 * @since 2.1.0
 */
public class EnableCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {

        event.deferReply().queue();

        //Setup
        List<BotSlash> cmdList = SlashEventListener.getEnabledCommands();
        List<String> guildCmdList = Bot.getGuildConfig(event.getGuild()).getCommands();
        String cmdOption = Objects.requireNonNull(event.getOption("cmd")).getAsString().toLowerCase();

        //Checks if the command exists
        if (cmdList.stream().noneMatch(cmd -> cmd.getName().equals(cmdOption)))
            throw new BotException("Command " + cmdOption + " not found");

        //Checks if the command can be disabled/enabled
        if (cmdOption.equals(getName()))
            throw new BotException("Cannot modify this command");

        //Disable a command
        if (event.getOption("disable") != null && Objects.requireNonNull(event.getOption("disable")).getAsBoolean()) {

            if (guildCmdList.contains(cmdOption)) {
                guildCmdList.remove(cmdOption);
                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Disabled the `" + cmdOption + "` command!").build()).queue();
            }
            else throw new BotException("This command is already disabled");
        }

        //Enable a command
        else {
            if (!guildCmdList.contains(cmdOption)) {
                guildCmdList.add(cmdOption);
                event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed(Emotes.YES + " Enabled the `" + cmdOption + "` command!").build()).queue();
            }
            else throw new BotException("This command is already enabled");
        }

        //Write to server_config.json
        Bot.getGuildConfig(event.getGuild()).setCommands(guildCmdList);

        /* Sets up the command again
         * This actually adds / removes a command */
        for (BotSlash cmd : cmdList) {
            if (cmd.getName().equals(cmdOption)) {
                cmd.setup(cmd, event.getJDA());
                break;
            }
        }
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDesc() {
        return "Enable or disable a command in this server";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{
                // Which command to enable
                new OptionData(OptionType.STRING, "cmd", "Command to enable", true)
                        .addChoices(getChoices()),

                // Disable the command instead
                new OptionData(OptionType.BOOLEAN, "disable", "Whether to disable the command instead", false)
        };
    }

    @Override
    public Permission[] getPermissions() {
        return new Permission[]{
                Permission.MANAGE_SERVER
        };
    }

    private @NotNull List<Command.Choice> getChoices() {
        List<Command.Choice> choices = new ArrayList<>();

        SlashEventListener.getEnabledCommands().forEach((cmd) -> {
            if (!cmd.getName().equals(this.getName())) {
                choices.add(new Command.Choice(cmd.getName(), cmd.getName()));
            }
        });

        return choices;
    }
}
