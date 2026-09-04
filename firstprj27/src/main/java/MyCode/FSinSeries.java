package MyCode;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import unibo.basicomm23.utils.CommUtils;

public class FSinSeries {
	
	//  double r = 3.2;          // Provoca l'oscillazione stabile
	//	double r = 3.8;          // Provoca andamento caotico
	//	double r = 1;          //exp
	//	double r = 1.5;          //si stabilizza a (r-1)/r per 1<r<2
	private static double r = 2.9;          //oscilla e poi si stabilizza a (r-1)/r per 2<r<3
	private static double x = 0.1;          // 10% della capacità iniziale
	private static int anniTotali = 50;     // Estendiamo a 50 anni per vedere bene l'estabilizzazione dell'onda

	private static List<String> labels = new ArrayList<>();
	private static List<String> values = new ArrayList<>();

	public static void setParam_r(String vs) {
		double v = Double.parseDouble(vs);
		r = v;
		CommUtils.outmagenta("r now is:" + r);
	}
	
    public static String buildMapChartUrl( double r, double x, int anniTotali ) {
 
    	evalSinPoints(-3.0,  3.0,  0.1);
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

//        "borderColor: rgb(30, 144, 255)" +  // Colore blu
//        "backgroundColor: rgb(30, 144, 255)" +  // Colore della legenda e dei punti
//        "borderWidth: 1.5" +                  // Linea più sottile (default ~3-4)
//        "pointRadius: 2 "  +                  // Pallini più piccoli (usa 0 per nasconderli)            "    }]" +

        // Encoding dell'URL per gestire correttamente i caratteri speciali del JSON
        String encodedConfig = URLEncoder.encode(jsonConfig, StandardCharsets.UTF_8);
        return "https://quickchart.io/chart?v=3&c=" + encodedConfig;
    }
    
 
    public static String evalSinPoints(double Min, double Max, double dx ) {
     for (double x = Min; x <= Max; x += dx) {
          double y = FSin.eval(x);
          labels.add(String.format(Locale.US, "%.1f", x));
          values.add(String.format(Locale.US, "%.3f", y));
      }
     
  // 1. Conversione delle due liste in un'unica stringa
     String listaAStringa = String.join(",", labels);
     String listaBStringa = String.join(",", values);
     String stringaDaInviare = listaAStringa + "###" + listaBStringa;
     System.out.println("Serie di punti valutata" + stringaDaInviare);
     return stringaDaInviare;
  }

   public static void xxx(String stringaRicevuta) {
	// 2. Ricostruzione delle liste sul client ricevente
	   String[] parti = stringaRicevuta.split("###", -1); // -1 gestisce eventuali liste vuote

	   ArrayList<String> aRicevuto = new ArrayList<>(Arrays.asList(parti[0].split(",")));
	   ArrayList<String> bRicevuto = new ArrayList<>(Arrays.asList(parti[1].split(",")));	  
	   
	   System.out.println("aRicevuto " + aRicevuto + " na=" + aRicevuto.size());
	   System.out.println("bRicevuto " + bRicevuto + " nb=" + bRicevuto.size());
   }

    /**
     * Effettua la chiamata HTTP a QuickChart e scarica direttamente l'immagine PNG in byte.
     */
    public static byte[] fetchChartImage(String chartUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chartUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Errore HTTP da QuickChart: " + response.statusCode());
        }
    }
    
    public static void OpenChartInBrowser( int nyears) {
    	anniTotali = nyears;
    	OpenChartInBrowser();
    }
    
    public static void OpenChartInBrowser( ) {
        try {
            // Genera l'URL usando la funzione vista in precedenza
            //String chartUrl = QuickChartLogistic.buildChartUrl(1.0, 0.8, 0.0);
            
            String chartUrl = FSinSeries.buildMapChartUrl(r, x, anniTotali );

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

    // Esempio di utilizzo
    public static void main(String[] args) {
 
        	//OpenChartInBrowser();
    	String s = evalSinPoints(-3,3,0.5);
    	xxx(s);
 
    }
}

 