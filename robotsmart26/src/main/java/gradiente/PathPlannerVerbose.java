package gradiente;

import java.util.*;

public class PathPlannerVerbose {

    // Classe di supporto per rappresentare le coordinate (x, y) della cella
    public static class Point {
        public int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * Pianifica il percorso alternativo usando una ricerca in ampiezza (BFS) 
     * per esplorare le celle libere e puntare a quella con temperatura massima.
     */
    public static List<Point> pianificaPercorsoAlternativo(double[][] grigliaT, Point start, Set<Point> ostacoli) {
        int righe = grigliaT.length;
        int colonne = grigliaT[0].length;

        Queue<Point> queue = new LinkedList<>();
        Set<Point> visited = new HashSet<>();
        Map<Point, Point> parent = new HashMap<>();

        // Inizializzazione della BFS
        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        // Movimenti consentiti: su, giù, sinistra, destra
        int[][] direzioni = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            for (int[] dir : direzioni) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                Point nextCell = new Point(nx, ny);

                // Verifica limiti della griglia, assenza di ostacoli e celle non ancora visitate
                if (nx >= 0 && nx < righe && ny >= 0 && ny < colonne 
                        && !ostacoli.contains(nextCell) 
                        && !visited.contains(nextCell)) {
                    visited.add(nextCell);
                    parent.put(nextCell, current);
                    queue.add(nextCell);
                }
            }
        }

        if (visited.isEmpty()) {
            return Collections.emptyList(); // Nessuna cella raggiungibile
        }

        // 2. Trova la cella con la temperatura MASSIMA tra tutte quelle RAGGIUNGIBILI
        Point miglioreCella = null;
        double maxTemp = -Double.MAX_VALUE;

        for (Point p : visited) {
            double temp = grigliaT[p.x][p.y];
            if (temp > maxTemp) {
                maxTemp = temp;
                miglioreCella = p;
            }
        }

        // 3. Ricostruisci il percorso a ritroso dalla cella migliore fino a 'start'
        List<Point> percorso = new ArrayList<>();
        Point curr = miglioreCella;
        while (curr != null) {
            percorso.add(curr);
            curr = parent.get(curr);
        }
        Collections.reverse(percorso); // Inverte per avere l'ordine da start a destinazione

        return percorso;
    }

    // Metodo di test / Esempio di esecuzione
    public static void main(String[] args) {
        // Matrice di temperatura 5x7
        double[][] grigliaT = {
            {28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32},
            {29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31},
            {30.22, 30.98, 31.94, 33.21, 34.90, 37.24, 40.66},
            {29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31},
            {28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32}
        };

        // Definizione degli ostacoli (es. cella 2,3 bloccata)
        Set<Point> ostacoli = new HashSet<>();
//        (1,2), (1,3), (2,4),(3,2),(3,3)
        ostacoli.add(new Point(1, 2));
        ostacoli.add(new Point(1, 3));
        ostacoli.add(new Point(2, 4));
        ostacoli.add(new Point(3, 2));
        ostacoli.add(new Point(3, 3));
 
        

        // Punto di partenza del robot DDR
        Point start = new Point(0, 0);

        // Calcolo del percorso
        List<Point> percorso = pianificaPercorsoAlternativo(grigliaT, start, ostacoli);

        // Stampa dei risultati
        System.out.println("--- Percorso Calcolato (con aggiramento ostacolo) ---");
        for (Point p : percorso) {
            System.out.println("Cella " + p + " -> Temperatura: " + grigliaT[p.x][p.y] + " °C");
        }
    }
}
