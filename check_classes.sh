#!/bin/bash

echo "=== Verifica Corrispondenza Classi e Nomi File ==="

# Trova tutti i file Java
find /workspaces/GKTickets/src/main/java/it/Gecky/gktickets -type f -name "*.java" | while read -r file; do
    filename=$(basename "$file")
    classname="${filename%.java}"
    
    # Controlla se il file contiene la classe corretta (con possibili modificatori come final, abstract)
    if ! grep -q "public \(final \|abstract \)*class $classname" "$file"; then
        echo "ERRORE: Il file $file non contiene la classe pubblica $classname"
        echo "Contenuto del file:"
        head -20 "$file"
        echo "..."
    else
        echo "OK: $file contiene correttamente la classe $classname"
    fi
    
    # Verifica il package declaration
    package_path=$(echo "$file" | sed -e 's|/workspaces/GKTickets/src/main/java/||' -e 's|/[^/]*$||' -e 's|/|.|g')
    if ! grep -q "package $package_path;" "$file"; then
        echo "AVVERTIMENTO: Il file $file ha un package declaration che non corrisponde al percorso"
        grep "package" "$file"
        echo "Dovrebbe essere: package $package_path;"
    fi
done

echo "=== Verifica Completata ==="
