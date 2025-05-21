package it.Gecky.gktickets;

import it.Gecky.gktickets.blacklist.BlacklistManager;
import it.Gecky.gktickets.categories.CategoryManager;
import it.Gecky.gktickets.commands.TicketCommand;
import it.Gecky.gktickets.database.DatabaseManager;
import it.Gecky.gktickets.integrations.DiscordIntegration;
import it.Gecky.gktickets.notes.StaffNoteManager;
import it.Gecky.gktickets.notifications.NotificationManager;
import it.Gecky.gktickets.reporting.ReportManager;
import it.Gecky.gktickets.tasks.AutoCloseTask;
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
    private CategoryManager categoryManager;
    private StaffNoteManager staffNoteManager;
    private AutoCloseTask autoCloseTask;
    private VersionUtils versionUtils;
    private boolean placeholderAPIEnabled = false;
    private final String version = "1.0.3";
    private BlacklistManager blacklistManager;
    private ReportManager reportManager;
    
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
        getLogger().info("║    Creato da: gecky2102    ║");
        getLogger().info("╚════════════════════════════╝");
        getLogger().info("Server versione: " + versionUtils.getServerVersionString());
        
        // Caricamento configurazione e messaggi
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        
        // Inizializzazione del category manager
        this.categoryManager = new CategoryManager(this);
        
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
        
        // Initialize staff note manager
        this.staffNoteManager = new StaffNoteManager(this);

        // Initialize auto-close task if enabled
        if (this.configManager.isAutoCloseEnabled()) {
            this.autoCloseTask = new AutoCloseTask(this);
            
            // Run task every hour (20 ticks * 60 seconds * 60 minutes = 72000 ticks)
            autoCloseTask.runTaskTimer(this, 72000, 72000);
            
            getLogger().info("Auto-close system enabled: inactive tickets will close after " + 
                            configManager.getAutoCloseTime() + " hours");
        }
        
        // Initialize the blacklist manager
        this.blacklistManager = new BlacklistManager(this);
        
        // Initialize the report manager
        this.reportManager = new ReportManager(this);
        
        getLogger().info("GKTickets è stato avviato con successo!");
    }

    @Override
    public void onDisable() {
        // Chiusura della connessione al database
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        // Cancel auto-close task if it exists
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        
        getLogger().info("GKTickets è stato disabilitato.");
    }
    
    /**
     * Ricarica solo il file di configurazione senza richiamare altri metodi
     * Questo metodo è utilizzato da ConfigManager.reloadConfig()
     */
    public void reloadConfigFile() {
        // Call only the super method without additional logic
        super.reloadConfig();
    }
    
    /**
     * Ricarica la configurazione del plugin
     */
    @Override
    public void reloadConfig() {
        // First reload the base config file
        super.reloadConfig();
        
        // Now reload each component
        if (configManager != null) {
            // We use getConfig() which gets the already reloaded config
            // rather than triggering another reloadConfig cycle
            configManager.loadConfig(); // Use loadConfig instead of reloadConfig
        }
        
        // Ricarica i messaggi
        if (messageManager != null) {
            messageManager.loadMessages();
        }
        
        // Ricarica le categorie
        if (categoryManager != null) {
            categoryManager.reloadCategories();
        }
        
        getLogger().info("Configurazione ricaricata con successo.");
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
    
    public CategoryManager getCategoryManager() {
        return categoryManager;
    }
    
    /**
     * Get the staff note manager
     * @return The staff note manager
     */
    public StaffNoteManager getStaffNoteManager() {
        return staffNoteManager;
    }
    
    /**
     * Get the blacklist manager
     * @return The blacklist manager
     */
    public BlacklistManager getBlacklistManager() {
        return blacklistManager;
    }
    
    /**
     * Get the report manager
     * @return The report manager
     */
    public ReportManager getReportManager() {
        return reportManager;
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