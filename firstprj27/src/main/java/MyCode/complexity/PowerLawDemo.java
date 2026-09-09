package MyCode.complexity;

public class PowerLawDemo {

    public static void main(String[] args) {
        // Parametri della Power Law: y = a * (x ^ k)
        double a = 100.0;     // Costante di scala
        double k = -1.5;      // Esponente tipico di una legge a "coda lunga" (es. distribuzione di Pareto)

        int numPunti = 10;
        double xIniziale = 1.0;
        double xFinale = 1000.0;

        // Calcoliamo il fattore di incremento moltiplicativo per distribuire i punti su scala logaritmica
        double fattoreMoltiplicativo = Math.pow(xFinale / xIniziale, 1.0 / (numPunti - 1));

        System.out.println("==========================================================================");
        System.out.println("                   DEMO ANDAMENTO FUNZIONE POWER LAW                      ");
        System.out.println("                         Equazione: y = " + a + " * x^(" + k + ")");
        System.out.println("==========================================================================");
        System.out.printf("%-12s %-15s %-15s %-15s%n", "x (Lineare)", "y (Lineare)", "log10(x)", "log10(y)");
        System.out.println("--------------------------------------------------------------------------");

        double x = xIniziale;
        for (int i = 0; i < numPunti; i++) {
            // Calcolo del valore y = a * x^k
            double y = a * Math.pow(x, k);

            // Trasformazione logaritmica: log10(y) = log10(a) + k * log10(x)
            double logX = Math.log10(x);
            double logY = Math.log10(y);

            System.out.printf("%-12.2f %-15.6f %-15.4f %-15.4f%n", x, y, logX, logY);

            // Incrementiamo x in modo moltiplicativo
            x *= fattoreMoltiplicativo;
        }

        System.out.println("==========================================================================");
        System.out.println("NOTA: Nota come log10(y) decresca in modo perfettamente lineare rispetto a log10(x).");
    }
}


/*
==========================================================================
                   DEMO ANDAMENTO FUNZIONE POWER LAW                      
                         Equazione: y = 100.0 * x^(-1.5)
==========================================================================
x (Lineare)  y (Lineare)     log10(x)        log10(y)       
--------------------------------------------------------------------------
1,00         100,000000      0,0000          2,0000         
2,15         31,622777       0,3333          1,5000         
4,64         10,000000       0,6667          1,0000         
10,00        3,162278        1,0000          0,5000         
21,54        1,000000        1,3333          -0,0000        
46,42        0,316228        1,6667          -0,5000        
100,00       0,100000        2,0000          -1,0000        
215,44       0,031623        2,3333          -1,5000        
464,16       0,010000        2,6667          -2,0000        
1000,00      0,003162        3,0000          -2,5000        
==========================================================================
NOTA: Nota come log10(y) decresca in modo perfettamente lineare rispetto a log10(x).
*/
