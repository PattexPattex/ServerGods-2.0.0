package com.pattexpattex.servergods2.core;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.pattexpattex.servergods2.core.config.Config;
import com.pattexpattex.servergods2.core.config.GuildConfig;
import com.pattexpattex.servergods2.core.config.GuildConfigManager;
import com.pattexpattex.servergods2.core.giveaway.GiveawayManager;
import com.pattexpattex.servergods2.core.kvintakord.Kvintakord;
import com.pattexpattex.servergods2.core.listeners.*;
import com.pattexpattex.servergods2.core.mute.MuteManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The main class of the bot.
 */
public class Bot {

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    /* Don't let anyone instantiate this class */
    private Bot() {}

    /* If you are editing the code, could you not change this string?
     * Give some credit to the developer's hard work, or change it,
     * I'm just a comment... */
    public static final String Developer = "<@!714406547161350155>";

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

    private static boolean debug;
    private static JDA jda;
    private static Config config;
    private static EventWaiter waiter;
    private static Kvintakord kvintakord;
    private static MuteManager muteManager;
    private static GiveawayManager giveawayManager;
    private static ScheduledExecutorService executors;
    private static GuildConfigManager guildConfigManager;


    public static void main(String[] args) {

        executors = Executors.newSingleThreadScheduledExecutor();

        config = new Config();
        debug = config.enabledDebugInfo();
        guildConfigManager = new GuildConfigManager();
        giveawayManager = new GiveawayManager();
        muteManager = new MuteManager();
        kvintakord = new Kvintakord();
        waiter = new EventWaiter(executors, false);

        try {
            JDABuilder builder = JDABuilder.createDefault(config.getValue("token"))
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(config.getActivity())
                    .setStatus(config.getStatus())
                    .addEventListeners(new SlashEventListener(),
                            new ButtonEventListener(),
                            new SelectionEventListener(),
                            new HiddenEventListener(config.getValue("prefix")),
                            new MiscEventListener(),
                            waiter);
                    /* NativeAudioSendFactory in JDA-NAS may or may not cause fatal crashes,
                     * probably because of a memory leak, see https://github.com/sedmelluq/jda-nas/issues/12 */
                    //builder.setAudioSendFactory(new NativeAudioSendFactory());

            jda = builder.build();
        }
        catch (Exception e) {
            log.error("Caught a flying " + e.getClass().getSimpleName() + " at high speeds", e);
        }
    }

    public static void shutdown() {

        log.info("Shutting down bot");

        log.info("Shutting down Kvintakord");
        kvintakord.shutdown();

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

    public static MuteManager getMuteManager() {
        return muteManager;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return executors;
    }

    public static EventWaiter getEventWaiter() {
        return waiter;
    }

    public static Kvintakord getKvintakord() {
        return kvintakord;
    }

    public static String getPrefix() {
        return Bot.getConfig().getValue("prefix");
    }

    public static Permission[] getRecommendedPermissions() {
        return permissions;
    }

    public static boolean isDebugInRepliesEnabled() {
        return debug;
    }
}
