# GKTickets

Un plugin per Minecraft (1.21.4) che implementa un sistema di ticket tramite comandi, senza GUI, utilizzando SQLite per la memorizzazione dei dati.

## 📋 Caratteristiche

- Sistema di ticket completamente basato su comandi
- Database SQLite per la memorizzazione persistente
- Notifiche interattive per lo staff e i giocatori
- Supporto per PlaceholderAPI
- Messaggi e configurazioni altamente personalizzabili
- Sistema di suoni per le notifiche
- Permessi granulari per diverse funzionalità

## 🔧 Requisiti

- Server Minecraft 1.21.4 (Spigot/Paper)
- Java 17 o superiore
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (opzionale, per le placeholders)

## 🚀 Installazione

1. Scarica il file `.jar` dalla sezione release
2. Posiziona il file nella cartella `plugins` del tuo server
3. Riavvia il server o carica il plugin con un plugin manager
4. I file di configurazione verranno generati automaticamente

## 📝 Comandi

| Comando | Descrizione | Permesso |
|---------|-------------|----------|
| `/ticket create <descrizione>` | Crea un nuovo ticket | gktickets.create |
| `/ticket list` | Visualizza tutti i ticket aperti | gktickets.view |
| `/ticket info <id>` | Mostra i dettagli di un ticket | gktickets.view |
| `/ticket close <id>` | Chiude un ticket specifico | gktickets.staff |
| `/ticket reply <id> <messaggio>` | Risponde a un ticket | gktickets.view |

## 🛡️ Permessi

| Permesso | Descrizione | Default |
|----------|-------------|---------|
| gktickets.create | Permette di creare ticket | true |
| gktickets.view | Permette di visualizzare i propri ticket | true |
| gktickets.staff | Permette di gestire tutti i ticket | op |

## ⚙️ Configurazione

Il plugin genera un file `config.yml` con tutte le impostazioni personalizzabili:

```yaml
# GKTickets - Configurazione

# Impostazioni del database
database:
  # Tipo di database (supportato: sqlite)
  type: sqlite
  # Nome del file del database
  name: tickets

# Impostazioni dei ticket
tickets:
  # Impostazioni per la chiusura automatica dei ticket
  auto-close:
    # Abilita la chiusura automatica dei ticket inattivi
    enabled: false
    # Tempo in ore dopo il quale un ticket inattivo viene chiuso automaticamente
    time: 72

# Impostazioni delle notifiche
notifications:
  # Notifiche per lo staff
  staff:
    # Notifica lo staff quando viene creato un nuovo ticket
    new-ticket: true
    # Notifica lo staff quando viene aggiunta una risposta ad un ticket
    new-reply: true

  # Notifiche per i giocatori
  player:
    # Notifica al giocatore quando viene aggiunta una risposta al suo ticket
    new-reply: true
    # Notifica al giocatore quando il suo ticket viene chiuso
    ticket-closed: true

  # Impostazioni dei suoni
  sound:
    # Abilita i suoni per le notifiche
    enabled: true
    # Suono per i nuovi ticket
    new-ticket: entity.experience_orb.pickup
    # Suono per le nuove risposte
    new-reply: entity.player.levelup

# Permessi
permissions:
  # Impostazione di gestione ticket 
  # Se true, i giocatori possono visualizzare e rispondere solo ai propri ticket
  # Se false, i giocatori possono visualizzare e rispondere a qualsiasi ticket con il permesso appropriato
  player-only-own-tickets: true
```

## 💬 Messaggi Personalizzabili

Il plugin utilizza un file `messages.yml` che consente di personalizzare tutti i messaggi:

- Supporto completo per i colori di Minecraft usando `&` come prefisso
- Supporto per PlaceholderAPI
- Placeholders specifiche di GKTickets come `{id}`, `{player}`, `{description}`, ecc.

## 📋 Integrazione con PlaceholderAPI

GKTickets si integra con PlaceholderAPI, permettendoti di usare qualsiasi placeholder nei messaggi. Se PlaceholderAPI è installato, verrà rilevato automaticamente.

## 🔄 Compilazione

Per compilare il plugin da sorgente:

1. Clona il repository
2. Esegui `./compile.sh` o usa Maven direttamente con `mvn clean package`
3. Il file JAR compilato sarà disponibile nella cartella `target`

## 📄 Licenza

GKTickets è rilasciato sotto licenza MIT. Sei libero di utilizzare, modificare e distribuire questo plugin secondo i termini di tale licenza.

## 👥 Supporto

Se hai bisogno di aiuto con il plugin, puoi aprire un issue su GitHub.