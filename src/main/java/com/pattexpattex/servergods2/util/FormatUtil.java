package com.pattexpattex.servergods2.util;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.exceptions.BotException;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.core.kvintakord.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class FormatUtil {

    private static final String avatarUrl = Bot.getJDA().getSelfUser().getEffectiveAvatarUrl();
    private static final String kvintakordAvatar = "https://raw.githubusercontent.com/PattexPattex/ServerGods/master/musicGods.png";

    public static final Color COLOR = new Color((int) Long.parseLong(Bot.getConfig().getValue("color"), 16));
    public static final Color ERR_COLOR = new Color(0xFF2626);
    public static final Color KVINTAKORD_COLOR = new Color(0xDFE393);

    private FormatUtil() {}


    /* ---- Generic Embeds ---- */

    public static @NotNull EmbedBuilder defaultEmbed(@Nullable String message,
                                                     @Nullable String title,
                                                     @Nullable String thumbnail,
                                                     @Nullable String image)
    {
        EmbedBuilder builder = new EmbedBuilder();

        //Color color = new Color(0, 90, 112); //005A70 hex value

        builder.setColor(COLOR)
                .setFooter("Powered by Server Gods 2.1.0.", avatarUrl)
                .setTimestamp(OffsetDateTime.now());

        builder.setDescription(message);

        builder.setTitle(title);

        builder.setImage(image);

        if (thumbnail != null) {
            if (thumbnail.equals("DEFAULT")) builder.setThumbnail(avatarUrl);
            else builder.setThumbnail(thumbnail);
        }

        return builder;
    }

    public static @NotNull EmbedBuilder defaultEmbed(@Nullable String message,
                                                     @Nullable String title)
    {
        return defaultEmbed(message, title, null, null);
    }

    public static @NotNull EmbedBuilder defaultEmbed(@NotNull String message) {
        return defaultEmbed(message, null, null, null); //Nothing to see here, just a comment
    }


    /* ---- Kvintakord Embeds ---- */

    public static @NotNull EmbedBuilder kvintakordEmbed(@Nullable String message,
                                                        @Nullable String title,
                                                        @Nullable String thumbnail,
                                                        @Nullable String image)
    {
        EmbedBuilder builder = defaultEmbed(message, title, thumbnail, image);

        builder.setFooter("Powered by Kvintakord", kvintakordAvatar)
                .setColor(KVINTAKORD_COLOR);

        return builder;
    }

    public static @NotNull EmbedBuilder kvintakordEmbed(@Nullable String message,
                                                        @Nullable String title)
    {
        return kvintakordEmbed(message, title, null, null);
    }

    public static @NotNull EmbedBuilder kvintakordEmbed(@Nullable String message) {
        return kvintakordEmbed(message, null, null, null);
    }


    /* ---- Failure Embeds ---- */

    public static @NotNull EmbedBuilder noPermissionEmbed(Permission @NotNull ... permissions) {
        StringBuilder sb = new StringBuilder();

        for (Permission permission : permissions) {
            sb.append(permission.getName()).append(", ");
        }

        sb.delete(sb.lastIndexOf(","), sb.length());

        return defaultEmbed(BotEmoji.NO + " You do not have the permission(s) to run this command!\n\n" +
                BotEmoji.CODE + "*You need the permission(s): `" + sb + "`*", "Oops!").setColor(ERR_COLOR);
    }

    public static @NotNull EmbedBuilder noSelfPermissionEmbed(Permission @NotNull ... permissions) {
        StringBuilder sb = new StringBuilder();

        for (Permission permission : permissions) {
            sb.append(permission.getName()).append(", ");
        }

        sb.delete(sb.lastIndexOf(","), sb.length());

        return defaultEmbed(BotEmoji.NO + " I do not have the permission(s) to run this command!\n\n" +
                BotEmoji.CODE + "*I need the permission(s): `" + sb + "`*", "Oops!").setColor(ERR_COLOR);
    }

    public static @NotNull EmbedBuilder notEnabledEmbed() {
        return defaultEmbed(BotEmoji.NO + " This command is not enabled in this server!\n\n" +
                BotEmoji.CODE + "*Try /enable <command> to enable this command.*", "Oops!").setColor(ERR_COLOR);
    }

    public static @NotNull EmbedBuilder errorEmbed(@Nullable Throwable t, Class<?> clazz) {
        EmbedBuilder builder;
        Logger log = LoggerFactory.getLogger(clazz);

        builder = defaultEmbed(BotEmoji.NO + " ", "Oops!").setColor(new Color(0xFF4040));

        if (t == null) {
            t = new Exception("null");
        }

        builder.appendDescription("`" + t.getClass().getSimpleName() + ": " + t.getMessage() + "`");

        if (t instanceof BotException) {
            log.warn("Caught a flying BotException at high speeds", t);
        }
        else {
            log.error("Something broke", t);
        }


        return builder;
    }


    /* ---- Delete Message ---- */

    public static void deleteAfter(@NotNull Message message, int seconds) {
        message.delete().queueAfter(seconds, TimeUnit.SECONDS);
    }


    /* ---- Mention a User ---- */

    public static void mentionUserAndDelete(@NotNull User user, @NotNull TextChannel channel) {
        Message msg = channel.sendMessage(BotEmoji.MENTION + " " + user.getAsMention()).complete();

        deleteAfter(msg, 5);
    }


    /* ---- Selections ---- */

    public static @NotNull SelectOption getEmptyOption() {
        return SelectOption.of("Wow such empty", "empty")
                .withEmoji(Emoji.fromEmote("such_empty", 934439539378896957L, false));
    }


    /* ---- Time Formatting, Encoding and Decoding ---- */

    /**
     * Formatted: 01:23:45
     */
    public static @NotNull String formatTime(long duration) {

        if (duration == Long.MAX_VALUE) {
            return "LIVE";
        }

        long seconds = Math.round(duration / 1000.0);
        long hours = seconds / (60 * 60);

        seconds %= 60 * 60;

        long minutes = seconds / 60;

        seconds %= 60;

        return (hours > 0 ? hours + ":" : "") + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    /**
     * Formatted: 1 days, 2 hours, 3 minutes, 4 seconds
     */
    public static @NotNull String formatTimeAlternate(long seconds) {

        StringBuilder builder = new StringBuilder();

        int years = (int) (seconds / (60 * 60 * 24 * 365));

        if (years > 0) {
            builder.append(years).append(" years, ");
            seconds = seconds % (60 * 60 * 24 * 365);
        }
        int weeks = (int) (seconds / (60 * 60 * 24 * 7));

        if (weeks > 0) {
            builder.append(weeks).append(" weeks, ");
            seconds = seconds % (60 * 60 * 24 * 7);
        }
        int days = (int) (seconds / (60 * 60 * 24));

        if (days > 0) {
            builder.append(days).append(" days, ");
            seconds = seconds % (60 * 60 * 24);
        }
        int hours = (int) (seconds / (60 * 60));

        if (hours > 0) {
            builder.append(hours).append(" hours, ");
            seconds = seconds % (60 * 60);
        }
        int minutes = (int) (seconds / (60));

        if (minutes > 0) {
            builder.append(minutes).append(" minutes, ");
            seconds = seconds % (60);
        }

        if (seconds > 0)
            builder.append(seconds).append(" seconds");

        String str = builder.toString();

        if (str.endsWith(", "))
            str = str.substring(0, str.length() - 2);

        if (str.equals(""))
            str = "No seconds";

        return str;
    }

    /**
     * Formatted to a Discord epoch timestamp ({@code < t:1647291600:f>}), that is formatted like so: {@code March 14, 2022 at 10:00 PM}.
     */
    public static @NotNull String epochTimestamp(long epoch) {
        return "<t:" + epoch + ":f>";
    }

    /**
     * Formatted to a Discord epoch timestamp ({@code < t:1647291600:R>}), that is relative to the time ({@code in 5 hours}).
     */
    public static @NotNull String epochTimestampRelative(long epoch) {
        return "<t:" + epoch + ":R>";
    }

    /**
     * @param time Formatted input: {@code HH:MM:SS / MM:SS / H:MM:SS / M:SS}
     * @throws NumberFormatException if the input is formatted illegally
     */
    @Contract(value = "null -> fail", pure = true)
    public static long decodeTime(String time) {
        Checks.notNull(time, "Input");

        Pattern one = Pattern.compile("([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?");

        if (!one.matcher(time).matches()) {
            throw new NumberFormatException();
        }

        long hours;
        long minutes;
        long seconds;

        time = time.replaceAll(":", "");

        if (time.length() == 3) {
            hours = 0;
            minutes = Long.parseLong(time, 0, 1, 10);
            seconds = Long.parseLong(time, 1, 3, 10);
        }
        else if (time.length() == 4) {
            hours = 0;
            minutes = Long.parseLong(time, 0, 2, 10);
            seconds = Long.parseLong(time, 2, 4, 10);
        }
        else if (time.length() == 5) {
            hours = Long.parseLong(time, 0, 1, 10);
            minutes = Long.parseLong(time, 1, 3, 10);
            seconds = Long.parseLong(time, 3, 5, 10);
        }
        else {
            hours = Long.parseLong(time, 0, 2, 10);
            minutes = Long.parseLong(time, 2, 4, 10);
            seconds = Long.parseLong(time, 4, 6, 10);
        }

        return seconds + (minutes * 60) + (hours * 60 * 60);
    }

    /**
     * Possible input formats are:
     * {@code d h m s}, {@code dd hh mm ss} with or without whitespaces
     * and any combination of them, e.g.: {@code 5m} or {@code 1d 2h 3m 4s}.
     * It is illegal to pass an input with the time units' order different from {@code d, h, m, s}.
     *
     * @param time Formatted input
     * @return -1 if {@code time} is {@code null}
     * @throws NumberFormatException if the input is formatted illegally
     */
    @Contract(pure = true)
    public static long decodeTimeAlternate(String time) {
        if (time == null) return -1;

        Pattern one = Pattern.compile("(\\d{1,2}[dhms]\\s?)+");

        if (!one.matcher(time).matches()) {
            throw new NumberFormatException();
        }

        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        while (!time.isEmpty()) {
            if (time.contains("d")) {
                days = Long.parseLong(time, 0, time.indexOf("d"), 10);
                time = time.substring(time.indexOf("d") + 1).strip();
            }
            else if (time.contains("h")) {
                hours = Long.parseLong(time, 0, time.indexOf("h"), 10);
                time = time.substring(time.indexOf("h") + 1).strip();
            }
            else if (time.contains("m")) {
                minutes = Long.parseLong(time, 0, time.indexOf("m"), 10);
                time = time.substring(time.indexOf("m") + 1).strip();
            }
            else if (time.contains("s")) {
                seconds = Long.parseLong(time, 0, time.indexOf("s"), 10);
                time = time.substring(time.indexOf("s") + 1).strip();
            }
        }

        return seconds + (minutes * 60) + (hours * 60 * 60) + (days * 60 * 60 * 24);
    }


    /* ---- Kvintakord ---- */

    public static @NotNull String formatTrackLink(@NotNull AudioTrack track) {
        return "[" + TrackMetadata.getTrackName(track) + "](" + TrackMetadata.getTrackUri(track) + ")";
    }

    public static String getLyricsProviderUrl() {
        String provider = Bot.getConfig().getLyricsProvider();
        String url;

        switch (provider) {
            default -> url = "https://www.azlyrics.com";
            case "MusixMatch" -> url = "https://www.musixmatch.com/";
            case "Genius" -> url = "https://genius.com/";
            case "LyricsFreak" -> url = "https://www.lyricsfreak.com/";
        }

        return url;
    }

    @Contract("null -> fail")
    public static @NotNull MessageEmbed getQueueEmbed(Guild guild) {
        List<AudioTrack> queue = Bot.getKvintakord().getQueue(guild);
        AudioTrack currentTrack = Objects.requireNonNull(Bot.getKvintakord().getCurrentTrack(guild));

        StringBuilder sb = new StringBuilder();

        if (currentTrack.isSeekable()) {
            sb.append("**`").append(formatTime(currentTrack.getPosition())).append(" / ").append(formatTime(currentTrack.getDuration())).append("`**");
        }
        else {
            sb.append("\uD83D\uDD34").append(" **`").append(formatTime(currentTrack.getDuration())).append("`**");
        }

        if (Bot.getKvintakord().isPaused(guild)) {
            sb.append(" **| \u23F8**");
        }

        if (Bot.getGuildConfig(guild).getLoop() == Kvintakord.LoopMode.SINGLE) {
            sb.append(" **| \uD83D\uDD02**");
        }
        else if (Bot.getGuildConfig(guild).getLoop() == Kvintakord.LoopMode.ALL) {
            sb.append(" **| \uD83D\uDD01**");
        }

        sb.append(" **| \uD83D\uDD0A `").append(Bot.getKvintakord().getVolume(guild)).append("`**");

        sb.append("\n\n");


        for (int i = 0; queue.size() > i; i++) {
            AudioTrack track = queue.get(i);

            sb.append("**").append(i + 1).append(".** `").append(FormatUtil.formatTime(track.getDuration())).append("` ").append(FormatUtil.formatTrackLink(track)).append("\n");
        }

        return FormatUtil.kvintakordEmbed(sb.toString())
                .setTitle(TrackMetadata.getTrackName(currentTrack), (!currentTrack.getIdentifier().startsWith("C:\\") ? TrackMetadata.getTrackUri(currentTrack) : null))
                .setAuthor(TrackMetadata.getTrackAuthor(currentTrack), TrackMetadata.getTrackAuthorUrl(currentTrack))
                .setThumbnail(TrackMetadata.getTrackImage(currentTrack))
                .build();
    }


    /* ---- Giveaways ---- */

    private static final String REACT = " React with \uD83C\uDF89 to enter";

    public static EmbedBuilder runningGiveawayEmbed(int winners, long end, String reward, Member host) {
        return defaultEmbed(
                REACT + "\n" +
                        (winners == 1 ? BotEmoji.PERSON + " 1 winner" : BotEmoji.PERSON + " " + winners + " winners") + "\n" +
                        BotEmoji.LOADING + " Ends " + FormatUtil.epochTimestampRelative(end) + "\n" +
                        BotEmoji.MENTION + " Host: " + host.getAsMention(),

                reward);
    }

    public static EmbedBuilder endedGiveawayEmbed(String winners, Member host, String reward) {
        return defaultEmbed(
                "\uD83C\uDF7E Winners: " + winners + "\n" +
                        BotEmoji.MENTION + " DM " + host.getAsMention() + " to claim the reward!",
                reward);
    }

    public static EmbedBuilder noWinnersGiveawayEmbed(String reward) {
        return defaultEmbed("\u2753 Winners: no one\n*`Waiting for re-roll...`*", reward);
    }

    public static EmbedBuilder rerollGiveawayEmbed(String winners, Member host, String reward) {
        return defaultEmbed(
                "\uD83C\uDF7E Re-roll winners: " + winners + "\n" +
                        BotEmoji.MENTION + " DM " + host.getAsMention() + " to claim the reward!",
                reward);
    }

    public static ActionRow jumpButton(Message message) {
        return ActionRow.of(Button.link(message.getJumpUrl(), "Original"));
    }


    /* ---- Arrays ---- */

    public static String formatArray(Object[] array) {
        return Arrays.toString(array).replaceAll("\\[", "").replaceAll("]", "");
    }

    public static String formatArray(Collection<?> collection) {
        return formatArray(collection.toArray());
    }
}
