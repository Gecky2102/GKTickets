package it.Gecky.gktickets.commands;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.database.DatabaseManager;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
import it.Gecky.gktickets.notifications.NotificationManager;
import it.Gecky.gktickets.categories.CategoryManager;
import it.Gecky.gktickets.categories.TicketCategory;
import it.Gecky.gktickets.blacklist.BlacklistEntry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TicketCommand implements CommandExecutor, TabCompleter {

    private final GKTickets plugin;
    private final DatabaseManager databaseManager;
    private final NotificationManager notificationManager;
    private CategoryManager categoryManager;

    public TicketCommand(GKTickets plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.notificationManager = plugin.getNotificationManager();
        this.categoryManager = plugin.getCategoryManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("player-only"));
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
            case "categories":
                handleCategoriesCommand(player);
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
            case "feedback":
                handleFeedbackCommand(player, args);
                break;
            case "stats":
                handleStatsCommand(player, args);
                break;
            case "testdiscord":
                handleTestDiscordCommand(player);
                break;
            case "reload":
                handleReloadCommand(player);
                break;
            case "note":
                handleNoteCommand(player, args);
                break;
            case "blacklist":
                handleBlacklistCommand(player, args);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Gestisce il comando per visualizzare le categorie disponibili
     */
    private void handleCategoriesCommand(Player player) {
        if (!player.hasPermission("gktickets.create")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("category-list-header"));
        
        List<String> availableCategories = categoryManager.getAvailableCategories(player);
        for (String categoryId : availableCategories) {
            TicketCategory category = categoryManager.getCategory(categoryId);
            if (category != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("category", category.getFormattedNameWithIcon());
                placeholders.put("description", category.getDescription());
                player.sendMessage(plugin.getMessageManager().formatMessage("category-list-item", placeholders));
            }
        }
    }
    
    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.create")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-create-usage-with-category"));
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
        
        // Verifica se il primo argomento è una categoria
        String categoryId = "general"; // Categoria predefinita
        String description;
        
        if (categoryManager != null && categoryManager.categoryExists(args[1])) {
            // Il primo argomento è una categoria valida
            categoryId = args[1].toLowerCase();
            
            // Verifica se il giocatore può usare questa categoria
            if (!categoryManager.canUseCategory(player, categoryId)) {
                player.sendMessage(plugin.getMessageManager().getMessage("category-no-permission"));
                return;
            }
            
            // Verifica che ci sia una descrizione
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getMessage("ticket-create-usage-with-category"));
                return;
            }
            
            // Combina tutti gli argomenti dopo la categoria in una descrizione
            description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            // Non è stata specificata una categoria, usa la descrizione direttamente
            description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        
        // Crea il ticket con la categoria specificata
        int ticketId;
        if (categoryManager != null) {
            ticketId = databaseManager.createTicket(player, categoryId, description);
        } else {
            ticketId = databaseManager.createTicket(player, description);
        }
        
        if (ticketId != -1) {
            String categoryName = categoryId;
            if (categoryManager != null) {
                TicketCategory category = categoryManager.getCategory(categoryId);
                if (category != null) {
                    categoryName = category.getFormattedNameWithIcon();
                }
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticketId));
            placeholders.put("category", categoryName);
            
            if (categoryManager != null) {
                player.sendMessage(plugin.getMessageManager().formatMessage("ticket-create-success-with-category", placeholders));
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("ticket-create-success", placeholders));
            }
            
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
            player.sendMessage("Utilizzo corretto: /ticket info <id>");
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("L'ID del ticket deve essere un numero.");
            return;
        }
        
        Ticket ticket = databaseManager.getTicketById(ticketId);
        
        if (ticket == null) {
            player.sendMessage("Ticket non trovato.");
            return;
        }
        
        // Verifica se il giocatore può visualizzare questo ticket
        if (!player.hasPermission("gktickets.staff") && !ticket.getPlayerUuid().equals(player.getUniqueId())) {
            player.sendMessage("Non puoi visualizzare questo ticket.");
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
        
        // Mostra le note dello staff se presenti
        if (player.hasPermission("gktickets.staff")) {
            List<it.Gecky.gktickets.models.StaffNote> notes = plugin.getStaffNoteManager().getNotesForTicket(ticket.getId());
            
            if (!notes.isEmpty()) {
                Map<String, String> noteTitlePlaceholders = new HashMap<>();
                noteTitlePlaceholders.put("count", String.valueOf(notes.size()));
                player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-notes-title", noteTitlePlaceholders));
                
                for (it.Gecky.gktickets.models.StaffNote note : notes) {
                    Map<String, String> notePlaceholders = new HashMap<>();
                    notePlaceholders.put("staff", note.getStaffName());
                    notePlaceholders.put("date", note.getCreatedAt());
                    notePlaceholders.put("note", note.getNote());
                    
                    player.sendMessage(plugin.getMessageManager().formatMessage("ticket-info-note-format", notePlaceholders));
                }
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("ticket-info-no-notes"));
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
        
        // Fix: Change this line to handle boolean return type correctly
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
            
            // Aggiungi i pulsanti al componente principale
            ticketInfo.addExtra(infoButton);
            
            // Aggiungi pulsante Risposta se il ticket è aperto ? 
            if (ticket.isOpen()) {
                // Pulsante Risposta con stile migliorato
                TextComponent replyButton = new TextComponent("§2[§a✉§2]");
                replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§aClicca per rispondere al ticket").create()));
                replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                        "/tk reply " + ticket.getId() + " "));
                ticketInfo.addExtra(replyButton);
                
                // Pulsante Chiudi
                TextComponent closeButton = new TextComponent("§4[§c✕§4]");
                closeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§cClicca per chiudere il ticket").create()));
                closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/tk close " + ticket.getId()));
                ticketInfo.addExtra(closeButton);
            }
            
            // Invia il messaggio interattivo
            player.spigot().sendMessage(ticketInfo);
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("divider"));
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("help-header"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-title"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-create"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-list"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-info"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-reply"));
        player.sendMessage(plugin.getMessageManager().getMessage("help-close"));
        
        if (player.hasPermission("gktickets.staff")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help-staff-header"));
            player.sendMessage(plugin.getMessageManager().getMessage("help-user"));
            player.sendMessage(plugin.getMessageManager().getMessage("help-note"));
        }
        
        if (player.hasPermission("gktickets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help-stats"));
        }
        
        if (player.hasPermission("gktickets.blacklist")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help-blacklist"));
        }
        
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
            subCommands.add("close");
            subCommands.add("feedback");
            subCommands.add("categories");
            
            if (sender.hasPermission("gktickets.staff") || sender.hasPermission("gktickets.admin")) {
                subCommands.add("user");
                subCommands.add("note");
                subCommands.add("stats");
                subCommands.add("reload");
                subCommands.add("blacklist");
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create") && sender instanceof Player) {
                // Suggerisci le categorie disponibili
                if (categoryManager != null) {
                    Player player = (Player) sender;
                    List<String> availableCategories = categoryManager.getAvailableCategories(player);
                    
                    for (String category : availableCategories) {
                        if (category.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(category);
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("info") || 
                     args[0].equalsIgnoreCase("close") || 
                     args[0].equalsIgnoreCase("reply") ||
                     args[0].equalsIgnoreCase("feedback")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<Ticket> tickets;
                    
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
            } else if (args[0].equalsIgnoreCase("note")) {
                if (sender.hasPermission("gktickets.staff")) {
                    // Suggerisci gli ID dei ticket
                    for (Ticket ticket : databaseManager.getOpenTickets()) {
                        String ticketId = String.valueOf(ticket.getId());
                        if (ticketId.startsWith(args[1])) {
                            completions.add(ticketId);
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("blacklist")) {
                if (sender.hasPermission("gktickets.blacklist")) {
                    List<String> subCommands = new ArrayList<>();
                    subCommands.add("add");
                    subCommands.add("remove");
                    subCommands.add("list");
                    subCommands.add("info");
                    
                    return subCommands.stream()
                           .filter(cmd -> cmd.startsWith(args[1].toLowerCase()))
                           .collect(Collectors.toList());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("blacklist")) {
                if ((args[1].equalsIgnoreCase("remove") && sender.hasPermission("gktickets.blacklist.remove")) ||
                    (args[1].equalsIgnoreCase("info") && sender.hasPermission("gktickets.blacklist.info"))) {
                    // Suggerisci i nomi dei giocatori per rimuovere o info
                    String partial = args[2].toLowerCase();
                    List<String> names = new ArrayList<>();
                    
                    for (BlacklistEntry entry : plugin.getBlacklistManager().getBlacklistedPlayers()) {
                        if (entry.getPlayerName().toLowerCase().startsWith(partial)) {
                            names.add(entry.getPlayerName());
                        }
                    }
                    
                    return names;
                } else if (args[1].equalsIgnoreCase("add") && sender.hasPermission("gktickets.blacklist.add")) {
                    // Suggerisci i giocatori online per l'aggiunta
                    String partial = args[2].toLowerCase();
                    return plugin.getServer().getOnlinePlayers().stream()
                           .map(Player::getName)
                           .filter(name -> name.toLowerCase().startsWith(partial))
                           .collect(Collectors.toList());
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Gestisce il comando per dare feedback a un ticket chiuso
     */
    private void handleFeedbackCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.feedback")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-usage"));
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-not-found"));
            return;
        }
        
        // Verifica che il ticket esista, sia chiuso e appartenga al giocatore
        Ticket ticket = databaseManager.getTicketById(ticketId);
        if (ticket == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-not-found"));
            return;
        }
        
        if (!ticket.getPlayerUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-not-owner"));
            return;
        }
        
        if (ticket.isOpen()) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-still-open"));
            return;
        }
        
        // Verifica se è già stato dato feedback
        if (databaseManager.hasTicketFeedback(ticketId)) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-already-given"));
            return;
        }
        
        // Se è specificato solo l'ID, mostra l'interfaccia di valutazione
        if (args.length == 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-header"));
            
            // Costruisci i pulsanti per le stelle
            for (int rating = 1; rating <= 5; rating++) {
                TextComponent star = new TextComponent(plugin.getMessageManager().formatMessage("ticket-feedback-star", 
                        Map.of("rating", String.valueOf(rating))));
                        
                star.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§7Valuta questo ticket con " + rating + " " + (rating == 1 ? "stella" : "stelle")).create()));
                star.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/ticket feedback " + ticketId + " " + rating));
                
                player.spigot().sendMessage(star);
            }
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-footer"));
            return;
        }
        
        // Processa la valutazione
        int rating;
        try {
            rating = Integer.parseInt(args[2]);
            if (rating < 1 || rating > 5) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-invalid-rating"));
            return;
        }
        
        // Salva il commento se fornito
        String comment = null;
        if (args.length > 3) {
            comment = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }
        
        // Salva il feedback nel database
        boolean success = databaseManager.saveTicketFeedback(ticketId, player.getUniqueId(), rating, comment);
        
        if (success) {
            player.sendMessage(plugin.getMessageManager().formatMessage("ticket-feedback-success", 
                    Map.of("id", String.valueOf(ticketId), "rating", String.valueOf(rating))));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-feedback-error"));
        }
    }
    
    /**
     * Gestisce il comando per visualizzare le statistiche
     */
    private void handleStatsCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.stats")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        // Ottieni le statistiche
        Map<String, Object> stats = databaseManager.getTicketStats();
        
        // Formatta e mostra le statistiche
        player.sendMessage(plugin.getMessageManager().getMessage("stats-header"));
        
        // Mostra conteggi ticket
        player.sendMessage(plugin.getMessageManager().formatMessage(
            "stats-tickets-total", 
            Map.of("count", String.valueOf(stats.getOrDefault("total", 0)))
        ));
        
        player.sendMessage(plugin.getMessageManager().formatMessage(
            "stats-tickets-open", 
            Map.of("count", String.valueOf(stats.getOrDefault("open", 0)))
        ));
        
        player.sendMessage(plugin.getMessageManager().formatMessage(
            "stats-tickets-closed", 
            Map.of("count", String.valueOf(stats.getOrDefault("closed", 0)))
        ));
        
        // Mostra valutazione media
        player.sendMessage(plugin.getMessageManager().formatMessage(
            "stats-avg-rating", 
            Map.of("rating", (String) stats.getOrDefault("avg_rating", "N/A"))
        ));
        
        // Mostra distribuzione valutazioni
        player.sendMessage(plugin.getMessageManager().getMessage("stats-rating-distribution"));
        
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> ratingDistribution = (Map<Integer, Integer>) stats.getOrDefault("rating_distribution", new HashMap<Integer, Integer>());
        
        if (ratingDistribution != null) {
            int totalRatings = ratingDistribution.values().stream().mapToInt(Integer::intValue).sum();
            if (totalRatings > 0) {
                // Per ogni valutazione da 1 a 5
                for (int stars = 1; stars <= 5; stars++) {
                    int count = ratingDistribution.getOrDefault(stars, 0);
                    
                    // Calcola la percentuale
                    int percentage = (totalRatings > 0) ? (count * 100 / totalRatings) : 0;
                    
                    // Costruisci la barra di progresso - FIX COLOR FORMATTING
                    StringBuilder bar = new StringBuilder();
                    int barLength = 10; // Lunghezza totale della barra
                    int filledBars = (int) Math.round((double) percentage / 100 * barLength);
                    
                    for (int i = 0; i < filledBars; i++) {
                        bar.append("§a|");  // Green filled bar with § prefix
                    }
                    for (int i = filledBars; i < barLength; i++) {
                        bar.append("§7|");  // Gray empty bar with § prefix
                    }
                    
                    // Create placeholders map with proper escaping for color codes
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("stars", String.valueOf(stars));
                    placeholders.put("count", String.valueOf(count));
                    placeholders.put("bar", ChatColor.translateAlternateColorCodes('§', bar.toString()));
                    placeholders.put("percentage", String.valueOf(percentage));
                    
                    // Send formatted message
                    String message = plugin.getMessageManager().formatMessage("stats-rating-bar", placeholders);
                    player.sendMessage(message);
                }
            } else {
                player.sendMessage("  §7Nessuna valutazione disponibile");
            }
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("stats-footer"));
    }
    
    /**
     * Gestisce il comando per ricaricare le configurazioni
     */
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("gktickets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        try {
            // Call only the main reloadConfig method
            plugin.reloadConfig();
            
            // Force reference update for the category manager
            this.categoryManager = plugin.getCategoryManager();
            
            player.sendMessage(plugin.getMessageManager().getMessage("reload-success"));
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getMessage("reload-error"));
            plugin.getLogger().severe("Errore durante il ricaricamento: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gestisce il comando per aggiungere note dello staff ai ticket
     */
    private void handleNoteCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.staff")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage("note-usage"));
            return;
        }
        
        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("invalid-ticket-id"));
            return;
        }
        
        Ticket ticket = databaseManager.getTicketById(ticketId);
        if (ticket == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("ticket-not-found"));
            return;
        }
        
        // Combina gli argomenti rimanenti come nota
        String note = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        // Aggiungi la nota
        boolean success = plugin.getStaffNoteManager().addNote(ticketId, player, note);
        
        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", String.valueOf(ticketId));
            player.sendMessage(plugin.getMessageManager().formatMessage("note-added-success", placeholders));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("note-added-error"));
        }
    }
    
    /**
     * Gestisce i comandi della blacklist
     */
    private void handleBlacklistCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.blacklist")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("blacklist-usage"));
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "add":
                handleBlacklistAddCommand(player, args);
                break;
            case "remove":
                handleBlacklistRemoveCommand(player, args);
                break;
            case "list":
                handleBlacklistListCommand(player, args);
                break;
            case "info":
                handleBlacklistInfoCommand(player, args);
                break;
            default:
                player.sendMessage(plugin.getMessageManager().getMessage("blacklist-usage"));
                break;
        }
    }
    
    /**
     * Gestisce l'aggiunta di un giocatore alla blacklist
     */
    private void handleBlacklistAddCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.blacklist.add")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 4) {
            player.sendMessage(plugin.getMessageManager().getMessage("blacklist-add-usage"));
            return;
        }
        
        String targetName = args[2];
        
        // Trova il giocatore di destinazione (può essere offline)
        UUID targetUUID = null;
        String finalTargetName = targetName;
        
        // Controlla prima se il giocatore è online
        Player target = plugin.getServer().getPlayer(targetName);
        if (target != null) {
            targetUUID = target.getUniqueId();
            finalTargetName = target.getName();
        } else {
            // Prova a trovarlo come giocatore offline
            try {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(targetName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    finalTargetName = offlinePlayer.getName();
                }
            } catch (Exception ex) {
                player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
                return;
            }
        }
        
        if (targetUUID == null) {
            player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
            return;
        }
        
        // Controlla se è già nella blacklist
        if (plugin.getBlacklistManager().isBlacklisted(targetUUID)) {
            Map<String, String> placeholders = Map.of("player", finalTargetName);
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-player-already", placeholders));
            return;
        }
        
        // Ottieni il motivo (combina tutti gli argomenti dall'indice 3 in poi)
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        // Ottieni i giorni di scadenza se specificati
        int expiryDays = plugin.getConfig().getInt("blacklist.default-expiry", 7);
        if (args.length > 4) {
            try {
                expiryDays = Integer.parseInt(args[args.length - 1]);
                
                // Se è stato specificato il termine, rimuovilo dal motivo
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));
            } catch (NumberFormatException ignored) {
                // L'ultimo argomento non era un numero, va bene così
            }
        }
        
        // Aggiungi alla blacklist
        boolean success = plugin.getBlacklistManager().blacklistPlayer(
            targetUUID, finalTargetName, reason, player.getUniqueId(), player.getName(), expiryDays);
        
        if (success) {
            // Notifica all'amministratore
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", finalTargetName);
            placeholders.put("reason", reason);
            
            if (expiryDays > 0) {
                placeholders.put("days", String.valueOf(expiryDays));
                player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-add-with-expiry", placeholders));
            } else {
                player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-add-permanent", placeholders));
            }
            
            // Notifica al giocatore se è online
            if (target != null && target.isOnline()) {
                placeholders.put("reason", reason);
                
                target.sendMessage(plugin.getMessageManager().formatMessage("blacklist-player-denied", placeholders));
                target.sendMessage(plugin.getMessageManager().formatMessage("blacklist-reason", placeholders));
                
                if (expiryDays > 0) {
                    placeholders.put("days", String.valueOf(expiryDays));
                    target.sendMessage(plugin.getMessageManager().formatMessage("blacklist-expires", placeholders));
                } else {
                    target.sendMessage(plugin.getMessageManager().getMessage("blacklist-permanent"));
                }
            }
        } else {
            player.sendMessage("§cErrore durante l'aggiunta del giocatore alla blacklist.");
        }
    }
    
    /**
     * Gestisce la rimozione di un giocatore dalla blacklist
     */
    private void handleBlacklistRemoveCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.blacklist.remove")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage("blacklist-remove-usage"));
            return;
        }
        
        String targetName = args[2];
        
        // Trova il giocatore di destinazione (può essere offline)
        UUID targetUUID = null;
        String finalTargetName = targetName;
        
        // Controlla prima se il giocatore è online
        Player target = plugin.getServer().getPlayer(targetName);
        if (target != null) {
            targetUUID = target.getUniqueId();
            finalTargetName = target.getName();
        } else {
            // Prova a trovarlo come giocatore offline
            try {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(targetName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    finalTargetName = offlinePlayer.getName();
                }
            } catch (Exception ex) {
                player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
                return;
            }
        }
        
        if (targetUUID == null) {
            player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
            return;
        }
        
        // Rimuovi dalla blacklist
        boolean success = plugin.getBlacklistManager().unblacklistPlayer(targetUUID);
        
        if (success) {
            // Notifica all'amministratore
            Map<String, String> placeholders = Map.of("player", finalTargetName);
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-remove-success", placeholders));
            
            // Notifica al giocatore se è online
            if (target != null && target.isOnline()) {
                target.sendMessage(plugin.getMessageManager().getMessage("prefix") + 
                                 " §aSei stato rimosso dalla blacklist e puoi creare ticket di nuovo.");
            }
        } else {
            Map<String, String> placeholders = Map.of("player", finalTargetName);
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-not-found", placeholders));
        }
    }
    
    /**
     * Elenca tutti i giocatori nella blacklist
     */
    private void handleBlacklistListCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.blacklist.list")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        List<BlacklistEntry> blacklistedPlayers = plugin.getBlacklistManager().getBlacklistedPlayers();
        
        if (blacklistedPlayers.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("blacklist-list-empty"));
            return;
        }
        
        // Messaggio di intestazione con conteggio
        Map<String, String> headerPlaceholders = Map.of("count", String.valueOf(blacklistedPlayers.size()));
        player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-list-header", headerPlaceholders));
        
        // Elenca ogni giocatore nella blacklist
        for (BlacklistEntry entry : blacklistedPlayers) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", entry.getPlayerName());
            placeholders.put("reason", entry.getReason());
            
            String expires;
            if (entry.isPermanent()) {
                expires = "Permanente";
            } else {
                int daysRemaining = plugin.getBlacklistManager().getDaysRemaining(entry.getPlayerUUID());
                expires = daysRemaining + " giorni";
            }
            placeholders.put("expires", expires);
            
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-list-item", placeholders));
        }
    }
    
    /**
     * Mostra informazioni dettagliate su un giocatore nella blacklist
     */
    private void handleBlacklistInfoCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.blacklist.info")) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(plugin.getMessageManager().getMessage("blacklist-info-usage"));
            return;
        }
        
        String targetName = args[2];
        
        // Trova il giocatore di destinazione (può essere offline)
        UUID targetUUID = null;
        String finalTargetName = targetName;
        
        // Controlla prima se il giocatore è online
        Player target = plugin.getServer().getPlayer(targetName);
        if (target != null) {
            targetUUID = target.getUniqueId();
            finalTargetName = target.getName();
        } else {
            // Prova a trovarlo come giocatore offline
            try {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(targetName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    finalTargetName = offlinePlayer.getName();
                }
            } catch (Exception ex) {
                player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
                return;
            }
        }
        
        if (targetUUID == null) {
            player.sendMessage("§cGiocatore non trovato. Assicurati di inserire il nome esatto.");
            return;
        }
        
        // Controlla se è nella blacklist
        if (!plugin.getBlacklistManager().isBlacklisted(targetUUID)) {
            Map<String, String> placeholders = Map.of("player", finalTargetName);
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-not-found", placeholders));
            return;
        }
        
        // Ottieni l'entry
        BlacklistEntry entry = null;
        for (BlacklistEntry bl : plugin.getBlacklistManager().getBlacklistedPlayers()) {
            if (bl.getPlayerUUID().equals(targetUUID)) {
                entry = bl;
                break;
            }
        }
        
        if (entry != null) {
            // Mostra informazioni dettagliate
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", entry.getPlayerName());
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-info-header", placeholders));
            
            placeholders.put("reason", entry.getReason());
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-info-reason", placeholders));
            
            placeholders.put("staff", entry.getBlacklistedByName());
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-info-by", placeholders));
            
            placeholders.put("date", entry.getCreatedAt());
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-info-date", placeholders));
            
            String expires;
            if (entry.isPermanent()) {
                expires = "Permanente";
            } else {
                expires = entry.getExpiresAt() + " (" + 
                         plugin.getBlacklistManager().getDaysRemaining(entry.getPlayerUUID()) + " giorni)";
            }
            placeholders.put("expires", expires);
            player.sendMessage(plugin.getMessageManager().formatMessage("blacklist-info-expires", placeholders));
        }
    }
}
