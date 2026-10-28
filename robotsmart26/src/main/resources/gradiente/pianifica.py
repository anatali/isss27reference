# Pseudo-codice Python per pianificazione cammino
def pianifica_percorso(griglia_T, start=(0,0), ostacoli=[]):
    percorso = [start]
    corrente = start
    
    while True:
        print("cella corrente:", corrente)
        x, y = corrente
        vicini = [(x+1, y), (x-1, y), (x, y+1), (x, y-1)]
        
        # Filtra celle fuori griglia o con ostacoli
        validi = [
            p for p in vicini 
            if 0 <= p[0] < 5 and 0 <= p[1] < 7 and p not in ostacoli
        ]
        
        if not validi:
            print("Nessuna strada disponibile. Arresto per ostacolo/bordo.")
            break
            
        # Scegli la cella con la temperatura massima (gradiente)
        prossima = max(validi, key=lambda p: griglia_T[p[0]][p[1]])
        
        # Condizione di arresto: se la temperatura non aumenta più
        if griglia_T[prossima[0]][prossima[1]] <= griglia_T[x][y]:
            print("Raggiunto il massimo locale di temperatura.")
            break
            
        percorso.append(prossima)
        print("Percorso:", percorso)
        corrente = prossima
         
    return percorso

from collections import deque

def pianifica_percorso_alternativo(griglia_T, start=(0,0), ostacoli=[]):
    fil = len(griglia_T)
    col = len(griglia_T[0])
    
    # 1. Esplora tutte le celle raggiungibili da 'start' evitando gli ostacoli (BFS)
    queue = deque([start])
    visited = {start}
    parent = {start: None}
    
    while queue:
        current = queue.popleft()
        x, y = current
        
        # Movimenti consentiti (su, giù, sinistra, destra)
        vicini = [(x+1, y), (x-1, y), (x, y+1), (x, y-1)]
        
        for nx, ny in vicini:
            next_cell = (nx, ny)
            # Verifica limiti della griglia e assenza di ostacoli/visite precedenti
            if 0 <= nx < fil and 0 <= ny < col and next_cell not in ostacoli and next_cell not in visited:
                visited.add(next_cell)
                parent[next_cell] = current
                queue.append(next_cell)
                
    if not visited:
        return [] # Nessuna cella raggiungibile

    # 2. Trova la cella con la temperatura MASSIMA tra tutte quelle RAGGIUNGIBILI
    migliore_cella = max(visited, key=lambda c: griglia_T[c[0]][c[1]])
    
    # 3. Ricostruisci il percorso a ritroso partendo dalla migliore cella fino a 'start'
    percorso = []
    curr = migliore_cella
    while curr is not None:
        percorso.append(curr)
        curr = parent[curr]
    percorso.reverse() # Invertiamo per avere l'ordine da start a destinazione
    
    return percorso

# --- Esempio di utilizzo con ostacolo in (2,3) ---
griglia_T = [
    [28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32],
    [29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31],
    [30.22, 30.98, 31.94, 33.21, 34.90, 37.24, 40.66],
    [29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31],
    [28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32]
]

ostacoli = [(2, 3)] # Ostacolo posizionato sulla traiettoria centrale

percorso_sicuro = pianifica_percorso_alternativo(griglia_T, start=(0,0), ostacoli=ostacoli)
print("Percorso calcolato:", percorso_sicuro)


# Definizione della matrice di temperatura e dell'ostacolo
griglia_T = [
    [28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32],
    [29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31],
    [30.22, 30.98, 31.94, 33.21, 34.90, 37.24, 40.66], #QUI STUFA
    [29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31],
    [28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32]
]

punti_ostacolo = [(1,2), (1,3), (2,4),(3,2),(3,3)]  # Esempio di ostacoli nella griglia

if __name__ == "__main__":
    print("pianifica_percorso: Esempio di utilizzo")
    #percorso_calcolato = pianifica_percorso( griglia_T )
    #percorso_calcolato = pianifica_percorso( griglia_T,  start=(0, 0), ostacoli=punti_ostacolo)
    percorso_calcolato = pianifica_percorso_alternativo( griglia_T,  start=(0, 0), ostacoli=punti_ostacolo)
    print("Percorso pianificato:", percorso_calcolato)
		