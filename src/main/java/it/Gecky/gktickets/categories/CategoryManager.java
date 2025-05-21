package it.Gecky.gktickets.categories;

import it.Gecky.gktickets.GKTickets;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategoryManager {

    private final GKTickets plugin;
    private final Map<String, TicketCategory> categories;
    private File categoriesFile;
    private FileConfiguration categoriesConfig;
    
    public CategoryManager(GKTickets plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        loadCategories();
    }
    
    /**
     * Carica le categorie dal file di configurazione
     */
    public void loadCategories() {
        categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
        
        // Crea il file se non esiste
        if (!categoriesFile.exists()) {
            plugin.saveResource("categories.yml", false);
        }
        
        categoriesConfig = YamlConfiguration.loadConfiguration(categoriesFile);
        
        // Verifica che ci siano tutte le categorie predefinite
        InputStream defaultStream = plugin.getResource("categories.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream));
            
            for (String key : defaultConfig.getKeys(true)) {
                if (!categoriesConfig.contains(key)) {
                    categoriesConfig.set(key, defaultConfig.get(key));
                }
            }
            
            try {
                categoriesConfig.save(categoriesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile salvare categories.yml: " + e.getMessage());
            }
        }
        
        // Pulisci le categorie esistenti
        categories.clear();
        
        // Carica le categorie dal file
        ConfigurationSection categoriesSection = categoriesConfig.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                String name = categoriesSection.getString(categoryId + ".name", categoryId);
                String description = categoriesSection.getString(categoryId + ".description", "");
                String color = categoriesSection.getString(categoryId + ".color", "&7");
                String icon = categoriesSection.getString(categoryId + ".icon", "");
                boolean defaultPermission = categoriesSection.getBoolean(categoryId + ".default_permission", true);
                
                TicketCategory category = new TicketCategory(categoryId, name, description, color, icon, defaultPermission);
                categories.put(categoryId.toLowerCase(), category);
                
                plugin.getLogger().info("Categoria caricata: " + categoryId);
            }
        }
        
        // Se non ci sono categorie, crea almeno quella predefinita
        if (categories.isEmpty()) {
            TicketCategory defaultCategory = new TicketCategory(
                "general",
                "Generale",
                "Categoria generale per tutti i ticket",
                "&7",
                "!",
                true
            );
            categories.put("general", defaultCategory);
            plugin.getLogger().warning("Nessuna categoria trovata, creata categoria generale predefinita.");
        }
    }
    
    /**
     * Ottiene una categoria dal suo ID
     * @param categoryId ID della categoria
     * @return La categoria o null se non esiste
     */
    public TicketCategory getCategory(String categoryId) {
        return categories.get(categoryId.toLowerCase());
    }
    
    /**
     * Ottiene la categoria predefinita
     * @return La prima categoria disponibile
     */
    public TicketCategory getDefaultCategory() {
        return categories.values().iterator().next();
    }
    
    /**
     * Verifica se esiste una categoria con l'ID specificato
     * @param categoryId ID della categoria
     * @return true se la categoria esiste
     */
    public boolean categoryExists(String categoryId) {
        return categories.containsKey(categoryId.toLowerCase());
    }
    
    /**
     * Ottiene tutte le categorie
     * @return Mappa di tutte le categorie
     */
    public Map<String, TicketCategory> getAllCategories() {
        return Collections.unmodifiableMap(categories);
    }
    
    /**
     * Ottiene la lista di ID di tutte le categorie
     * @return Lista degli ID delle categorie
     */
    public List<String> getCategoryIds() {
        return new ArrayList<>(categories.keySet());
    }
    
    /**
     * Ottiene la lista delle categorie disponibili per il giocatore in base ai permessi
     * @param player Giocatore da controllare
     * @return Lista degli ID delle categorie accessibili
     */
    public List<String> getAvailableCategories(Player player) {
        List<String> available = new ArrayList<>();
        
        for (Map.Entry<String, TicketCategory> entry : categories.entrySet()) {
            String categoryId = entry.getKey();
            TicketCategory category = entry.getValue();
            
            // Controlla se il giocatore ha il permesso o se la categoria è disponibile per tutti
            if (category.isDefaultPermission() || 
                player.hasPermission("gktickets.create." + categoryId) || 
                player.hasPermission("gktickets.admin")) {
                available.add(categoryId);
            }
        }
        
        return available;
    }
    
    /**
     * Verifica se il giocatore ha accesso alla categoria
     * @param player Giocatore da controllare
     * @param categoryId ID della categoria
     * @return true se il giocatore può utilizzare la categoria
     */
    public boolean canUseCategory(Player player, String categoryId) {
        TicketCategory category = getCategory(categoryId);
        if (category == null) return false;
        
        return category.isDefaultPermission() || 
               player.hasPermission("gktickets.create." + categoryId) || 
               player.hasPermission("gktickets.admin");
    }
    
    /**
     * Verifica se il giocatore può visualizzare ticket di questa categoria
     * @param player Giocatore da controllare
     * @param categoryId ID della categoria
     * @return true se il giocatore può visualizzare ticket di questa categoria
     */
    public boolean canViewCategory(Player player, String categoryId) {
        TicketCategory category = getCategory(categoryId);
        if (category == null) return false;
        
        return player.hasPermission("gktickets.view." + categoryId) || 
               player.hasPermission("gktickets.staff") || 
               player.hasPermission("gktickets.admin");
    }
}
