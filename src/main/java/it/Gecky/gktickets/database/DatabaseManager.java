package it.Gecky.gktickets;

import it.Gecky.gktickets.commands.TicketCommand;
import it.Gecky.gktickets.database.DatabaseManager;
import it.Gecky.gktickets.integrations.DiscordIntegration;
import it.Gecky.gktickets.notifications.NotificationManager;
import it.Gecky.gktickets.utils.ConfigManager;
import it.Gecky.gktickets.utils.MessageManager;
import it.Gecky.gktickets.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class GKTickets extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private NotificationManager notificationManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DiscordIntegration discordIntegration;
    private VersionUtils versionUtils;
    private boolean placeholderAPIEnabled = false;
    private final String version = "1.0.2";

    @Override
    public void onEnable() {
        // Creazione della cartella del plugin se non esiste
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        
        // Inizializzazione dell'utility per la versione
        this.versionUtils = new VersionUtils(this);
        
        // Mostra banner di avvio migliorato
        getLogger().info("╔════════════════════════════╗");
        getLogger().info("║      GKTickets v" + version + "      ║");
        getLogger().info("║    Creato da: gecky2102     ║");
        getLogger().info("╚════════════════════════════╝");
        getLogger().info("Server versione: " + versionUtils.getServerVersionString());
        
        // Caricamento configurazione e messaggi
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        
        // Inizializzazione del database manager
        this.databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Inizializzazione del notification manager
        this.notificationManager = new NotificationManager(this);
        
        // Inizializzazione dell'integrazione Discord
        this.discordIntegration = new DiscordIntegration(this);
        if (this.discordIntegration.isEnabled()) {
            getLogger().info("Integrazione Discord abilitata!");
        }
        
        // Hook PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI trovato e collegato con successo!");
        } else {
            getLogger().warning("PlaceholderAPI non trovato. Le placeholders non saranno disponibili.");
        }
        
        // Registrazione dei comandi
        getCommand("ticket").setExecutor(new TicketCommand(this));
        getCommand("ticket").setTabCompleter(new TicketCommand(this));
        
        // Supporto di alias per versioni precedenti
        try {
            getCommand("tk").setExecutor(new TicketCommand(this));
            getCommand("tk").setTabCompleter(new TicketCommand(this));
        } catch (Exception e) {
            getLogger().warning("Non è stato possibile registrare l'alias 'tk'. Questo potrebbe essere normale per alcune versioni di Bukkit.");
        }
        
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
    
    public DiscordIntegration getDiscordIntegration() {
        return discordIntegration;
    }
    
    public VersionUtils getVersionUtils() {
        return versionUtils;
    }
    
    /**
     * Verifica se la versione del server è uguale o successiva a quella specificata.
     * @param major Versione maggiore (es. 1)
     * @param minor Versione minore (es. 13)
     * @return true se il server è alla versione specificata o successiva
     */
    public boolean isVersionOrHigher(int major, int minor) {
        return versionUtils.isVersionOrHigher(major, minor);
    }
}