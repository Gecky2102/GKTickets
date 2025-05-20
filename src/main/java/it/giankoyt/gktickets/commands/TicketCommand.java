package it.giankoyt.gktickets.commands;

import it.giankoyt.gktickets.GKTickets;
import it.giankoyt.gktickets.database.DatabaseManager;
import it.giankoyt.gktickets.models.Reply;
import it.giankoyt.gktickets.models.Ticket;
import it.giankoyt.gktickets.notifications.NotificationManager;
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
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.create")) {
            player.sendMessage("§cNon hai il permesso di creare ticket.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUtilizzo corretto: /ticket create <descrizione>");
            return;
        }
        
        // Combina tutti gli argomenti dopo "create" in una descrizione
        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        int ticketId = databaseManager.createTicket(player, description);
        if (ticketId != -1) {
            player.sendMessage("§8[§6GKTickets§8] §aTicket creato con successo! ID: §e#" + ticketId);
            
            // Notifica allo staff
            Ticket newTicket = databaseManager.getTicketById(ticketId);
            notificationManager.notifyStaffNewTicket(newTicket);
        } else {
            player.sendMessage("§8[§6GKTickets§8] §cErrore durante la creazione del ticket. Riprova più tardi.");
        }
    }
    
    private void handleListCommand(Player player) {
        List<Ticket> tickets;
        
        if (player.hasPermission("gktickets.staff")) {
            // Lo staff può vedere tutti i ticket aperti
            tickets = databaseManager.getOpenTickets();
            player.sendMessage("§8[§6GKTickets§8] §7Lista dei ticket aperti:");
        } else if (player.hasPermission("gktickets.view")) {
            // I giocatori normali possono vedere solo i propri ticket
            tickets = databaseManager.getPlayerTickets(player.getUniqueId());
            player.sendMessage("§8[§6GKTickets§8] §7I tuoi ticket:");
        } else {
            player.sendMessage("§cNon hai il permesso di visualizzare i ticket.");
            return;
        }
        
        if (tickets.isEmpty()) {
            player.sendMessage("  §7Nessun ticket trovato.");
            return;
        }
        
        for (Ticket ticket : tickets) {
            String statusColor = ticket.isOpen() ? "§a" : "§c";
            String message = "  §e#" + ticket.getId() + " §7- " + statusColor + ticket.getStatus() + 
                             " §7- §f" + ticket.getPlayerName() + ": §7" + 
                             (ticket.getDescription().length() > 30 ? 
                             ticket.getDescription().substring(0, 30) + "..." : ticket.getDescription());
            player.sendMessage(message);
        }
    }
    
    private void handleInfoCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.view")) {
            player.sendMessage("§cNon hai il permesso di visualizzare i ticket.");
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
        
        // Mostra le informazioni del ticket
        String statusColor = ticket.isOpen() ? "§a" : "§c";
        player.sendMessage("§8[§6GKTickets§8] §7Info ticket #" + ticket.getId() + ":");
        player.sendMessage("  §7Stato: " + statusColor + ticket.getStatus());
        player.sendMessage("  §7Creato da: §f" + ticket.getPlayerName());
        player.sendMessage("  §7Data: §f" + ticket.getCreatedAt());
        player.sendMessage("  §7Descrizione: §f" + ticket.getDescription());
        
        // Mostra le risposte
        List<Reply> replies = ticket.getReplies();
        if (replies.isEmpty()) {
            player.sendMessage("  §7Nessuna risposta.");
        } else {
            player.sendMessage("  §7Risposte:");
            for (Reply reply : replies) {
                player.sendMessage("    §e" + reply.getPlayerName() + " §8(" + reply.getCreatedAt() + ")§7: §f" + reply.getMessage());
            }
        }
        
        // Aggiungi pulsanti interattivi se il ticket è ancora aperto
        if (ticket.isOpen()) {
            // Invia i pulsanti solo come componenti di testo
            TextComponent spacer = new TextComponent("  ");
            
            // Pulsante Rispondi
            TextComponent replyButton = new TextComponent("§a[Rispondi]");
            replyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clicca per rispondere al ticket").create()));
            replyButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/ticket reply " + ticket.getId() + " "));
            
            // Pulsante Chiudi (solo per lo staff o il creatore del ticket)
            TextComponent closeButton = new TextComponent("§c[Chiudi]");
            closeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clicca per chiudere il ticket").create()));
            closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/ticket close " + ticket.getId()));
            
            TextComponent buttonsMessage = new TextComponent("  ");
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
        if (!player.hasPermission("gktickets.staff")) {
            player.sendMessage("§cNon hai il permesso di chiudere i ticket.");
            return;
        }
        
        if (args.length != 2) {
            player.sendMessage("§cUtilizzo corretto: /ticket close <id>");
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
            player.sendMessage("§8[§6GKTickets§8] §cQuesto ticket è già chiuso.");
            return;
        }
        
        boolean success = databaseManager.closeTicket(ticketId);
        if (success) {
            player.sendMessage("§8[§6GKTickets§8] §aTicket #" + ticketId + " chiuso con successo.");
            notificationManager.notifyPlayerTicketClosed(ticket, player);
        } else {
            player.sendMessage("§8[§6GKTickets§8] §cErrore durante la chiusura del ticket. Riprova più tardi.");
        }
    }
    
    private void handleReplyCommand(Player player, String[] args) {
        if (!player.hasPermission("gktickets.view")) {
            player.sendMessage("§cNon hai il permesso di rispondere ai ticket.");
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
            player.sendMessage("§8[§6GKTickets§8] §aRisposta inviata con successo.");
            
            // Notifica la risposta
            if (player.hasPermission("gktickets.staff")) {
                notificationManager.notifyStaffNewReply(ticket, player.getName());
                notificationManager.notifyPlayerNewReply(ticket, player);
            }
        } else {
            player.sendMessage("§8[§6GKTickets§8] §cErrore durante l'invio della risposta. Riprova più tardi.");
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§8[§6GKTickets§8] §7Comandi disponibili:");
        player.sendMessage("  §e/ticket create <descrizione> §7- Crea un nuovo ticket");
        player.sendMessage("  §e/ticket list §7- Visualizza i ticket aperti");
        player.sendMessage("  §e/ticket info <id> §7- Visualizza i dettagli di un ticket");
        if (player.hasPermission("gktickets.staff")) {
            player.sendMessage("  §e/ticket close <id> §7- Chiudi un ticket");
        }
        player.sendMessage("  §e/ticket reply <id> <messaggio> §7- Rispondi a un ticket");
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
            
            if (sender.hasPermission("gktickets.staff")) {
                subCommands.add("close");
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Secondo argomento - ID tickets per info/close/reply
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
            }
        }
        
        return completions;
    }
}
