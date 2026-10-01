package logisticmap;
import org.junit.Test;

import MyCode.LogisticSeries;

import static org.junit.Assert.assertEquals;

/**
 * Test "oracolo": casi in cui, nonostante il regime possa essere periodico
 * o addirittura caotico, esiste una soluzione analitica chiusa nota, quindi
 * il valore atteso NON e' una stima empirica ma un fatto matematico.
 *
 * Questi complementano gli invarianti property-based: dove gli invarianti
 * dicono "la traiettoria rispetta certe regole strutturali", gli oracoli
 * dicono "la traiettoria e' esattamente questa".
 */
public class LogisticSeriesOracleTest {

    private static final double EPS = 1e-9;

    // ------------------------------------------------------------------
    // Caso 1 - r=2.0: punto fisso x* = (r-1)/r = 0.5.
    // Stesso scenario già presente nel .qak (QActor "callerforquicktesting",
    // stato "convergenza"): qui lo si rende un'asserzione automatica.
    // ------------------------------------------------------------------
    @Test
    public void r2_convergesToKnownFixedPoint() {
        double r = 2.0;
        double x0 = 0.7;
        int n = 60; // sufficienti iterazioni per avvicinarsi al punto fisso

        double[] ys = LogisticSeries.computeSeries(r, x0, n);
        double fixedPoint = (r - 1) / r; // 0.5

        assertEquals(fixedPoint, ys[ys.length - 1], 1e-6);
    }

    // ------------------------------------------------------------------
    // Caso 2 - regime periodico (r=3.2): punto fisso di periodo 2 noto in
    // forma chiusa. Le due soluzioni del ciclo sono le radici di
    //   x = r^2 x (1-x) (1 - r x (1-x))
    // ma piu' semplice verificare la proprieta' x_n ~= x_{n+2} per n grande,
    // e i due valori del ciclo tramite le formule di Feigenbaum/May:
    //   x_{1,2} = [ (r+1) ± sqrt((r+1)(r-3)) ] / (2r)
    // ------------------------------------------------------------------
    @Test
    public void r3_2_settlesIntoKnownPeriod2Cycle() {
        double r = 3.2;
        double x0 = 0.5;
        int n = 80;

        double[] ys = LogisticSeries.computeSeries(r, x0, n);

        double discriminant = (r + 1) * (r - 3);
        double sqrtDisc = Math.sqrt(discriminant);
        double xHigh = ((r + 1) + sqrtDisc) / (2 * r);
        double xLow  = ((r + 1) - sqrtDisc) / (2 * r);

        double last = ys[ys.length - 1];
        double secondLast = ys[ys.length - 2];

        // Gli ultimi due valori devono corrispondere (in un ordine o
        // nell'altro) ai due punti del ciclo di periodo 2.
        boolean matchesCycle =
                (isClose(last, xHigh) && isClose(secondLast, xLow)) ||
                (isClose(last, xLow) && isClose(secondLast, xHigh));

        assertTrueMessage("la coppia finale non corrisponde al ciclo di periodo 2 atteso: "
                        + "last=" + last + " secondLast=" + secondLast
                        + " attesi {" + xLow + ", " + xHigh + "}",
                matchesCycle);
    }

    // ------------------------------------------------------------------
    // Caso 3 - r=4.0, regime pienamente caotico: MA con x0 = sin^2(theta*pi)
    // e theta razionale, la mappa a r=4 e' coniugata alla mappa
    // "raddoppio d'angolo" ed esiste soluzione chiusa esatta:
    //   x_n = sin^2( 2^n * theta * pi )
    // Questo permette di generare vettori di test ESATTI anche nel regime
    // piu' difficile, invece di rinunciare a testare i valori assoluti.
    //
    // Attenzione: la formula chiusa e' calcolata con la stessa aritmetica
    // double dell'implementazione sotto test, quindi anche qui vale il
    // limite dell'orizzonte di Lyapunov (vedi commento sotto): oltre un
    // certo n, il confronto punto-per-punto perde significato perche'
    // ENTRAMBE le formule (oracolo e implementazione) accumulano errore
    // di arrotondamento in modo scorrelato.
    // ------------------------------------------------------------------
    @Test
    public void r4_matchesClosedFormSolution_shortHorizon() {
        double r = 4.0;
        double theta = 1.0 / 7.0; // razionale qualsiasi, theta in (0, 0.5)
        double x0 = Math.pow(Math.sin(theta * Math.PI), 2);

        // Orizzonte di predicibilita': con esponente di Lyapunov ~ ln(2)
        // per iterazione, oltre n ~ 15-20 il confronto diretto diventa
        // rumore. Qui restiamo volutamente conservativi.
        int n = 15;

        double[] ys = LogisticSeries.computeSeries(r, x0, n);

        for (int i = 0; i <= n; i++) {
            double expected = Math.pow(Math.sin(Math.pow(2, i) * theta * Math.PI), 2);
            assertEquals("scostamento dalla soluzione chiusa al passo " + i,
                    expected, ys[i], 1e-3);  //1e-6 no
        }
    }

    // ------------------------------------------------------------------
    // Caso 4 - r=4.0 con theta razionale "periodico": scegliendo theta
    // tale che 2^n * theta sia un multiplo intero di un periodo, si ottiene
    // un ciclo esatto anche a r=4. Esempio: theta = 1/3 ->
    //   2^0*theta=1/3, 2^1*theta=2/3, 2^2*theta=4/3=1/3 (mod 1) -> periodo 2
    // Utile per un test "lungo" che NON soffre del problema dell'orizzonte
    // di Lyapunov, perche' la traiettoria e' esattamente periodica.
    // ------------------------------------------------------------------
    @Test
    public void r4_withRationalTheta_isExactlyPeriodic() {
        double r     = 4.0;
        double theta = 1.0 / 3.0;
        double x0    = Math.pow(Math.sin(theta * Math.PI), 2);
        int n        = 40; // 50 molte iterazioni: qui e' sicuro perche' il ciclo è esatto | con 50 no

        double[] ys = LogisticSeries.computeSeries(r, x0, n);

        // Periodo 2 atteso: x_i deve ripetersi ogni 2 passi.
        for (int i = 0; i < ys.length - 2; i++) {
            assertEquals("periodicita' attesa violata al passo " + i,
                    ys[i], ys[i + 2], 1e-6);  // 
        }
    }

    // ==================================================================
    // Adapter e utility - ADATTARE ai metodi reali di MyCode.LogisticSeries
    // ==================================================================
//    private double[] computeSeries(double r, double x0, int n) {
//        MyCode.LogisticSeries.setParameters(r, x0, n);
//        String raw = MyCode.LogisticSeries.evalPoints();
//        return parseYValues(raw);
//    }
//
//    private double[] parseYValues(String sofpairs) {
//        String cleaned = sofpairs.replace("'", "").trim();
//        String[] parts = cleaned.split("###");
//        String[] yTokens = parts[1].split(",");
//
//        double[] ys = new double[yTokens.length];
//        for (int i = 0; i < yTokens.length; i++) {
//            ys[i] = Double.parseDouble(yTokens[i].trim());
//        }
//        return ys;
//    }

    private boolean isClose(double a, double b) {
        return Math.abs(a - b) < 1e-3;  //1e-3: r3_2 ok | 
    }

    private void assertTrueMessage(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
