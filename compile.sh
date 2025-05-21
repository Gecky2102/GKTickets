#!/bin/bash

# Rendi lo script eseguibile automaticamente
chmod +x "$0"

echo "=== Compilazione GKTickets ==="

# Controllo esistenza di Maven
if ! command -v mvn &> /dev/null; then
    echo "Maven non trovato. Assicurati che sia installato e nel PATH."
    exit 1
fi

# Verifica la presenza di file Java
if [ ! -d "/workspaces/GKTickets/src/main/java/it/Gecky/gktickets" ]; then
    echo "ERRORE: Directory dei sorgenti Java non trovata!"
    exit 1
fi

# Elimina i vecchi file compilati
echo "Eliminazione delle vecchie build..."
rm -rf target/

# Compila con Maven
echo "Compilazione con Maven..."
mvn clean package

# Verifica se la compilazione ha avuto successo
if [ $? -eq 0 ]; then
    echo "=== Compilazione completata con successo! ==="
    
    # Trova il file JAR e mostra il percorso
    jar_file=$(find target/ -name "GKTickets-*.jar" | head -n 1)
    
    if [ -n "$jar_file" ]; then
        echo "Il file JAR è disponibile in: $jar_file"
        echo "Dimensione: $(du -h "$jar_file" | cut -f1)"
    else
        echo "AVVERTIMENTO: Non è stato trovato alcun file JAR in target/"
    fi
    
    # Verifica se è stato compilato con avvertimenti
    if grep -q "WARNING" target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst 2>/dev/null; then
        echo "La compilazione è avvenuta con avvertimenti. Controlla i log per i dettagli."
    fi
else
    echo "=== Errore durante la compilazione! ==="
    echo "Per maggiori dettagli, esegui: mvn clean package -X"
    exit 1
fi
