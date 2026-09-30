package logisticmap;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test di livello "matematico": verificano proprietà che DEVONO valere per
 * qualunque combinazione di parametri nel dominio ammesso, indipendentemente
 * dal fatto che la traiettoria specifica sia convergente, periodica o caotica.
 *
 * Non testano MAI un valore finale atteso: testano invarianti strutturali,
 * l'unica cosa verificabile in modo esaustivo su tutto il dominio di r.
 *
 * Dipendenze (Maven):
 *   <dependency>
 *     <groupId>com.pholser</groupId>
 *     <artifactId>junit-quickcheck-core</artifactId>
 *     <version>1.0</version>
 *     <scope>test</scope>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.pholser</groupId>
 *     <artifactId>junit-quickcheck-generators</artifactId>
 *     <version>1.0</version>
 *     <scope>test</scope>
 *   </dependency>
 *
 * Gradle:
 *   testImplementation "com.pholser:junit-quickcheck-core:1.0"
 *   testImplementation "com.pholser:junit-quickcheck-generators:1.0"
 */
@RunWith(JUnitQuickcheck.class)
public class LogisticSeriesInvariantsTest {

    // Tolleranza numerica per confronti in virgola mobile.
    private static final double EPS = 1e-9;

    // Limite inferiore per x0: il dominio richiede x0 > 0.0 in senso stretto;
    // 0.0 esatto e valori denormalizzati non sono interessanti da testare.
    private static final double X0_MIN = 1e-6;
    private static final double X0_MAX = 0.999999;

    // ------------------------------------------------------------------
    // 1) Limitatezza: per r in [0,4] e x0 in (0,1), ogni x_i deve restare
    //    in [0,1]. E' una proprietà nota della mappa logistica e vale
    //    ANCHE in regime caotico: e' la prima cosa da rompere se c'e' un
    //    bug nell'implementazione (es. overflow, ordine delle operazioni).
    // ------------------------------------------------------------------
    @Property(trials = 500)
    public void sequenceStaysBoundedInUnitInterval(
            @InRange(minDouble = 0.0, maxDouble = 4.0) double r,
            @InRange(minDouble = X0_MIN, maxDouble = X0_MAX) double x0,
            @InRange(minInt = 1, maxInt = 99) int n) {

        double[] ys = computeSeries(r, x0, n);

        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            assertFalse("NaN al passo " + i + " (r=" + r + ", x0=" + x0 + ")",
                    Double.isNaN(y));
            assertFalse("valore infinito al passo " + i + " (r=" + r + ", x0=" + x0 + ")",
                    Double.isInfinite(y));
            assertTrue("valore fuori da [0,1] al passo " + i + ": " + y
                            + " (r=" + r + ", x0=" + x0 + ")",
                    y >= -EPS && y <= 1.0 + EPS);
        }
    }

    // ------------------------------------------------------------------
    // 2) Lunghezza corretta: la sequenza deve contenere esattamente n+1
    //    valori (x0 incluso). ADATTARE se la convenzione del servizio e'
    //    diversa (es. n valori esclusi x0).
    // ------------------------------------------------------------------
    @Property(trials = 500)
    public void sequenceHasExpectedLength(
            @InRange(minDouble = 0.0, maxDouble = 4.0) double r,
            @InRange(minDouble = X0_MIN, maxDouble = X0_MAX) double x0,
            @InRange(minInt = 1, maxInt = 99) int n) {

        double[] ys = computeSeries(r, x0, n);
        assertEquals("lunghezza sequenza inattesa", n + 1, ys.length);
    }

    // ------------------------------------------------------------------
    // 3) Ricorrenza puntuale: per OGNI coppia consecutiva deve valere
    //    x_{i+1} = r * x_i * (1 - x_i).
    //    Questo e' il test piu' potente che si possa scrivere: verifica
    //    che l'implementazione applichi correttamente la formula ad ogni
    //    singolo passo, indipendentemente dal comportamento globale della
    //    traiettoria (convergente, periodica o caotica che sia).
    // ------------------------------------------------------------------
    @Property(trials = 500)
    public void everyStepRespectsTheRecurrenceRelation(
            @InRange(minDouble = 0.0, maxDouble = 4.0) double r,
            @InRange(minDouble = X0_MIN, maxDouble = X0_MAX) double x0,
            @InRange(minInt = 2, maxInt = 99) int n) {

        double[] ys = computeSeries(r, x0, n);

        for (int i = 0; i < ys.length - 1; i++) {
            double expectedNext = r * ys[i] * (1 - ys[i]);
            assertEquals("ricorrenza violata al passo " + i
                            + " (r=" + r + ", x0=" + x0 + ")",
                    expectedNext, ys[i + 1], 1e-6);
        }
    }

    // ------------------------------------------------------------------
    // 4) Determinismo: stesso input -> stesso output, bit per bit.
    //    Essenziale per un sistema con virgola mobile: se questa proprieta'
    //    fallisse, ogni altro test diventerebbe inaffidabile (il caos
    //    amplifica anche differenze dovute a non-determinismo interno,
    //    es. riordino di operazioni float in thread diversi).
    // ------------------------------------------------------------------
    @Property(trials = 200)
    public void computationIsBitwiseDeterministic(
            @InRange(minDouble = 0.0, maxDouble = 4.0) double r,
            @InRange(minDouble = X0_MIN, maxDouble = X0_MAX) double x0,
            @InRange(minInt = 1, maxInt = 99) int n) {

        double[] run1 = computeSeries(r, x0, n);
        double[] run2 = computeSeries(r, x0, n);

        assertArrayEquals("stesso input deve produrre output identico",
                run1, run2, 0.0);
    }

    // ------------------------------------------------------------------
    // 5) Caso limite noto: r = 0 collassa sempre a 0 dopo il primo passo,
    //    qualunque sia x0. Non e' property-based in senso stretto (r e'
    //    fissato), ma e' un ottimo controllo di sanita' da tenere qui
    //    accanto agli invarianti generali.
    // ------------------------------------------------------------------
    @Property(trials = 100)
    public void whenRIsZeroSequenceCollapsesToZero(
            @InRange(minDouble = X0_MIN, maxDouble = X0_MAX) double x0,
            @InRange(minInt = 1, maxInt = 99) int n) {

        double[] ys = computeSeries(0.0, x0, n);

        for (int i = 1; i < ys.length; i++) {
            assertEquals("con r=0 tutti i valori dopo x0 devono essere 0",
                    0.0, ys[i], EPS);
        }
    }

    // ==================================================================
    // Adapter verso il servizio sotto test.
    // ADATTARE ai nomi/metodi reali di MyCode.LogisticSeries e al formato
    // reale della stringa SOFPAIRS ("x1,...,xn###y1,...,yn").
    // ==================================================================
    private double[] computeSeries(double r, double x0, int n) {
        // Esempio se LogisticSeries è un oggetto/singleton Kotlin (chiamato da Java):
        //   MyCode.LogisticSeries.INSTANCE.setParameters(r, x0, n);
        //   String raw = MyCode.LogisticSeries.INSTANCE.evalPoints();
        //
        // Se invece è una classe instanziabile:
        //   LogisticSeries series = new LogisticSeries();
        //   series.setParameters(r, x0, n);
        //   String raw = series.evalPoints();

        MyCode.LogisticSeries.setParameters(r, x0, n);
        String raw = MyCode.LogisticSeries.evalPoints();

        return parseYValues(raw);
    }

    private double[] parseYValues(String sofpairs) {
        // Formato atteso (vedi commento Dispatch nel .qak):
        // "x1,x2,...,xn###y1,y2,...,yn"  (eventualmente con apici esterni)
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
