# ServerGods
[![CodeFactor](https://www.codefactor.io/repository/github/pattexpattex/servergods-2.0.0/badge)](https://www.codefactor.io/repository/github/pattexpattex/servergods-2.0.0)
[![Release](https://img.shields.io/github/v/release/PattexPattex/ServerGods-2.0.0?include_prereleases)](https://github.com/PattexPattex/ServerGods-2.0.0/releases)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/temurin/releases/)
[![License](https://img.shields.io/github/license/PattexPattex/ServerGods-2.0.0)](https://github.com/PattexPattex/ServerGods-2.0.0/blob/master/LICENSE)

A not so simple custom discord bot made in Java, designed for my Discord server, but it could be used in other servers with the release of 2.1.0.
It has mostly utility features, such as:
- banning, kicking & muting people,
- advanced music playback support (including from Spotify),
- creating & managing giveaways,
- creating polls,
- creating quick invites and more.

The bot is highly configurable, different commands can be enabled/disabled on different servers.

## Setup

The bot requires **Java 17**. I recommend using [Eclipse Termuin](https://adoptium.net/temurin/releases/).

To set up ServerGods for yourself, you must first create a Discord bot on the [Discord developer portal](https://discord.com/developers/applications).
Detailed instructions can be found on the [JDA wiki](https://github.com/DV8FromTheWorld/JDA/wiki/3%29-Getting-Started#creating-a-discord-bot).

Download the latest release from the [releases page](https://github.com/PattexPattex/ServerGods/releases/latest).
**Do not download the source code.**

After you've downloaded the `ServerGods-X.X.X.jar` file, copy it over to a directory in which the bot's files won't be deleted.

Next, start the bot, using the command line with a command `java -jar ServerGods-X.X.X.jar`. 
The bot should start, create a file `config.json` and quit.
Fill out the config as you want and then restart the bot. 
If you've done everything correctly, the bot should log something like this:

```text
19:05:04.428 [main] [net.dv8tion.jda.api.JDA] [INFO] - Login Successful!
19:05:04.721 [JDA MainWS-ReadThread] [n.d.j.i.requests.WebSocketClient] [INFO] - Connected to WebSocket
19:05:05.013 [JDA MainWS-ReadThread] [net.dv8tion.jda.api.JDA] [INFO] - Finished Loading!
```

If an `IOException` or `JSONException` is logged upon startup, it should be nothing to be alerted about, 
since it is normal to be logged if there is no server specific config. An example of the logging event:

```text
01:23:45.678 [main] [c.p.s.c.c.GuildConfigManager] [WARN] - Failed reading from server_config.json (normal if there are no values)
```

If the bot isn't present in any servers, it will automatically create and log an OAuth2 URL to invite it to a server.
After the bot joined a server, move its role to the top in the server's settings.

## Config

An example config file can be found [here](https://github.com/PattexPattex/ServerGods-2.0.0/blob/master/src/main/resources/config.json).

How to fill out the config:
- `token`: Your bot's token
- `activity_type`: The bot's activity, available options are `playing`, `streaming`, `watching`, `listening` and `competing`
- `activity_text`: The custom part of the bot's activity
- `status`: The bot's online status, available options are `online`, `idle`, `dnd` and `invisible`
- `color`: The color of embedded messages
- `bot_owner`: A snowflake ID of the bot's owner (you), e.g.: `125227483518861312`
- `alone_time_until_stop`: Time in seconds before the bot stops playing music when alone in a voice channel
- `prefix`: The prefix for owner-only commands
- `lyrics_provider`: The lyrics provider for lyrics lookup, available options are `A-Z Lyrics`, `MusixMatch`, `Genius` and `LyricsFreak`, 
but stick with A-Z Lyrics or MusixMatch since Genius and LyricsFreak are often inaccurate
- `debug_info_in_messages`: Set to `true` if you want stack traces attached to failure messages
- `spotify_app_id`: See 
- `spotify_app_secret`: See 
- `eval`: Set to `true` if you want to execute arbitrary code from the bot. 
**THIS IS VERY DANGEROUS AND SHOULD NOT BE ENABLED. 
IF SOMEONE TELLS YOU TO EVAL SOMETHING THERE IS AN 11/10 CHANCE THEY ARE TRYING TO SCAM YOU!**
- `commands`: Enable/disable commands globally

## Commands

All commands are slash commands/interactions, like `/about`.

List of slash commands:
- `config`: Manage server specific bot config
- `enable`: Enable/disable a command in a server
- `about`: Help and info
- `ban`: Ban a member
- `kick`: Kick a member
- `mute`: Mute a member
- `wake`: Move a member around voice channels, helpful if they are unresponsive in a group chat
- `roles`: Grant & manage cosmetic roles
- `ping`: The bot's ping
- `invite`: Create a temporary server invite or get a permanent one
- `giveaway`: Create & manage giveaways
- `poll`: Create polls
- `rickroll`: Rickroll other members in voice channels
- `music`: A command about music
- `emote`: Get unicode character codes for up to 10 symbols (letters, emoji, etc.)
- `user`: Get info about a user
- `avatar`: Get a user's avatar

There are also two commands enabled for the bot owner only, `stop` and `eval`.
- `stop`: Stop the bot
- `eval`: Run arbitrary java code

## Spotify Support

The bot has a built-in support to load & play tracks, playlists and albums directly from Spotify URLs, 
but it is disabled for default because you need a Spotify app ID & secret to log into the Spotify web API.

### Enabling Spotify Support

First, go to the [Spotify developers dashboard](https://developer.spotify.com/dashboard/applications) and log in if you aren't already. 
Then, create a new app. Fill out the name and description and click `create`. You should see something like this.

![](https://imgur.com/klNxTBM.png)

Click on `Show client secret` to reveal the application's secret.
Copy the `Client ID` and `Client Secret` to the config.
**KEEP THE CLIENT SECRET SAFE, DO NOT SHARE IT WITH ANYONE. IF YOU SUSPECT THAT IT HAS BEN SOMEHOW LEAKED, RESET THE SECRET ON THE DASHBOARD.**

If you've done everything correctly, the bot will notify you in the log that it has enabled Spotify as an audio source.

If you are still confused, you can go visit [Spotify's documentation](https://developer.spotify.com/documentation/general/guides/authorization/app-settings/).

## Contributing & Issues

If you have a question or found a bug, please open a new issue and mark it appropriately.

If you wish to make a contribution, open a pull request with a short description about your contribution.

Building... yea, you are on your own, I'm to lazy to write it down.