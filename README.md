# GKTickets

![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.8--1.20-green.svg)
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
- **Permissions-Based Access** - Fine-grained control over who can do what
- **PlaceholderAPI Support** - Use placeholders in messages
- **Version Compatible** - Works with Minecraft 1.8 to 1.20

## 📝 TODO List

Future features planned for implementation:

- [ ] **Ticket Categories** - Allow users to categorize tickets (bug, question, suggestion)
- [ ] **Staff Notes** - Private notes on tickets that only staff can see
- [ ] **Auto-Close System** - Automatically close tickets that have been inactive for X days
- [ ] **Blacklist System** - Prevent spam by limiting tickets from problematic users
- [ ] **Ticket Archiving** - Move closed tickets to archive rather than just marking them closed
- [ ] **Monthly Reports** - Generate periodic reports on ticket activity
- [ ] **Staff Efficiency Metrics** - Track average resolution time per staff member
- [ ] **User Satisfaction Tracking** - Advanced metrics on user feedback
- [ ] **Staff Dashboard** - Command to see a performance overview of all staff members

## 🔧 Commands

| Command | Description |
|---------|-------------|
| `/ticket create <description>` | Create a new ticket |
| `/ticket list` | List all open tickets (or your own) |
| `/ticket info <id>` | View detailed information about a ticket |
| `/ticket reply <id> <message>` | Reply to a ticket |
| `/ticket close <id>` | Close a ticket |
| `/ticket user <player>` | View tickets from a specific player |
| `/ticket feedback <id> [rating]` | Give feedback on a closed ticket |
| `/ticket stats` | View ticket system statistics |

**Aliases**: All commands can also be used with `/tk` instead of `/ticket`.

## 🔒 Permissions

| Permission | Description |
|------------|-------------|
| `gktickets.create` | Permission to create tickets |
| `gktickets.view` | Permission to view your own tickets |
| `gktickets.list` | Permission to list tickets |
| `gktickets.reply` | Permission to reply to tickets |
| `gktickets.close.own` | Permission to close your own tickets |
| `gktickets.close.others` | Permission to close tickets from other players |
| `gktickets.feedback` | Permission to give feedback on closed tickets |
| `gktickets.stats` | Permission to view ticket statistics |
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

# Ticket settings
tickets:
  max-per-user: 3  # Maximum number of open tickets per player
  auto-close:
    enabled: false
    time: 72       # Hours after which inactive tickets are closed

# Discord integration
discord:
  enabled: false
  webhook-url: ""   # Your Discord webhook URL
  
# Feedback system
feedback:
  enabled: true
  reminder-interval: 30  # Minutes between reminders
```

## 💡 Contributing

Contributions are welcome! Feel free to submit issues and pull requests.

## 📄 License

GKTickets is available under the MIT License. See the LICENSE file for more information.

## 👤 Author

Created with ❤️ by gecky2102.