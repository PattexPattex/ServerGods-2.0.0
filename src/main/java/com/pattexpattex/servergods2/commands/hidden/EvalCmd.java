package com.pattexpattex.servergods2.commands.hidden;

import com.pattexpattex.servergods2.core.Bot;
import com.pattexpattex.servergods2.core.commands.BotHidden;
import com.pattexpattex.servergods2.util.BotEmoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;

public class EvalCmd extends BotHidden {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static String importString = "";
    private ScriptEngine engine;

    static {
        List<String> packageImports = List.of(
                "java.io",
                "java.lang",
                "java.math",
                "java.time",
                "java.util",
                "java.util.concurrent",
                "java.util.stream",
                "net.dv8tion.jda.api",
                "net.dv8tion.jda.api.entities",
                "net.dv8tion.jda.api.managers",
                "net.dv8tion.jda.api.utils",
                "net.dv8tion.jda.api.events",
                "com.pattexpattex.servergods2.core",
                "com.pattexpattex.servergods2.core.config",
                "com.pattexpattex.servergods2.core.commands",
                "com.pattexpattex.servergods2.core.giveaway",
                "com.pattexpattex.servergods2.core.listeners",
                "com.pattexpattex.servergods2.core.mute",
                "com.pattexpattex.servergods2.util",
                "com.pattexpattex.servergods2.commands.button.music",
                "com.pattexpattex.servergods2.commands.hidden",
                "com.pattexpattex.servergods2.commands.selection",
                "com.pattexpattex.servergods2.commands.selection.roles",
                "com.pattexpattex.servergods2.commands.slash",
                "com.pattexpattex.servergods2.commands.slash.config",
                "com.pattexpattex.servergods2.commands.slash.fun",
                "com.pattexpattex.servergods2.commands.slash.moderation"
        );

        List<String> classImports = List.of(
                //Empty
        );

        List<String> staticImports = List.of(
                "com.pattexpattex.servergods2.core.Bot.Developer"
        );

        packageImports.forEach((item) -> importString = importString + "import " + item + ".*;\n");
        classImports.forEach((item) -> importString = importString + "import " + item + ";\n");
        staticImports.forEach((item) -> importString = importString + "import static " + item + ";\n");
    }

    public EvalCmd() {
        if (!Bot.getConfig().enabledEval()) throw new UnsupportedOperationException("Eval is not enabled");
    }

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull String[] args) throws Exception {
        if (!Bot.getConfig().enabledEval()) throw new UnsupportedOperationException("Eval is not enabled");

        engine = new ScriptEngineManager().getEngineByName("java");

        String input;
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        if (!attachments.isEmpty()) {
            File file = attachments.get(0).downloadToFile().get(5, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            String line;

            try (reader) {
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }

            input = builder.toString();
        }
        else {
            input = event.getMessage().getContentRaw().replaceFirst(Bot.getPrefix() + "eval( )?(\n)?", "");

            if (input.startsWith("```") && input.endsWith("```")) {
                input = input.replace("```", "");
            }
        }

        String script = importString + input;

        if (script.contains("MessageReceivedEvent event")) {
            engine.put("event", event);
        }

        engine.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);

        eval(event, script);
    }

    private void eval(MessageReceivedEvent event, String script) {
        //Another failsafe
        if (!Bot.getConfig().enabledEval()) throw new UnsupportedOperationException("Eval is not enabled");

        long startTime = System.currentTimeMillis();

        Future<?> future = executor.submit(() -> {
            try {
                return engine.eval(script);
            }
            catch (Throwable t) {
                return t;
            }
        });

        Object out;
        try {
            out = future.get(5, TimeUnit.SECONDS);
        }
        catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
            out = e;
        }

        parseEvalResponse(event, out);

        long elapsedTime = System.currentTimeMillis() - startTime;

        String loggedScript = script.replace(importString, "");
        loggedScript = loggedScript.substring(0, Math.min(loggedScript.length(), 500));

        LoggerFactory.getLogger("EvalCmd").info("Took {}ms for evaluating last script (User: {})\nScript: {}", elapsedTime, event.getAuthor().getAsTag(), loggedScript + (loggedScript.length() > 499 ? "..." : ""));
    }

    private void parseEvalResponse(MessageReceivedEvent event, Object response) {
        if (response == null) {
            event.getChannel().sendMessage(BotEmoji.YES + " Success").queue();
        }
        else if (response instanceof Throwable t) {
            MessageAction action = event.getChannel().sendMessage(BotEmoji.NO + " Failed");
            attachStackTrace(action, t).queue();
        }
        else if (response instanceof RestAction<?> ra) {
            ra.queue((s) -> event.getChannel().sendMessage(BotEmoji.YES + " Rest action success: ```" + s + "```").queue(),
                    (f) -> {
                MessageAction action = event.getChannel().sendMessage(BotEmoji.NO + " Rest action fail");
                attachStackTrace(action, f).queue();
            });
        }
        else {
            event.getChannel().sendMessage(BotEmoji.YES + " Success: ```" + response + "```").queue();
        }
    }

    private MessageAction attachStackTrace(MessageAction action, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        t.printStackTrace(pw);
        action.append("\n```" + sw + "```");

        return action;
    }
}
