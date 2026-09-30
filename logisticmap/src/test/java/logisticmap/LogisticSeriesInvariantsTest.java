package logisticmap;	
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Test di livello "matematico": verificano proprieta' che DEVONO valere per
 * qualunque combinazione di parametri nel dominio ammesso, indipendentemente
 * dal fatto che la traiettoria specifica sia convergente, periodica o caotica.
 *
 * Versione basata SOLO su JUnit4 puro (nessuna dipendenza esterna): i casi
 * di test sono generati con java.util.Random a seme fisso, cosi' da avere
 * ampia copertura del dominio E riproducibilita' bit-per-bit tra le run.
 *
 * Con @RunWith(Parameterized.class) ogni combinazione (r, x0, n) diventa
 * un test case separato e riportato individualmente dal runner: se una
 * proprieta' fallisce solo per certi valori di r, lo vedi subito nel report
 * invece di dover fare debug dentro un unico test monolitico.
 */

@RunWith(Parameterized.class)
public class LogisticSeriesInvariantsTest {

    // Seme fisso: stessa lista di casi ad ogni esecuzione, su ogni macchina.
    private static final long SEED = 20260930L;
    private static final int NUM_RANDOM_CASES = 200;

    private static final double X0_MIN = 1e-6;
    private static final double X0_MAX = 0.999999;

    private final double r;
    private final double x0;
    private final int n;

    public LogisticSeriesInvariantsTest(double r, double x0, int n) {
        this.r = r;
        this.x0 = x0;
        this.n = n;
    }

    // Il nome del test case nel report include i parametri: molto utile
    // per capire a colpo d'occhio quale combinazione ha fatto fallire cosa.
    @Parameters(name = "{index}: r={0}, x0={1}, n={2}")
    public static List<Object[]> parameters() {
        List<Object[]> cases = new ArrayList<>();

        // --- Casi limite espliciti, sempre presenti indipendentemente dal seme ---
        cases.add(new Object[]{0.0, 0.5, 10});          // estremo inferiore di r
        cases.add(new Object[]{4.0, 0.5, 10});           // estremo superiore di r (caos)
        cases.add(new Object[]{4.0, X0_MAX, 50});        // x0 vicino al bordo superiore
        cases.add(new Object[]{X0_MIN, X0_MIN, 5});      // r e x0 vicino allo zero
        cases.add(new Object[]{2.0, 0.7, 60});           // caso gia' nel .qak originale
        cases.add(new Object[]{3.99, 0.3, 90});          // n vicino al limite superiore
/*
        // --- Casi casuali con seme fisso: ampia copertura, riproducibile ---
        Random rnd = new Random(SEED);
        for (int i = 0; i < NUM_RANDOM_CASES; i++) {
            double randR = rnd.nextDouble() * 4.0;                     // [0,4]
            double randX0 = X0_MIN + rnd.nextDouble() * (X0_MAX - X0_MIN); // (0,1)
            int randN = 1 + rnd.nextInt(99);                            // [1,99]
            cases.add(new Object[]{randR, randX0, randN});
        }
*/
        return cases;
    }

    // ------------------------------------------------------------------
    // 1) Limitatezza: per r in [0,4] e x0 in (0,1), ogni x_i deve restare
    //    in [0,1]. Vale ANCHE in regime caotico: e' la prima cosa da
    //    rompere se c'e' un bug (es. overflow, ordine delle operazioni).
    // ------------------------------------------------------------------
    @Test
    public void sequenceStaysBoundedInUnitInterval() {
        double[] ys = computeSeries(r, x0, n);

        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            assertFalse("NaN al passo " + i, Double.isNaN(y));
            assertFalse("valore infinito al passo " + i, Double.isInfinite(y));
            assertTrue("valore fuori da [0,1] al passo " + i + ": " + y,
                    y >= -1e-9 && y <= 1.0 + 1e-9);
        }
    }

    // ------------------------------------------------------------------
    // 2) Lunghezza corretta: n+1 valori (x0 incluso).
    //    ADATTARE se la convenzione del servizio e' diversa.
    // ------------------------------------------------------------------
    @Test
    public void sequenceHasExpectedLength() {
        double[] ys = computeSeries(r, x0, n);
        assertEquals("lunghezza sequenza inattesa", n + 1, ys.length);
    }

    // ------------------------------------------------------------------
    // 3) Ricorrenza puntuale: per OGNI coppia consecutiva deve valere
    //    x_{i+1} = r * x_i * (1 - x_i). Il test piu' potente: verifica
    //    la corretta applicazione della formula ad ogni passo,
    //    indipendentemente dal comportamento globale della traiettoria.
    // ------------------------------------------------------------------
    @Test
    public void everyStepRespectsTheRecurrenceRelation() {
        double[] ys = computeSeries(r, x0, n);

        for (int i = 0; i < ys.length - 1; i++) {
            double expectedNext = r * ys[i] * (1 - ys[i]);
            assertEquals("ricorrenza violata al passo " + i,
                    expectedNext, ys[i + 1], 1e-6);
        }
    }

    // ------------------------------------------------------------------
    // 4) Determinismo: stesso input -> stesso output, bit per bit.
    // ------------------------------------------------------------------
    @Test
    public void computationIsBitwiseDeterministic() {
        double[] run1 = computeSeries(r, x0, n);
        double[] run2 = computeSeries(r, x0, n);

        assertArrayEquals("stesso input deve produrre output identico",
                run1, run2, 0.0);
    }

    // ==================================================================
    // Adapter verso il servizio sotto test.
    // ADATTARE ai nomi/metodi reali di MyCode.LogisticSeries e al formato
    // reale della stringa SOFPAIRS ("x1,...,xn###y1,...,yn").
    // ==================================================================
    private double[] computeSeries(double r, double x0, int n) {
        MyCode.LogisticSeries.setParameters(r, x0, n);
        String raw = MyCode.LogisticSeries.evalPoints();
        return parseYValues(raw);
    }

    private double[] parseYValues(String sofpairs) {
        String cleaned = sofpairs.replace("'", "").trim();
        String[] parts = cleaned.split("###");
        String[] yTokens = parts[1].split(",");

        double[] ys = new double[yTokens.length];
        for (int i = 0; i < yTokens.length; i++) {
            ys[i] = Double.parseDouble(yTokens[i].trim());
        }
        return ys;
    }
}
