package com.pattexpattex.servergods2.listeners.interactions;

import com.pattexpattex.servergods2.commands.interactions.slash.AboutCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.BotSlash;
import com.pattexpattex.servergods2.commands.interactions.slash.PingCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.config.ConfigCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.config.EnableCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.fun.*;
import com.pattexpattex.servergods2.commands.interactions.slash.moderation.BanCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.moderation.KickCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.moderation.MuteCmd;
import com.pattexpattex.servergods2.commands.interactions.slash.moderation.WakeUpCmd;
import com.pattexpattex.servergods2.util.BotException;
import com.pattexpattex.servergods2.util.FormatUtil;
import com.pattexpattex.servergods2.util.OtherUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This is the listener that executes the bots' {@link BotSlash slash commands}.
 * @since 2.1.0
 * @see BotSlash
 * */
public class SlashEventListener extends ListenerAdapter {

    private static final List<BotSlash> cmdList = new ArrayList<>();
    private static final List<BotSlash> cmdListEnabled = new ArrayList<>();

    public SlashEventListener() {

        //Create instances of all commands
        cmdList.add(new ConfigCmd());
        cmdList.add(new EnableCmd());
        cmdList.add(new AboutCmd());

        cmdList.add(new BanCmd());
        cmdList.add(new KickCmd());
        cmdList.add(new MuteCmd());
        cmdList.add(new WakeUpCmd());

        cmdList.add(new RolesCmd());
        cmdList.add(new PingCmd());
        cmdList.add(new InviteCmd());
        cmdList.add(new GiveawayCmd());
        cmdList.add(new PollCmd());
        cmdList.add(new RickrollCmd());
        cmdList.add(new MusicCmd());
        cmdList.add(new EmoteCmd());
        cmdList.add(new UserCmd());
        cmdList.add(new AvatarCmd());

        //Enables the commands given in the config.json
        cmdList.forEach((cmd) -> {
            if (cmd.isEnabled(cmd)) cmdListEnabled.add(cmd);
        });
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {

        /* This is commented out, because no command should be enabled globally */
        //event.getJDA().updateCommands().queue();

        /* Clears all commands in all servers */
        //event.getJDA().getGuilds().forEach((guild) -> guild.updateCommands().queue());

        List<String> disabled = new ArrayList<>();

        cmdList.forEach((cmd) -> {
            if (!cmdListEnabled.contains(cmd)) {
                disabled.add(cmd.getName());
            }
        });

        event.getJDA().getGuilds().forEach((guild) -> {
            List<Command> commands = guild.retrieveCommands().complete().stream()
                    .filter((cmd) -> cmd.getApplicationId().equals(event.getJDA().getSelfUser().getApplicationId())).toList();

            commands.forEach((cmd) -> {
                if (disabled.contains(cmd.getName())) {
                    cmd.delete().queue();
                }
            });
        });

        /* Runs the setup() method in each command,
           here is checked whether a command is enabled in a guild or not */
        cmdListEnabled.forEach((cmd) -> cmd.setup(cmd));
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {

        //Iterates through the enabled commands
        for (BotSlash cmd : cmdListEnabled) {

            //If the command exists,
            if (event.getName().equals(cmd.getName())) {

                //If the command was run in a guild,
                if (event.getGuild() != null) {

                    //If the command is enabled in the guild,
                    //(Should always be true, because this is already checked in the onReady method)
                    if (cmd.isEnabledInGuild(event.getGuild())) {

                        //And if the member has the permissions,
                        if (Objects.requireNonNull(event.getMember()).hasPermission(cmd.getPermissions())) {

                            if (event.getGuild().getSelfMember().hasPermission(cmd.getSelfPermissions())) {

                                //Then actually try to run it,
                                try {
                                    cmd.run(event);
                                }
                                //But it could still fail
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
                            }
                            else event.replyEmbeds(FormatUtil.noSelfPermissionEmbed(cmd.getSelfPermissions()).build()).setEphemeral(true).queue();
                        }
                        //No permission
                        else event.replyEmbeds(FormatUtil.noPermissionEmbed(cmd.getPermissions()).build()).setEphemeral(true).queue();
                    }
                    /* Not enabled in guild
                       This should never execute, because this is already checked in the onReady method */
                    else event.replyEmbeds(FormatUtil.notEnabledEmbed().build()).queue();

                    /* After the command executed, break the loop, because two commands cannot have the same name, it would lead to unnecessary confusion */
                    break;
                }
                //This should never execute, because no command is enabled globally
                else {
                    try {
                        cmd.run(event);
                    }
                    catch (Exception e) {
                        event.replyEmbeds(FormatUtil.errorEmbed(e, this.getClass()).build()).queue(null,
                                (f) -> event.getChannel().sendMessageEmbeds(FormatUtil.errorEmbed(e, this.getClass()).build()).queue());
                    }
                }
            }
        }
    }

    /**
     * Returns all the bots' slash commands, also those which are disabled in the config.json
     * @return {@link SlashEventListener#cmdList}
     * @since 2.1.0
     * */
    public static List<BotSlash> getCommands() {
        return cmdList;
    }

    /**
     * Returns only the slash commands enabled in the config.json
     * @return {@link SlashEventListener#cmdListEnabled}
     * @since 2.1.0
     * */
    public static List<BotSlash> getEnabledCommands() {
        return cmdListEnabled;
    }
}
