package MyCode;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ChartUtils {

	   public static String splitResultString(String stringaRicevuta) {
		   System.out.println("stringaRicevuta " + stringaRicevuta);
			// 2. Ricostruzione delle liste sul client ricevente
			   String[] parti = stringaRicevuta.split("###", -1); // -1 gestisce eventuali liste vuote

			   ArrayList<String> labels = new ArrayList<>(Arrays.asList(parti[0].split(",")));
			   ArrayList<String> values = new ArrayList<>(Arrays.asList(parti[1].split(",")));	  
			   
			   System.out.println("labels " + labels + " na=" + labels.size());
			   System.out.println("values " + values + " nb=" + values.size());
			   
			   return buildMapChartUrl(labels,values);
		   }

	   
    public static String buildMapChartUrl( ArrayList<String> labels, ArrayList<String> values ) {
        // Costruzione della struttura JSON minima per Chart.js / QuickChart
       String jsonConfig = String.format(Locale.US,
           "{" +
           "  type: 'line'," +
           "  data: {" +
           "    labels: [%s]," +
           "    datasets: [{" +
           "      label: 'Funzione sin'," +
           "      data: [%s]," +
           "      fill: false," +
           "      borderColor: 'rgb(30, 144, 255)'," + // Colore della linea e del box in legenda
           //"      backgroundColor: 'green'," +          // Colore punti e legenda            
           "      borderWidth: 1.5," +                  // Linea più sottile (default ~3-4)
           "      pointBackgroundColor: 'green'," +
           "      pointBorderColor: 'red'," +
           "      pointRadius: 1" + // Pallini più piccoli (usa 0 per nasconderli)
           "    }]" +
           "	},"+
			"  options: {" +
			    "    plugins: {" +
			    "      legend: { labels: { color: 'red' } }" + //Colore del testo della legenda
			    "    }," +
			    "    scales: {" +
			    "      x: { ticks: { color: 'green' } }," +  // Colore delle etichette sull'asse X
			    "      y: { ticks: { color: 'green' } }" +   // Colore delle etichette sull'asse Y
			    "    }" +
			    "  }" +   
			    "}",
           String.join(",", labels),
           String.join(",", values)
       );
        // Encoding dell'URL per gestire correttamente i caratteri speciali del JSON
       String encodedConfig = URLEncoder.encode(jsonConfig, StandardCharsets.UTF_8);
       return "https://quickchart.io/chart?v=3&c=" + encodedConfig;
   }

   public static void OpenChartInBrowser( String chartUrl ) {
       try {
           // Verifica se il sistema supporta l'apertura del browser
           if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
               System.out.println("Apertura del grafico nel browser in corso...");
               Desktop.getDesktop().browse(new URI(chartUrl));
           } else {
               System.out.println("Apertura automatica non supportata. Apri manualmente questo URL:\n" + chartUrl);
           }

       } catch (Exception e) {
           e.printStackTrace();
       }
   }

}
