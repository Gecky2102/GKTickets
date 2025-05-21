package it.Gecky.gktickets.commands;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.database.DatabaseManager;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
import it.Gecky.gktickets.notifications.NotificationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

public class TicketCommand implements CommandExecutor, TabCompleter {

    private final GKTickets plugin;
    private final DatabaseManager databaseManager;
    private final NotificationManager notificationManager;

    public TicketCommand(GKTickets plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.notificationManager = plugin.getNotificationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cQuesto comando può essere eseguito solo da un giocatore.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "info":
                handleInfoCommand(player, args);
                break;
            case "close":
                handleCloseCommand(player, args);
                break;
            case "reply":
                handleReplyCommand(player, args);
                break;
            case "user":
                handleUserCommand(player, args);
                break;
            case "testdiscord":
                handleTestDiscordCommand(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.create")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-create-usage"));
            return;
        }
        
        // Verifica il limite di ticket aperti per utente
        int maxTickets = plugin.getConfigManager().getMaxTicketsPerUser();
        if (maxTickets > 0) {
            int currentOpenTickets = databaseManager.countOpenTicketsForUser(player.getUniqueId());
            if (currentOpenTickets >= maxTickets) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("max", String.valueOf(maxTickets));
                player.sendMessage(plugin.getMessageManager().formatMessage("ticket-create-limit", placeholders));
                return;
            }
        }
        
        // Combina tutti gli argomenti dopo "create" in una descrizione
        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        int ticketId = databaseManager.createTicket(player, description);
        if (ticketId != -1) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticketId));
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-create-success", placeholders));
            
            // Notifica allo staff
            Ticket newTicket = databaseManager.getTicketById(ticketId);
            notificationManager.notifyStaffNewTicket(newTicket);
            
            // Log su Discord
            plugin.getDiscordIntegration().sendTicketCreatedMessage(newTicket, player);
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-create-error"));
        }
    }
    
    private void handleListCommand(Player player) {
        if (!player.hasPermission("gktickets.list")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        List<Ticket> tickets;
        String headerMessage;
        
        if (player.hasPermission("gktickets.staff")) {
            // Lo staff può vedere tutti i ticket aperti
            tickets = databaseManager.getOpenTickets();
            headerMessage = plugin.getMessageManager().getMessage("ticket-list-title");
        } else if (player.hasPermission("gktickets.view")) {
            // I giocatori normali possono vedere solo i propri ticket
            tickets = databaseManager.getPlayerTickets(player.getUniqueId());
            headerMessage = plugin.getMessageManager().getMessage("ticket-list-personal-title");
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        // Invia l'header con il divisore
        player.sendMessage(plugin.getMessageManager().getMessage("divider"));
        player.sendMessage(headerMessage);
        
        if (tickets.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-list-empty"));
            player.sendMessage(plugin.getMessageManager().getMessage("divider"));
            return;
        }
        
        for (Ticket ticket : tickets) {
            String statusColor = ticket.isOpen() ? "§a" : "§c";
            String status = ticket.getStatus();
            String shortDescription = ticket.getDescription().length() > 25 ? 
                                     ticket.getDescription().substring(0, 25) + "..." : 
                                     ticket.getDescription();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticket.getId()));
            placeholders.put("status_color", statusColor);
            placeholders.put("status", status);
            placeholders.put("player", ticket.getPlayerName());
            placeholders.put("description_short", shortDescription);
            
            String ticketText = plugin.getMessageManager().formatMessage("ticket-list-format", placeholders);
            
            // Base text component
            TextComponent ticketInfo = new TextComponent(ticketText + " ");
            
            // Aggiunta dei pulsanti solo se il ticket è ancora aperto
            if (ticket.isOpen()) {
                // Pulsante Risposta con stile migliorato
                TextComponent replyButton = new TextComponent("§2[§a✉§2]");
                replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§aClicca per rispondere al ticket").create()));
                replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                        "/tk reply " + ticket.getId() + " "));
                
                // Spazio tra i pulsanti
                TextComponent spacer = new TextComponent(" ");
                
                // Pulsante Chiudi con stile migliorato
                TextComponent closeButton = new TextComponent("§4[§c✕§4]");
                closeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§cClicca per chiudere il ticket").create()));
                closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/tk close " + ticket.getId()));
                
                // Pulsante Info con stile migliorato
                TextComponent infoButton = new TextComponent("§9[§b?§9]");
                infoButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§bClicca per vedere i dettagli del ticket").create()));
                infoButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/tk info " + ticket.getId()));
                
                // Aggiungi i pulsanti al componente principale
                ticketInfo.addExtra(spacer);
                ticketInfo.addExtra(infoButton);
                ticketInfo.addExtra(spacer);
                ticketInfo.addExtra(replyButton);
                
                // Aggiungi il pulsante di chiusura solo per lo staff o il creatore del ticket
                if (player.hasPermission("gktickets.staff") || ticket.getPlayerUuid().equals(player.getUniqueId())) {
                    ticketInfo.addExtra(spacer);
                    ticketInfo.addExtra(closeButton);
                }
            }
            
            // Invia il messaggio interattivo
            player.spigot().sendMessage(ticketInfo);
        }
        
        // Invia il divisore di chiusura
        player.sendMessage(plugin.getMessageManager().getMessage("divider"));
    }
    
    private void handleInfoCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.info")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length != 2) {
            player.sendMessage("§cUtilizzo corretto: /ticket info <id>");
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§8[§6GKTickets§8] §cL'ID del ticket deve essere un numero.");
            return;
        }
        
        Ticket ticket = databaseManager.getTicketById(ticketId);
        
        if (ticket == null) {
            player.sendMessage("§8[§6GKTickets§8] §cTicket non trovato.");
            return;
        }
        
        // Verifica se il giocatore può visualizzare questo ticket
        if (!player.hasPermission("gktickets.staff") && !ticket.getPlayerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§8[§6GKTickets§8] §cNon puoi visualizzare questo ticket.");
            return;
        }
        
        // Mostra le informazioni del ticket con un'interfaccia migliorata
        String statusColor = ticket.isOpen() ? "§a" : "§c";
        
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("id", String.valueOf(ticket.getId()));
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-header", headerPlaceholders));
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-title", headerPlaceholders));
        
        Map<String, String> infoPlaceholders = new HashMap<>();
        infoPlaceholders.put("id", String.valueOf(ticket.getId()));
        infoPlaceholders.put("status_color", statusColor);
        infoPlaceholders.put("status", ticket.getStatus());
        infoPlaceholders.put("player", ticket.getPlayerName());
        infoPlaceholders.put("created_at", ticket.getCreatedAt());
        infoPlaceholders.put("description", ticket.getDescription());
        
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-status", infoPlaceholders));
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-creator", infoPlaceholders));
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-date", infoPlaceholders));
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-description", infoPlaceholders));
        
        // Mostra le risposte
        List<Reply> replies = ticket.getReplies();
        if (replies.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-info-no-replies"));
        } else {
            Map<String, String> replyHeaderPlaceholders = new HashMap<>();
            replyHeaderPlaceholders.put("count", String.valueOf(replies.size()));
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-replies-title", replyHeaderPlaceholders));
            
            for (Reply reply : replies) {
                Map<String, String> replyPlaceholders = new HashMap<>();
                replyPlaceholders.put("player", reply.getPlayerName());
                replyPlaceholders.put("created_at", reply.getCreatedAt());
                replyPlaceholders.put("message", reply.getMessage());
                
                player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-reply-format", replyPlaceholders));
            }
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("ticket-info-footer"));
        
        // Aggiungi pulsanti interattivi se il ticket è ancora aperto
        if (ticket.isOpen()) {
            // Invia i pulsanti solo come componenti di testo con una grafica migliore
            TextComponent buttonsMessage = new TextComponent(" ");
            
            // Pulsante Rispondi
            TextComponent replyButton = new TextComponent("§2[§a✉ Rispondi§2]");
            replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§aClicca per rispondere al ticket").create()));
            replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/tk reply " + ticket.getId() + " "));
            
            // Spazio tra i pulsanti
            TextComponent spacer = new TextComponent(" ");
            
            // Pulsante Chiudi
            TextComponent closeButton = new TextComponent("§4[§c✕ Chiudi§4]");
            closeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§cClicca per chiudere il ticket").create()));
            closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/tk close " + ticket.getId()));
            
            // Aggiungi i pulsanti al componente principale
            buttonsMessage.addExtra(replyButton);
            
            // Aggiungi il pulsante di chiusura solo per lo staff o il creatore del ticket
            if (player.hasPermission("gktickets.staff") || ticket.getPlayerUuid().equals(player.getUniqueId())) {
                buttonsMessage.addExtra(spacer);
                buttonsMessage.addExtra(closeButton);
            }
            
            player.spigot().sendMessage(buttonsMessage);
        }
    }
    
    private void handleCloseCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.close")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-close-usage"));
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-not-found"));
            return;
        }
        
        Ticket ticket = databaseManager.getTicketById(ticketId);
        
        if (ticket == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-not-found"));
            return;
        }
        
        if (!ticket.isOpen()) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-already-closed"));
            return;
        }
        
        // Verifica i permessi specifici per la chiusura: proprio ticket o di altri
        boolean isTicketOwner = ticket.getPlayerUuid().equals(player.getUniqueId());
        
        if (isTicketOwner && !player.hasPermission("gktickets.close.own")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (!isTicketOwner && !player.hasPermission("gktickets.close.others")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        boolean success = databaseManager.closeTicket(ticketId);
        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticketId));
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-close-success", placeholders));
            
            // Notifica al giocatore che ha creato il ticket
            notificationManager.notifyPlayerTicketClosed(ticket, player);
            
            // Log su Discord
            plugin.getDiscordIntegration().sendTicketClosedMessage(ticket, player);
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-close-error"));
        }
    }
    
    private void handleReplyCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.reply")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage("§cUtilizzo corretto: /ticket reply <id> <messaggio>");
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§8[§6GKTickets§8] §cL'ID del ticket deve essere un numero.");
            return;
        }
        
        Ticket ticket = databaseManager.getTicketById(ticketId);
        
        if (ticket == null) {
            player.sendMessage("§8[§6GKTickets§8] §cTicket non trovato.");
            return;
        }
        
        if (!ticket.isOpen()) {
            player.sendMessage("§8[§6GKTickets§8] §cNon puoi rispondere a un ticket chiuso.");
            return;
        }
        
        // Verifica se il giocatore può rispondere a questo ticket
        if (!player.hasPermission("gktickets.staff") && !ticket.getPlayerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§8[§6GKTickets§8] §cNon puoi rispondere a questo ticket.");
            return;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        boolean success = databaseManager.addReply(ticketId, player, message);
        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticketId));
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-reply-success", placeholders));
            
            // Ottieni la risposta appena creata (ultima risposta del ticket)
            Ticket updatedTicket = databaseManager.getTicketById(ticketId);
            List<Reply> replies = updatedTicket.getReplies();
            if (!replies.isEmpty()) {
                Reply lastReply = replies.get(replies.size() - 1);
                
                // Log su Discord
                plugin.getDiscordIntegration().sendTicketReplyMessage(updatedTicket, lastReply, player);
            }
            
            // Notifiche in-game
            if (player.hasPermission("gktickets.staff")) {
                notificationManager.notifyStaffNewReply(ticket, player.getName());
                notificationManager.notifyPlayerNewReply(ticket, player);
            }
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-reply-error"));
        }
    }
    
    private void handleTestDiscordCommand(Player player) {
        if (!player.hasPermission("gktickets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (!plugin.getDiscordIntegration().isEnabled()) {
            player.sendMessage("§8[§6GKTickets§8] §cL'integrazione Discord non è abilitata. Controllare la configurazione.");
            return;
        }
        
        // Crea un ticket di test
        Ticket testTicket = new Ticket(
            9999,
            player.getUniqueId(),
            player.getName(),
            "Questo è un ticket di test per Discord",
            "OPEN",
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );
        
        // Invia un messaggio di test
        plugin.getDiscordIntegration().sendTicketCreatedMessage(testTicket, player);
        player.sendMessage("§8[§6GKTickets§8] §aTest Discord inviato! Controlla il canale Discord configurato.");
    }
    
    private void handleUserCommand(Player player, String[] args) {
        // Verifica i permessi
        if (!player.hasPermission("gktickets.staff") && !player.hasPermission("gktickets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-user-usage"));
            return;
        }
        
        String targetPlayerName = args[1];
        List<Ticket> tickets = databaseManager.getPlayerTicketsByName(targetPlayerName);
        
        if (tickets.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetPlayerName);
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-user-not-found", placeholders));
            return;
        }
        
        // Invia l'header con il divisore
        player.sendMessage(plugin.getMessageManager().getMessage("divider"));
        
        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("player", targetPlayerName);
        player.sendMessage(plugin.getMessageManager().formatMessage("ticket-user-title", titlePlaceholders));
        
        for (Ticket ticket : tickets) {
            String statusColor = ticket.isOpen() ? "§a" : "§c";
            String status = ticket.getStatus();
            String shortDescription = ticket.getDescription().length() > 25 ? 
                                      ticket.getDescription().substring(0, 25) + "..." : 
                                      ticket.getDescription();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticket.getId()));
            placeholders.put("status_color", statusColor);
            placeholders.put("status", status);
            placeholders.put("player", ticket.getPlayerName());
            placeholders.put("description_short", shortDescription);
            
            String ticketText = plugin.getMessageManager().formatMessage("ticket-list-format", placeholders);
            
            // Base text component
            TextComponent ticketInfo = new TextComponent(ticketText + " ");
            
            // Pulsante Info con stile migliorato
            TextComponent infoButton = new TextComponent("§9[§b?§9]");
            infoButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§bClicca per vedere i dettagli del ticket").create()));
            infoButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/tk info " + ticket.getId()));
            
            // Spazio tra i pulsanti
            TextComponent spacer = new TextComponent(" ");
            
            // Aggiungi i pulsanti al componente principale
            ticketInfo.addExtra(spacer);
            ticketInfo.addExtra(infoButton);
            
            // Aggiungi pulsante Risposta se il ticket è aperto
            if (ticket.isOpen()) {
                // Pulsante Risposta con stile migliorato
                TextComponent replyButton = new TextComponent("§2[§a✉§2]");
                replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§aClicca per rispondere al ticket").create()));
                replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                        "/tk reply " + ticket.getId() + " "));
                
                ticketInfo.addExtra(spacer);
                ticketInfo.addExtra(replyButton);
                
                // Pulsante Chiudi
                TextComponent closeButton = new TextComponent("§4[§c✕§4]");
                closeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§cClicca per chiudere il ticket").create()));
                closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/tk close " + ticket.getId()));
                
                ticketInfo.addExtra(spacer);
                ticketInfo.addExtra(closeButton);
            }
            
            // Invia il messaggio interattivo
            player.spigot().sendMessage(ticketInfo);
        }
        
        // Invia il divisore di chiusura
        player.sendMessage(plugin.getMessageManager().getMessage("divider"));
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("help-title"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-create"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-list"));
        
        if (player.hasPermission("gktickets.staff") || player.hasPermission("gktickets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help-user"));
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("help-info"));
        
        if (player.hasPermission("gktickets.staff")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help-close"));
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("help-reply"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Primo argomento - subcomandi
            List<String> subCommands = new ArrayList<>();
            subCommands.add("create");
            subCommands.add("list");
            subCommands.add("info");
            subCommands.add("reply");
            
            if (sender.hasPermission("gktickets.staff") || sender.hasPermission("gktickets.admin")) {
                subCommands.add("close");
                subCommands.add("user"); // Aggiungiamo il nuovo subcomando
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Secondo argomento - ID tickets per info/close/reply o username per user
            if (args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("close") || 
                args[0].equalsIgnoreCase("reply")) {
                
                List<Ticket> tickets;
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission("gktickets.staff")) {
                        tickets = databaseManager.getOpenTickets();
                    } else {
                        tickets = databaseManager.getPlayerTickets(player.getUniqueId());
                    }
                    
                    completions = tickets.stream()
                                        .map(ticket -> String.valueOf(ticket.getId()))
                                        .filter(id -> id.startsWith(args[1]))
                                        .collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("user") && 
                      (sender.hasPermission("gktickets.staff") || sender.hasPermission("gktickets.admin"))) {
                // Suggerimenti per i nomi dei giocatori online
                String partial = args[1].toLowerCase();
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
