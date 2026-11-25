package domain;

import static org.junit.Assert.*;

import java.net.URI;
import org.junit.Test;

/**
 * R3 · Test dei tipi di dominio. Devono essere VERDI già nell'analisi dei requisiti:
 * verificano che il glossario tradotto in tipi rispetti le decisioni D3, D7, D10.
 */
public class LogisticRequestTest {

    @Test public void validRequestIsAccepted() {
        LogisticRequest r = new LogisticRequest(2.0, 0.7, 8);
        assertEquals(8, r.n());
    }

    @Test public void boundsAreInclusive() {             // D3: intervalli chiusi
        new LogisticRequest(0.0, 0.0, 0);
        new LogisticRequest(4.0, 1.0, LogisticRequest.N_MAX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeRIsRejected()   { new LogisticRequest(-2.0, 0.7, 8); }

    @Test(expected = IllegalArgumentException.class)
    public void x0AboveOneIsRejected()  { new LogisticRequest(2.0, 1.5, 8); }

    @Test(expected = IllegalArgumentException.class)
    public void nAboveNmaxIsRejected()  { new LogisticRequest(2.0, 0.7, LogisticRequest.N_MAX + 1); }

    @Test(expected = IllegalArgumentException.class)
    public void nanIsRejected()         { new LogisticRequest(Double.NaN, 0.7, 8); }

    @Test public void notNumericTextIsInvalid() {          // scenario "parametri non numerici"
        assertFalse(LogisticRequest.isValid("abc", "0.7", "8"));
        assertFalse(LogisticRequest.isValid("NaN", "0.7", "8"));
        assertTrue (LogisticRequest.isValid("2.0", "0.7", "8"));
    }

    @Test public void trajectoryHasNPlusOneValues() {      // D7
        LogisticRequest r = new LogisticRequest(2.0, 0.7, 2);
        new Trajectory(r, new double[]{0.7, 0.42, 0.4872});
        try {
            new Trajectory(r, new double[]{0.42, 0.4872});
            fail("una traiettoria con N valori deve essere rifiutata");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void outcomeIsEitherDoneOrFailed() {      // D6, D10
        Outcome ok = new Done(URI.create("http://host:8333/mapservice/1"));
        Outcome ko = new Failed(Cause.WRONG_PARAMS);
        assertTrue(ok instanceof Done);
        assertEquals("wrongparams", ((Failed) ko).cause().qakName());
    }
}
