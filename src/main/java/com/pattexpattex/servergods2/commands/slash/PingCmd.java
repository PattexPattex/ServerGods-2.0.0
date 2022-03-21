package com.pattexpattex.servergods2.commands.slash;

import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.BotEmoji;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class PingCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {
        long a = System.currentTimeMillis();
        event.deferReply().queue();

        event.getHook().editOriginalEmbeds(FormatUtil.defaultEmbed("`loading...`").build())
                .queue((msg) -> msg.editMessageEmbeds(FormatUtil.defaultEmbed(BotEmoji.SETTINGS + " **My ping is:** `" + (System.currentTimeMillis() - a) + "ms` | Gateway ping: `" + event.getJDA().getGatewayPing() + "ms`").build()).queue());
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
