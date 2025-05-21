package it.Gecky.gktickets.utils;

import it.Gecky.gktickets.GKTickets;
import org.bukkit.Bukkit;

public class VersionUtils {
    private final GKTickets plugin;

    public VersionUtils(GKTickets plugin) {
        this.plugin = plugin;
    }

    /**
     * Restituisce la versione del server come stringa.
     * @return Stringa contenente la versione del server
     */
    public String getServerVersionString() {
        return Bukkit.getVersion();
    }

    /**
     * Verifica se la versione del server è uguale o successiva a quella specificata.
     * @param major Versione maggiore (es. 1)
     * @param minor Versione minore (es. 13)
     * @return true se il server è alla versione specificata o successiva
     */
    public boolean isVersionOrHigher(int major, int minor) {
        String[] versionParts = Bukkit.getBukkitVersion().split("\\.");
        int serverMajor = Integer.parseInt(versionParts[0]);
        int serverMinor = Integer.parseInt(versionParts[1]);

        return (serverMajor > major) || (serverMajor == major && serverMinor >= minor);
    }
}