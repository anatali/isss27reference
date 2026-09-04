package MyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import unibo.basicomm23.utils.CommUtils;

public class FSinSeries {
     
 
    public static String evalSinPoints(double Min, double Max, double dx ) {
    	 System.out.println("evalSinPoints Min=" + Min + " Max=" + Max + " Dx=" + dx);
    	List<String> labels = new ArrayList<>();
    	List<String> values = new ArrayList<>();
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

 