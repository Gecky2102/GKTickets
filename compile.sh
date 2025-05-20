#!/bin/bash

# Rendi lo script eseguibile automaticamente
chmod +x "$0"

echo "=== Compilazione GKTickets ==="

# Elimina i vecchi file compilati
echo "Eliminazione delle vecchie build..."
rm -rf target/

# Compila con Maven
echo "Compilazione con Maven..."
mvn clean package

# Verifica se la compilazione ha avuto successo
if [ $? -eq 0 ]; then
    echo "=== Compilazione completata con successo! ==="
    echo "Il file JAR è disponibile in: target/GKTickets-*.jar"
else
    echo "=== Errore durante la compilazione! ==="
    exit 1
fi
