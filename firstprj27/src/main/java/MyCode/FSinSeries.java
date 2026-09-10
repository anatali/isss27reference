package MyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import unibo.basicomm23.utils.CommUtils;

public class FSinSeries {
    private static boolean stopped = false;
    private static List<String> labels = new ArrayList<>();
    private static List<String> values = new ArrayList<>();
    
    private static double Min;
    private static double Max;
    private static double dx;
    private static double curX;
   
    public static void stop() {
    	stopped = true;
    }
    public static void goon() {
    	stopped = false;
    } 
    
    public static void setParams(double MinVal, double MaxVal, double delta ) {
    	Min = MinVal;
    	Max = MaxVal;
    	dx  = delta;
    	labels = new ArrayList<>();
    	values = new ArrayList<>();
    	curX = Min;
    }
    
    public static void evalNextPoint() {
    	if( curX > Max ) {
    		CommUtils.outred("Max exceeded");
    		return;
    	}
    	double y = FSin.eval(curX);
        labels.add(String.format(Locale.US, "%.1f", curX));
        values.add(String.format(Locale.US, "%.3f", y));    
        curX = curX + dx;
    }
    
    public static String getEvaluedPoints() {
    	  // 1. Conversione delle due liste in un'unica stringa
        String listaAStringa = String.join(",", labels);
        String listaBStringa = String.join(",", values);
        String stringaDaInviare = listaAStringa + "###" + listaBStringa;
        CommUtils.outcyan("Serie di punti valutata" + stringaDaInviare);
        return stringaDaInviare;
    	
    }
    
    /*
     * Valutazione non interrompibile
     */
    
    public static String evalSinPoints(double Min, double Max, double dx ) {
    	 System.out.println("evalSinPoints Min=" + Min + " Max=" + Max + " Dx=" + dx);
     for (double x = Min; x <= Max; x += dx) {
          double y = FSin.eval(x);
          labels.add(String.format(Locale.US, "%.1f", x));
          values.add(String.format(Locale.US, "%.3f", y));
     }
     
  // 1. Conversione delle due liste in un'unica stringa
     String listaAStringa = String.join(",", labels);
     String listaBStringa = String.join(",", values);
     String stringaDaInviare = listaAStringa + "###" + listaBStringa;
     CommUtils.outcyan("Serie di punti valutata" + stringaDaInviare);
     return stringaDaInviare;
  }
    
    

}

 