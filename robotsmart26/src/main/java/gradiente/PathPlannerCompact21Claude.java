package gradiente;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cli.System.Collections.ArrayList;

public class PathPlannerCompact21Claude {

	record Point(int x, int y) {}

	/*
	 * new ArrayList<Point>();  NON ESISTE in Java17  
	 * 
	 * 
	static List<Point> pianificaPercorsoAlternativoJava21(double[][] t, Point start, Set<Point> ostacoli) {
	    int righe = t.length, colonne = t[0].length;
	    var queue   = new ArrayDeque<>(List.of(start));
	    var visited = new HashSet<>(Set.of(start));
	    var parent  = new HashMap<Point, Point>();
	    parent.put(start, null);
	    while (!queue.isEmpty()) {
	        var c = queue.poll();
	        var vicini = List.of(new Point(c.x()+1, c.y()), new Point(c.x()-1, c.y()),
	                             new Point(c.x(), c.y()+1), new Point(c.x(), c.y()-1));
	        for (var n : vicini) {
	            if (0 <= n.x() && n.x() < righe && 0 <= n.y() && n.y() < colonne
	                && !ostacoli.contains(n) && visited.add(n)) {
	                parent.put(n, c);
	                queue.add(n);
	            }
	        }
	    }
	    var migliore = Collections.max(visited, Comparator.comparingDouble(p -> t[p.x()][p.y()]));
	    var percorso = new ArrayList<Point>();
	    for (var p = migliore; p != null; p = parent.get(p)) percorso.add(p);
	    Collections.reverse(percorso);
	    return percorso;
	}
	*/
}
