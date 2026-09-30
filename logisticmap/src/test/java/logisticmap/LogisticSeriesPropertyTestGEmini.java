package logisticmap;
 

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import MyCode.LogisticSeries;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class LogisticSeriesPropertyTestGEmini {

    // Parametri di input per ciascun test
    private final double r;
    private final double x0;
    private final int n;

    private LogisticSeries logisticSeries;

    // Costruttore: JUnit4 inietta i parametri forniti da @Parameters in ogni istanza
    public LogisticSeriesPropertyTestGEmini(double r, double x0, int n) {
        this.r = r;
        this.x0 = x0;
        this.n = n;
    }

    // Definizione dei dati di test (Parametri che coprono vari regimi: convergenti, periodici, caotici e casi limite)
    @Parameters(name = "Test {index}: r={0}, x0={1}, n={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // Caso limite: x0 = 0
            { 3.5, 0.0, 10 },
            // Caso limite: r = 0
            { 0.0, 0.5, 10 },
            // Regime Convergente (r < 3.0)
            { 2.5, 0.2, 20 },
            // Regime Periodico (r = 3.2 -> ciclo di periodo 2)
            { 3.2, 0.4, 30 },
            // Regime Caotico (r = 3.8, come nei valori di default della classe)
            { 3.8, 0.1, 50 },
            // Regime Caotico ai limiti del dominio (r = 4.0)
            { 4.0, 0.7, 100 }
        });
    }

    @Before
    public void setUp() {
        logisticSeries = new LogisticSeries();
    }

    /**
     * Proprietà 1: Lunghezza corretta della sequenza.
     * La funzione valuta x0 e poi n passi successivi, per un totale di n + 1 elementi.
     */
    @Test
    public void testCorrectSequenceLength() {
        double[] result = logisticSeries.computeSeries(r, x0, n);
        assertNotNull("L'array di output non deve essere null", result);
        assertEquals("La lunghezza della sequenza deve essere n + 1", n + 1, result.length);
    }

    /**
     * Proprietà 2: Limitatezza dei valori nell'intervallo [0, 1].
     * Indipendentemente da r e x0 nel dominio ammesso, x_t non deve mai uscire da [0, 1].
     */
    @Test
    public void testBoundednessOfValues() {
        double[] result = logisticSeries.computeSeries(r, x0, n);
        for (int i = 0; i < result.length; i++) {
            assertTrue("Il valore al passo " + i + " (" + result[i] + ") deve essere >= 0.0", result[i] >= 0.0);
            assertTrue("Il valore al passo " + i + " (" + result[i] + ") deve essere <= 1.0", result[i] <= 1.0);
        }
    }

    /**
     * Proprietà 3: Casi limite e stabilità dei punti fissi.
     * Se x0 == 0.0, tutti i valori della serie devono essere 0.0.
     */
    @Test
    public void testEdgeCaseZeroX0() {
        if (Double.compare(x0, 0.0) == 0) {
            double[] result = logisticSeries.computeSeries(r, x0, n);
            for (double val : result) {
                assertEquals("Se x0 e' 0, tutti i punti devono rimanere 0.0", 0.0, val, 1e-3);
            }
        }
    }

    /**
     * Proprietà 4: Ricorrenza puntuale della Mappa Logistica.
     * Verifica la coerenza della relazione x_{t+1} = r * x_t * (1 - x_t).
     * Nota: La classe converte internamente i double formattandoli a 3 cifre decimali ("%.3f").
     * Si usa una tolleranza adeguata (delta = 1e-2).
     */
    @Test
    public void testPunctualRecurrenceRelation() {
        double[] result = logisticSeries.computeSeries(r, x0, n);
        for (int i = 0; i < result.length - 1; i++) {
            double currentX = result[i];
            double expectedNextX = r * currentX * (1.0 - currentX);
            double actualNextX = result[i + 1];
            
            assertEquals("La relazione di ricorrenza fallisce al passo " + (i + 1),
                         expectedNextX, actualNextX, 1e-2);
        }
    }

    /**
     * Proprietà 5: Determinismo.
     * Esecuzioni successive con la medesima configurazione devono generare output identici.
     */
    @Test
    public void testDeterminism() {
        double[] run1 = logisticSeries.computeSeries(r, x0, n);
        double[] run2 = logisticSeries.computeSeries(r, x0, n);

        assertArrayEquals("Esecuzioni identiche devono produrre gli stessi valori", run1, run2, 1e-6);
    }
}
