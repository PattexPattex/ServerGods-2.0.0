package com.pattexpattex.servergods2.commands.interactions.slash;

import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class PingCmd implements BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {
        event.replyEmbeds(FormatUtil.defaultEmbed(BotEmoji.SETTINGS + " My ping is: `"
                + event.getJDA().getGatewayPing() + "ms`").build()).queue();
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDesc() {
        return "Pongs the ping";
    }
}
