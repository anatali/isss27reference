package MyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import unibo.basicomm23.utils.CommUtils;

/**
 * Classe che implementa la simulazione della mappa logistica: x_{n+1} = r * x_n * (1 - x_n)
 * Permette di calcolare la serie di valori e analizzare il comportamento caotico.
 */
public class LogisticSeries {
    // Parametro di controllo della mappa logistica (valori tipici: 0-4)
    private static double r  = 3.8;
    // Condizione iniziale x0 (valore iniziale, rappresenta il 10% della capacità)
    private static double x1 = 0.1;
    // Numero totale di iterazioni da eseguire
    private static int anniTotali = 50;

    /**
     * Imposta i parametri della mappa logistica
     * @param r_p parametro di controllo
     * @param x1_p condizione iniziale
     * @param anni_p numero di iterazioni
     */
    public static void setParameters(double r_p, double x1_p, int anni_p ) {
    	r          = r_p;
    	x1         = x1_p;
    	anniTotali = anni_p;
    }
    /**
     * Calcola la serie completa di valori della mappa logistica
     * @param r parametro di controllo
     * @param x0 condizione iniziale
     * @param n numero di iterazioni
     * @return array di valori y calcolati
     */
    public static double[] computeSeries(double r, double x0, int n) {
        setParameters(r, x0, n);
        String raw = evalPoints();
        return parseYValues(raw);
    }

    /**
     * Estrae i valori Y da una stringa formattata "labels###values"
     * @param sofpairs stringa contenente coppie etichetta###valore
     * @return array di valori Y come double
     */
    public static double[] parseYValues(String sofpairs) {
        String cleaned = sofpairs.replace("'", "").trim();
        String[] parts = cleaned.split("###");
        String[] yTokens = parts[1].split(",");

        double[] ys = new double[yTokens.length];
        for (int i = 0; i < yTokens.length; i++) {
            ys[i] = Double.parseDouble(yTokens[i].trim());
        }
        return ys;
    }
    
    /**
     * Esegue un singolo passo della mappa logistica: x_1 = r * x_0 * (1 - x_0)
     * @return stringa formato "x_old###x_new"
     */
    public static String eval( ) {
    	double xold = x1;
    	double x1   = r * xold * (1.0 - xold);
    	return xold+"###"+x1;
    }
    
    /**
     * Valuta tutti i punti della serie usando i parametri statici attuali
     * @return stringa formato "labels###values"
     */
    public static String evalPoints(  ) {
    	return evalPoints(r,x1,anniTotali);
    }
    
    /**
     * Valuta tutti i punti della serie (alias di evalPoints)
     * @return stringa formato "labels###values"
     */
    public static String evalAllPoints( ) {
    	return evalPoints(r,x1,anniTotali);
    }
    
    /**
     * Esegue la simulazione passo-passo della mappa logistica per n iterazioni.
     * Computa: x_{i+1} = r * x_i * (1 - x_i)
     * @param x0 condizione iniziale
     * @param r parametro di controllo
     * @param n numero di iterazioni
     * @return valore finale dopo n iterazioni
     */
    public static double stepByStepSimulation(double x0, double r, int n) {
        double x = x0;
        for (int i = 0; i < n; i++) {
            x = r * x * (1.0 - x);
        }
        return x;
    }
    
    /**
     * Calcola la serie completa di punti della mappa logistica.
     * Genera etichette (indici temporali) e valori Y formattati a 3 decimali.
     * @param r parametro di controllo
     * @param x condizione iniziale
     * @param anni numero di iterazioni
     * @return stringa formato "t1,t2,...,tn###x1,x2,...,xn"
     */
    public static String evalPoints(double r, double x, int anni ) {
    	 System.out.println("evalSinPoints r=" + r + " x=" + x + " anni=" + anni);
    	 // Liste per immagazzinare le etichette (indici tempo) e i valori calcolati
    	List<String> labels = new ArrayList<>();
    	List<String> values = new ArrayList<>();
        // Aggiungi il primo valore (condizione iniziale al tempo t=0)
    	labels.add(String.format(Locale.US, "%d", 1));
        values.add(String.format(Locale.US, "%.3f", x));
     
     // Itera per 'anni' passi, calcolando il prossimo valore con la mappa logistica
     for (int t = 1; t <= anni; t++) {
         x = r * x * (1.0 - x);
         labels.add(String.format(Locale.US, "%d", t));
         values.add(String.format(Locale.US, "%.3f", x));
     }

     
  	 // Converti le liste in stringhe separate da virgola, unite dal separatore ###
     String listaAStringa = String.join(",", labels);
     String listaBStringa = String.join(",", values);
     String stringaDaInviare = listaAStringa + "###" + listaBStringa;
     CommUtils.outcyan("Serie di punti valutata" + stringaDaInviare);
     return stringaDaInviare;
  }
    
    
    /**
     * Calcola la forma chiusa della mappa logistica per r=4 (caso special caotico).
     * Formula: x_n = sin²(2^n * arcsin(√x_0))
     * Nota: Precisa per n ≤ 30; per n > 50 la perdita di precisione floating-point è significativa
     * 
     * @param x0 condizione iniziale (deve essere in [0, 1])
     * @param n numero di iterazioni
     * @return valore di x_n calcolato con formula chiusa
     * @throws IllegalArgumentException se x0 non è in [0, 1]
     */
    public static double computeClosedFormR4(double x0, int n) {
        if (x0 < 0.0 || x0 > 1.0) {
            throw new IllegalArgumentException("x0 deve essere compreso tra 0 e 1");
        }
        
        // Calcola l'angolo iniziale: θ_0 = arcsin(√x_0)
        double theta0 = Math.asin(Math.sqrt(x0));
        
        // Moltiplica l'angolo per 2^n: θ_n = 2^n * θ_0
        double factor = Math.pow(2.0, n);
        double thetaN = factor * theta0;
        
        // Ritorna sin²(θ_n) = sin(θ_n) * sin(θ_n)
        double sinVal = Math.sin(thetaN);
        return sinVal * sinVal;
    }
    
    /**
     * Metodo principale per testare la mappa logistica
     */
    public static void main (String args[] ) {
        // Esempi di utilizzo (attualmente commentati):
        // String s   = LogisticSeries.evalPoints( );
        // String url = ChartUtils.buildMapChartUrl("Mappa logistica r=" +r, s);
        // ChartUtils.OpenChartInBrowser(url);
        
        // Test della forma chiusa per r=4 con x0=0.5 e 10 iterazioni
    	System.out.println(""+computeClosedFormR4(0.5,10) );
    }

}

/*
Nota Critica: Limite per $n$ elevato e Perdita di PrecisioneSebbene la formula 
analitica sia matematicamente esatta per ogni $n$, nell'aritmetica floating-point (standard IEEE 754) 
subentra un fenomeno fisico-computazionale legato alla natura della mappa caotica:Crescita Esponenziale 
dell'Argomento: Il fattore $2^n$ raddoppia l'angolo ad ogni passo. 

Già per $n = 53$, $2^{53}$ supera la mantissa a 53 bit di un double.

Cancellazione Numerica: La funzione Math.sin(x) per $x$ molto grandi perde cifre significative 
a causa della riduzione dell'argomento modulo $2\pi$.In sintesi per gli Unit Test:Per $n \le 30$: 
Il metodo in forma chiusa è eccellente per verificare l'accuratezza passo-passo del simulatore 
tramite assertEquals(expected, actual, 1e-9).Per $n > 50$: 
La sensibilità alle condizioni iniziali (effetto farfalla) farà divergere l'iterazione numerica 
dalla formula calcolata in precisione finita.
*/