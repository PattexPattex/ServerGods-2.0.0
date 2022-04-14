package com.pattexpattex.servergods2.commands.slash.fun;

import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.commands.BotSlash;
import com.pattexpattex.servergods2.util.Emotes;
import com.pattexpattex.servergods2.util.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class EmoteCmd extends BotSlash {

    @Override
    public void run(@NotNull SlashCommandEvent event) {
        event.deferReply().queue();

        Message message = event.getHook().editOriginal(Objects.requireNonNull(event.getOption("emote")).getAsString()).complete();
        List<Emote> emotes = message.getEmotes();

        if (!emotes.isEmpty()) {
            customEmoteMentioned(message, emotes.get(0));
            return;
        }

        String content = message.getContentRaw();

        if (content.codePoints().count() > 10) {
            throw new BotException("Invalid emote or input is too long");
        }

        normalEmoteMentioned(message, content);
    }

    private void customEmoteMentioned(Message message, Emote emote) {
        String name = emote.getName();
        String id = emote.getId();
        String url = emote.getImageUrl();
        boolean animated = emote.isAnimated();
        String markdown = "`< " + (animated ? "a" : "") +":" + name + ":" + id + ">`";

        message.editMessage(Emotes.YES).complete().editMessageEmbeds(FormatUtil.defaultEmbed(
                "**Emote: **" + name +
                        "\n**Id: **" + id +
                        "\n**Animated: **" + animated +
                        "\n**Markdown: **" + markdown +
                        "\n**Url: **" + url, "Emote info for custom emote", url, null).build()).queue();
    }

    private void normalEmoteMentioned(Message message, String emote) {
        StringBuilder joinedHex = new StringBuilder();
        EmbedBuilder builder = FormatUtil.defaultEmbed(null, "Emoji/Char info for: " + emote);

        emote.codePoints().forEach((it) -> {
            char[] chars = Character.toChars(it);
            String hex = ensureFourHex(toHex(it));

            builder.appendDescription("`\\u" + hex + "` ");

            if (chars.length > 1) {
                StringBuilder extraHex = new StringBuilder();
                for (char c : chars) {
                    extraHex.append("\\u").append(ensureFourHex(toHex(c)));
                }

                builder.appendDescription("[`" + extraHex + "`]");
                joinedHex.append(extraHex);
            }
            else {
                joinedHex.append("\\u").append(hex);
            }

            builder.appendDescription(" _" + getName(it) + "_\n");
        });

        if (emote.codePointCount(0, emote.length()) > 1) {
            builder.appendDescription("\n**Copy-paste string: `" + joinedHex + "`**");
        }

        message.editMessage(Emotes.YES).complete().editMessageEmbeds(builder.build()).queue();
    }

    private String toHex(int i) {
        return Integer.toHexString(i).toUpperCase();
    }
    private String getName(int i) {
        return Character.getName(i);
    }
    private String ensureFourHex(String s) {
        return ("0000"+s).substring(s.length());
    }

    @Override
    public String getName() {
        return "emote";
    }

    @Override
    public String getDesc() {
        return "Shows information about an emoji or emote";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "emote", "The emote", true)
        };
    }
}
