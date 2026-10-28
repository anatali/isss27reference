package gradiente;

import java.util.*;

/**
 * Pianificazione del percorso verso la cella più calda raggiungibile (BFS).
 * Versione Java 17: record, var, collezioni immutabili, Collections.max.
 */
public class PathPlanner17Claude {

    /** Cella della griglia: record = valore immutabile con equals/hashCode/toString generati. */
    public record Point(int x, int y) {

        /** Le 4 celle adiacenti (su, giù, sinistra, destra). */
        List<Point> vicini() {
            return List.of(new Point(x + 1, y), new Point(x - 1, y),
                           new Point(x, y + 1), new Point(x, y - 1));
        }

        /** true se la cella cade dentro una griglia righe x colonne. */
        boolean dentro(int righe, int colonne) {
            return 0 <= x && x < righe && 0 <= y && y < colonne;
        }

        @Override
        public String toString() { return "(" + x + ", " + y + ")"; }
    }

/**
	 * Pianifica il percorso alternativo usando una ricerca in ampiezza (BFS) 
	 * per esplorare le celle libere e puntare a quella con temperatura massima.
	 */    
    public static List<Point> pianificaPercorsoAlternativo(double[][] grigliaT, Point start, Set<Point> ostacoli) {
        int righe = grigliaT.length, colonne = grigliaT[0].length;

        // 1. BFS: esplora tutte le celle raggiungibili da start evitando gli ostacoli
        var queue   = new ArrayDeque<Point>(List.of(start));
        var visited = new HashSet<Point>(Set.of(start));
        var parent  = new HashMap<Point, Point>();
        parent.put(start, null);                      // Map.of non accetta null: serve put

        while (!queue.isEmpty()) {
            var current = queue.poll();
            for (var next : current.vicini()) {
                if (next.dentro(righe, colonne) && !ostacoli.contains(next) && !visited.contains(next)) {
                    visited.add(next);
                    parent.put(next, current);
                    queue.add(next);
                }
            }
        }

        // 2. Cella con temperatura massima tra quelle raggiungibili
        var migliore = Collections.max(visited,
                Comparator.comparingDouble((Point p) -> grigliaT[p.x()][p.y()]));

        // 3. Ricostruzione a ritroso del percorso da migliore fino a start
        var percorso = new ArrayList<Point>();
        for (var p = migliore; p != null; p = parent.get(p)) percorso.add(p);
        Collections.reverse(percorso);
        return percorso;
    }

    public static void main(String[] args) {
        double[][] grigliaT = {
            {28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32},
            {29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31},
            {30.22, 30.98, 31.94, 33.21, 34.90, 37.24, 40.66},
            {29.87, 30.58, 31.48, 32.65, 34.20, 36.32, 39.31},
            {28.87, 29.47, 30.22, 31.18, 32.42, 34.07, 36.32}
        };
        var ostacoli = Set.of(new Point(1, 2), new Point(1, 3), new Point(2, 4),
                              new Point(3, 2), new Point(3, 3));

        var percorso = pianificaPercorsoAlternativo(grigliaT, new Point(0, 0), ostacoli);

        System.out.println("--- Percorso Calcolato (con aggiramento ostacolo) ---");
        percorso.forEach(p -> System.out.println("Cella " + p + " -> Temperatura: " + grigliaT[p.x()][p.y()] + " °C"));
    }
}
