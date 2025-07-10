# GKTickets

![Version](https://img.shields.io/badge/version-1.0.3-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.8--1.21-green.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

A comprehensive ticket management system for Bukkit/Spigot Minecraft servers. GKTickets provides server administrators with powerful tools to handle player support requests efficiently.

## 📋 Overview

GKTickets allows players to create support tickets that staff members can respond to through an easy-to-use in-game interface. The plugin tracks statistics and enables players to provide feedback on closed tickets, helping server administrators improve their support quality.

## ✨ Features

- **Simple and Intuitive Interface** - Easy-to-use commands for both players and staff
- **Complete Ticket Management** - Create, view, respond to, and close tickets all in-game
- **Discord Integration** - Get notified in your Discord server when tickets are created, updated, or closed
- **SQLite Database Storage** - All tickets and replies are securely stored in a database
- **Feedback System** - Players can rate their support experience after ticket resolution
- **Comprehensive Statistics** - Track response times and staff performance
- **Blacklist System** - Prevent problematic players from creating tickets with temporary or permanent bans
- **Report Generation** - Automatic Discord reporting for blacklisting events and periodic ticket statistics
- **Auto-Close System** - Automatically close inactive tickets after a configurable time period
- **Staff Notes** - Private notes on tickets that only staff can see
- **Permissions-Based Access** - Fine-grained control over who can do what
- **PlaceholderAPI Support** - Use placeholders in messages
- **Version Compatible** - Works with Minecraft 1.8 to 1.20

## 📝 Implemented Features

All planned features have been implemented:

- [x] **Ticket Categories** - Allow users to categorize tickets (bug, question, suggestion)
- [x] **Staff Notes** - Private notes on tickets that only staff can see
- [x] **Auto-Close System** - Automatically close tickets that have been inactive for X days
- [x] **Blacklist System** - Prevent spam by limiting tickets from problematic users
- [x] **Reports** - Generate reports on ticket activity
- [x] **User Satisfaction Tracking** - Advanced metrics on user feedback

## 🔧 Commands

| Command | Description |
|---------|-------------|
| `/ticket create <description>` | Create a new ticket |
| `/ticket create <category> <description>` | Create a ticket in a specific category |
| `/ticket list` | List all open tickets (or your own) |
| `/ticket info <id>` | View detailed information about a ticket |
| `/ticket reply <id> <message>` | Reply to a ticket |
| `/ticket close <id>` | Close a ticket |
| `/ticket user <player>` | View tickets from a specific player |
| `/ticket categories` | List all available ticket categories |
| `/ticket feedback <id> [rating]` | Give feedback on a closed ticket |
| `/ticket stats` | View ticket system statistics |
| `/ticket note <id> <note>` | Add a private staff note to a ticket |
| `/ticket blacklist add <player> <reason> [days]` | Add player to blacklist |
| `/ticket blacklist remove <player>` | Remove player from blacklist |
| `/ticket blacklist list` | List all blacklisted players |
| `/ticket blacklist info <player>` | View blacklist details for a player |
| `/ticket reload` | Reload the plugin configuration |

**Aliases**: All commands can also be used with `/tk` instead of `/ticket`.

## 🔒 Permissions

| Permission | Description |
|------------|-------------|
| `gktickets.create` | Permission to create tickets |
| `gktickets.view` | Permission to view your own tickets |
| `gktickets.list` | Permission to list tickets |
| `gktickets.info` | Permission to view ticket details |
| `gktickets.reply` | Permission to reply to tickets |
| `gktickets.close.own` | Permission to close your own tickets |
| `gktickets.close.others` | Permission to close tickets from other players |
| `gktickets.feedback` | Permission to give feedback on closed tickets |
| `gktickets.stats` | Permission to view ticket statistics |
| `gktickets.blacklist` | Base permission for all blacklist commands |
| `gktickets.blacklist.add` | Permission to add players to blacklist |
| `gktickets.blacklist.remove` | Permission to remove players from blacklist |
| `gktickets.blacklist.list` | Permission to list all blacklisted players |
| `gktickets.blacklist.info` | Permission to view detailed blacklist info |
| `gktickets.staff` | All staff permissions (includes view, list, info, reply, close.others) |
| `gktickets.admin` | All administrative permissions |

## 💾 Installation

1. Download the latest version of GKTickets from [GitHub Releases](https://github.com/gecky2102/GKTickets/releases)
2. Place the downloaded JAR file in your server's `plugins` folder
3. Restart your server
4. Edit the configuration file in `plugins/GKTickets/config.yml` as needed
5. Use `/ticket` in-game to start using the plugin

## ⚙️ Configuration

The default configuration file includes:

```yaml
# Database configuration
database:
  type: sqlite  # Only SQLite is currently supported
  name: tickets  # Database filename

# Ticket settings
tickets:
  # Maximum number of open tickets per player (0 = unlimited)
  max-per-user: 3
  
  # Settings for automatic ticket closing
  auto-close:
    # Enable automatic closing of inactive tickets
    enabled: true
    # Hours after which inactive tickets are closed
    time: 72
    # Message shown when a ticket is auto-closed
    message: "Ticket chiuso automaticamente dopo {hours} ore di inattività"
    # Send notification to player when their ticket is auto-closed
    notify-player: true
    # Categories exempt from auto-closing
    exempt-categories:
      - importante
      - urgente
    # Hours before auto-close to send a warning
    warning-time: 48
    # Whether to send warnings before auto-closing
    send-warning: true

# Blacklist configuration
blacklist:
  # Enable blacklist system
  enabled: true
  # Default expiry time in days (0 = permanent)
  default-expiry: 7
  # Whether to notify players when they're blacklisted
  notify-player: true

# Discord integration for ticket notifications
discord:
  # Enable Discord notifications
  enabled: false
  # Discord webhook URL for ticket notifications
  webhook-url: ""
  # Username for the webhook bot
  username: "GKTickets"
  # URL for the webhook bot's avatar
  avatar-url: "https://i.imgur.com/your-avatar.png"
  # Events to log
  log-events:
    ticket-create: true
    ticket-close: true
    ticket-reply: true
  # Embed colors (hex color codes)
  colors:
    create: "#00FF00"  # Green
    reply: "#0099FF"   # Blue
    close: "#FF0000"   # Red

# Reporting configuration for statistics and blacklist events
reporting:
  # Enable reporting system
  enabled: false
  # Discord webhook URL for reports (separate from ticket notifications)
  webhook-url: ""
  # Username for the reporting bot
  username: "GKTickets Reporter"
  # URL for the reporting bot's avatar
  avatar-url: "https://i.imgur.com/your-logo.png"
  # Types of reports to send
  report-types:
    blacklist: true
    ticket-stats: true
  # Hours between automatic stats reports (0 to disable)
  stats-interval: 24
  # Colors for different report types (hex color codes)
  colors:
    blacklist: "#FF0000"  # Red
    stats: "#00FFFF"      # Cyan

# Notification settings
notifications:
  # Staff notifications
  staff:
    # Notify staff when a new ticket is created
    new-ticket: true
    # Notify staff when a reply is added to a ticket
    new-reply: true
  # Player notifications
  player:
    # Notify player when their ticket receives a reply
    new-reply: true
    # Notify player when their ticket is closed
    ticket-closed: true
  # Sound settings
  sound:
    # Enable notification sounds
    enabled: true
    # Sound for new tickets
    new-ticket: entity.experience_orb.pickup
    # Sound for new replies
    new-reply: entity.player.levelup

# Feedback system settings
feedback:
  # Enable feedback system
  enabled: true
  # Minutes between feedback reminders (0 to disable)
  reminder-interval: 30
```

## 🌟 Feature Highlights

### Blacklist System
Keep problematic players from creating tickets with the flexible blacklist system. Set temporary or permanent blacklists, specify reasons, and get Discord notifications when players are blacklisted.

### Reporting System
Two separate Discord webhook integrations:
1. **Ticket Notifications**: Get real-time notifications of ticket activities
2. **Reports**: Receive blacklist events and periodic statistics reports

### Auto-Close System
Automatically close inactive tickets after a configurable time period, with:
- Configurable warning time
- Exempt categories
- Custom messages
- Player notifications

### Staff Notes
Add private notes to tickets that only staff members can see, helping coordinate your support team without exposing internal communication to players.

## ⭐ Star history

<details>
  <summary>Show graph</summary>

  [![Star History Chart](https://api.star-history.com/svg?repos=Gecky2102/GKTickets&type=Date)](https://www.star-history.com/#Gecky2102/GKTickets&Date)

</details>

## 💡 Contributing

Contributions are welcome! Feel free to submit issues and pull requests.

## 📄 License

GKTickets is available under the MIT License. See the LICENSE file for more information.

## 👤 Author

Created with ❤️ by gecky2102.
