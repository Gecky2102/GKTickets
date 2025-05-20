package it.giankoyt.gktickets;

import it.giankoyt.gktickets.commands.TicketCommand;
import it.giankoyt.gktickets.database.DatabaseManager;
import it.giankoyt.gktickets.notifications.NotificationManager;
import it.giankoyt.gktickets.utils.ConfigManager;
import it.giankoyt.gktickets.utils.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class GKTickets extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private NotificationManager notificationManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        // Creazione della cartella del plugin se non esiste
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        
        // Caricamento configurazione e messaggi
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        
        // Inizializzazione del database manager
        this.databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Inizializzazione del notification manager
        this.notificationManager = new NotificationManager(this);
        
        // Hook PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI trovato e collegato con successo!");
        } else {
            getLogger().warning("PlaceholderAPI non trovato. Le placeholders non saranno disponibili.");
        }
        
        // Registrazione dei comandi
        getCommand("ticket").setExecutor(new TicketCommand(this));
        
        getLogger().info("GKTickets è stato avviato con successo!");
    }

    @Override
    public void onDisable() {
        // Chiusura della connessione al database
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        getLogger().info("GKTickets è stato disabilitato.");
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
}
