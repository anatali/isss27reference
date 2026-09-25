package MyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import unibo.basicomm23.utils.CommUtils;

public class LogisticSeries {
    private static double r  = 3.8;          //
    private static double x1 = 0.1;          // 10% della capacità iniziale
    private static int anniTotali = 50;     // Estendiamo a 50 anni per vedere bene l'estabilizzazione dell'onda

    public static void setParameters(double r_p, double x1_p, int anni_p ) {
    	r = r_p;
    	x1 = x1_p;
    	anniTotali = anni_p;
    }
    
    public static String eval( ) {
    	double xold = x1;
    	double x1   = r * xold * (1.0 - xold);
    	return xold+"###"+x1;
    }
    
    public static String evalPoints(  ) {
    	return evalPoints(r,x1,anniTotali);
    }
    
    public static String evalAllPoints( ) {
    	return evalPoints(r,x1,anniTotali);
    }
    
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
    	String s   = LogisticSeries.evalPoints( );
    	String url = ChartUtils.buildMapChartUrl("Mappa logistica r=" +r, s);
    	ChartUtils.OpenChartInBrowser(url);
    }

}

 