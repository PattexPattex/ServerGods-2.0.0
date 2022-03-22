package com.pattexpattex.servergods2.util;

import com.pattexpattex.servergods2.core.Bot;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OtherUtil {

    private static final Logger log = LoggerFactory.getLogger(OtherUtil.class);

    private OtherUtil() {}

    public static Path getPath(String path) {

        Path result = Paths.get(path);

        if (result.toAbsolutePath().toString().toLowerCase().startsWith("c:\\windows\\system32\\")) {
            try {
                result = Paths.get(new File(Bot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                                           .getParentFile().getPath() + File.separator + path);
            }
            catch (URISyntaxException ignored) {}
        }

        return result;
    }

    public static @Nullable String loadResource(Object clazz, String name) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(clazz.getClass().getResourceAsStream(name))))) {

            StringBuilder sb = new StringBuilder();

            reader.lines().forEach(line -> sb.append("\r\n").append(line));

            return sb.toString().trim();
        }
        catch(IOException e) {
            log.error("Failed loading resource \"" + name + "\"", e);
            Bot.shutdown();
            return null;
        }
    }

    public static String getOauthInvite() {
        String oauthLink;

        try {
            ApplicationInfo info = Bot.getJDA().retrieveApplicationInfo().complete();
            info.setRequiredScopes("bot", "applications.commands");
            oauthLink = info.isBotPublic() ? info.getInviteUrl(Bot.getRecommendedPermissions()) : "https://github.com/PattexPattex/ServerGods";

        }
        catch (Exception e) {
            LoggerFactory.getLogger(OtherUtil.class).error("Could not generate OAuth2 invite link", e);
            oauthLink = "https://github.com/PattexPattex/ServerGods";
        }

        return oauthLink;
    }

    public static Button getInviteButton() {
        String link = getOauthInvite();

        return Button.link(getOauthInvite(), "Invite me to your server").withEmoji(Emoji.fromEmote("discord", 934441038192476260L, false)).withDisabled(link.equals("https://github.com/PattexPattex/ServerGods"));
    }

    @Contract(value = "null -> null", pure = true)
    public static @Nullable File createStackTraceFile(Throwable t) {
        if (t == null) {
            return null;
        }

        try {
            File file = File.createTempFile("stacktrace", ".txt");
            file.deleteOnExit();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(t + "\n");

            for (StackTraceElement element : t.getStackTrace()) {
                String module = element.getModuleName();

                writer.write("\t at " + (module != null ? module + "/" : "") + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")\n");
            }

            writer.close();

            return file;
        }
        catch (IOException e) {
            log.error("Something broke", e);
        }

        return null;
    }

    public static void handleBotException(GenericInteractionCreateEvent event, Exception e) {

        if (Bot.isDebugInRepliesEnabled()) {
            File file = createStackTraceFile(e);
            if (file != null) {
                event.getMessageChannel().sendMessage(new MessageBuilder().append("`Stacktrace of message ").append(event.getId()).append("`").build()).addFile(file, "stacktrace.txt").queue();
            }
        }
    }

    public static @Nullable Member findMemberById(long id) {

        for (Guild guild : Bot.getJDA().getGuildCache()) {
            try {
                return guild.retrieveMemberById(id).complete();
            }
            catch (RuntimeException ignored) {}
        }

        return null;
    }

    public static @Nullable User findUserById(long id) throws ExecutionException, InterruptedException {
        CompletableFuture<User> future = new CompletableFuture<>();

        Bot.getJDA().retrieveUserById(id).queue(future::complete, (f) -> {
            if (f instanceof ErrorResponseException &&
                    ((ErrorResponseException) f).getErrorResponse() != ErrorResponse.UNKNOWN_USER) {
                log.warn("Failed retrieving user", f);
            }
        });

        return future.get();
    }

    public static @Nullable Message findMessageById(long id) throws ExecutionException, InterruptedException {
        CompletableFuture<Message> future = new CompletableFuture<>();

        for (Guild guild : Bot.getJDA().getGuilds()) {
            for (TextChannel channel : guild.getTextChannels()) {
                channel.retrieveMessageById(id).queue(future::complete, (f) -> {
                    if (f instanceof ErrorResponseException &&
                    ((ErrorResponseException) f).getErrorResponse() != ErrorResponse.UNKNOWN_MESSAGE) {
                            log.warn("Failed retrieving message", f);
                    }});
            }
        }

        return future.get();
    }
}
