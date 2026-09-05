package MyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import unibo.basicomm23.utils.CommUtils;

public class LogisticSeries {
    private double r = 3.2;          // Provoca l'oscillazione stabile
    private double x = 0.1;          // 10% della capacità iniziale
    private int anniTotali = 50;     // Estendiamo a 50 anni per vedere bene l'estabilizzazione dell'onda

 
    public static String evalPoints(double r, double x, int anni ) {
    	 System.out.println("evalSinPoints r=" + r + " x=" + x + " anni=" + anni);
    	List<String> labels = new ArrayList<>();
    	List<String> values = new ArrayList<>();
     
     for (int t = 1; t <= anni; t++) {
         x = r * x * (1.0 - x);
         labels.add(String.format(Locale.US, "%d", t));
         values.add(String.format(Locale.US, "%.3f", x));
     }

     
  // 1. Conversione delle due liste in un'unica stringa
     String listaAStringa = String.join(",", labels);
     String listaBStringa = String.join(",", values);
     String stringaDaInviare = listaAStringa + "###" + listaBStringa;
     CommUtils.outcyan("Serie di punti valutata" + stringaDaInviare);
     return stringaDaInviare;
  }
    
    public static void main (String args[] ) {
    	String s   = LogisticSeries.evalPoints(3.2, 0.1, 30);
    	String url = ChartUtils.buildMapChartUrl(s);
    	ChartUtils.OpenChartInBrowser(url);
    }

}

 