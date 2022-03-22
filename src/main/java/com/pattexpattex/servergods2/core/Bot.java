package com.pattexpattex.servergods2.core;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.pattexpattex.servergods2.core.config.Config;
import com.pattexpattex.servergods2.core.config.GuildConfig;
import com.pattexpattex.servergods2.core.config.GuildConfigManager;
import com.pattexpattex.servergods2.core.giveaway.GiveawayManager;
import com.pattexpattex.servergods2.core.listeners.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The main class of the bot.
 */
public class Bot {

    /* If you are editing the code, could you not change this string?
     * Give some credit to the developer's hard work, or change it,
     * I'm just a comment... */
    public static final String Developer = "<@!714406547161350155>";

    private static Config config;
    private static GuildConfigManager guildConfigManager;
    private static GiveawayManager giveawayManager;
    private static ScheduledExecutorService executors;
    private static EventWaiter waiter;
    private static JDA jda;

    private static Logger log;
    private static boolean debug;

    private static final Permission[] permissions = {
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_MENTION_EVERYONE,

            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.VOICE_MOVE_OTHERS,

            Permission.CREATE_INSTANT_INVITE,
            Permission.MANAGE_ROLES,
            Permission.MANAGE_CHANNEL,
            Permission.MANAGE_SERVER,

            Permission.BAN_MEMBERS,
            Permission.KICK_MEMBERS,
            Permission.MODERATE_MEMBERS
    };

    public static void main(String[] args) {

        //Logging
        log = LoggerFactory.getLogger(Bot.class);

        //Event waiting
        executors = Executors.newSingleThreadScheduledExecutor();
        waiter = new EventWaiter(executors, false);

        //Load the configs
        config = new Config();
        guildConfigManager = new GuildConfigManager();
        giveawayManager = new GiveawayManager();
        debug = getConfig().enabledDebugInfo();
        String prefix = getConfig().getConfigValue("prefix");



        //Try to initialize and start the bot
        try {
            JDABuilder builder = JDABuilder.createDefault(config.getConfigValue("token"))
                    .enableCache(CacheFlag.VOICE_STATE, CacheFlag.values())
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(EnumSet.allOf(GatewayIntent.class))
                    .setActivity(config.getActivity())
                    .setStatus(config.getStatus())
                    .addEventListeners(
                            new SlashEventListener(),
                            new ButtonEventListener(),
                            new SelectionEventListener(),

                            new HiddenEventListener(prefix),

                            new MiscEventListener(),
                            new Kvintakord.DiscordAudioEventListener(),
                            waiter
                    );
                    /* NativeAudioSendFactory in jda-nas may or may not cause fatal crashes in certain situations,
                     * probably because of a memory leak, see https://github.com/sedmelluq/jda-nas/issues/12 */
                    //.setAudioSendFactory(new NativeAudioSendFactory());

            jda = builder.build();
        }
        catch (Exception e) {
            log.error("Caught a flying " + e.getClass().getSimpleName() + " at high speeds", e);
        }
    }

    /* Don't let anyone instantiate this class */
    private Bot() {}

    public static void shutdown() {

        log.info("Shutting down bot");

        Kvintakord.shutdown();

        log.info("Shutting down ScheduledExecutorService");
        executors.shutdown();

        log.info("Shutting down EventWaiter");
        waiter.shutdown();

        log.info("Shutting down JDA");
        jda.shutdown();

        log.info("Goodbye!");
        System.exit(0);
    }

    public static JDA getJDA() {
        return jda;
    }

    public static GuildConfig getGuildConfig(Guild guild) {
        return guildConfigManager.getConfig(guild);
    }

    public static Config getConfig() {
        return config;
    }

    public static GiveawayManager getGiveawayManager() {
        return giveawayManager;
    }

    public static Permission[] getRecommendedPermissions() {
        return permissions;
    }

    public static ScheduledExecutorService getExecutor() {
        return executors;
    }

    public static EventWaiter getWaiter() {
        return waiter;
    }

    public static boolean isDebugInRepliesEnabled() {
        return debug;
    }
}
