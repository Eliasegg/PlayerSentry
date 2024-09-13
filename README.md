# 🚨 PlayerSentry

PlayerSentry is a comprehensive Minecraft plugin for server administrators to manage player punishments efficiently. It provides a robust system for banning, muting, kicking, and blacklisting players, with support for both temporary and permanent actions.

## 🌟 Features

- Multiple punishment types: ban, temporary ban, mute, temporary mute, kick, and IP blacklist.
- Each punishment type has an opposite command to remove the punishment (except for kick, which doesn't have an opposite).
- Persistent storage of player data and punishment logs using SQLite.
- Audit logs for all punishment actions, including unbans and removals.
- Configurable messages and settings.
- Asynchronous database operations for improved performance.
- Able to ban/mute players that are not online, as long as they have played on the server before: the plugin will try to look first if the player is online, and if not, it will look up their last known name in the database and find their associated UUID.

## 👨🏻‍⌨️ Commands

- `/sban <player> <reason>` - Permanently ban a player.
- `/sunban <player>` - Unban a player.
- `/stempban <player> <time> <reason>` - Temporarily ban a player.
- `/stempunban <player>` - Remove a temporary ban.
- `/sblacklist <player> <reason>` - Blacklist a player's IP address.
- `/sunblacklist <player>` - Remove a player from the IP blacklist.
- `/smute <player> <reason>` - Permanently mute a player.
- `/sunmute <player>` - Unmute a player.
- `/stempmute <player> <time> <reason>` - Temporarily mute a player.
- `/stempunmute <player>` - Remove a temporary mute.
- `/skick <player> <reason>` - Kick a player from the server.
- `/slogs <player> [page]` - View punishment logs for a player.

## Permissions

- `playersentry.ban` - Allow use of /sban command.
- `playersentry.unban` - Allow use of /sunban command.
- `playersentry.tempban` - Allow use of /stempban command.
- `playersentry.tempunban` - Allow use of /stempunban command.
- `playersentry.blacklist` - Allow use of /sblacklist command.
- `playersentry.unblacklist` - Allow use of /sunblacklist command.
- `playersentry.mute` - Allow use of /smute command.
- `playersentry.unmute` - Allow use of /sunmute command.
- `playersentry.tempmute` - Allow use of /stempmute command.
- `playersentry.tempunmute` - Allow use of /stempunmute command.
- `playersentry.kick` - Allow use of /skick command.
- `playersentry.logs` - Allow use of /slogs command.

*Server operators have access to all commands by default.*

## 🤓 Technical Details

### Ban Types
PlayerSentry uses a system of "ban types" to handle different punishment actions. Each ban type (e.g., Ban, TempBan, Mute, TempMute, Blacklist, Kick) extends the `BaseBanType` class and implements its own `handleBan` and `handleUnban` methods.

These ban types are registered in the main `PlayerSentry` class.

### SQLITE
PlayerSentry uses SQLite to store player data and punishment logs. The database is created when the plugin is first enabled, and is located in the `plugins/PlayerSentry/db` folder.

The database contains the following tables:

- `offline_players`: Stores information about offline players, including their name, IP address, and last known name. It is updated asynchronously when a player leaves the server.
- `muted_players`: Stores information about muted players, including their name, reason, and expiration date.
- `audit_logs`: Stores information about punishment actions, including the player's name, reason, duration, and punishment type.

### Punishment Logs
Punishment logs are displayed when a player is banned, muted, or kicked. They include the player's name, reason, duration, and punishment type.