package logisticmap;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import unibo.basicomm23.utils.CommUtils;

public class LogisticMapTestClosedFormGemini {

    // Tolleranza per il confronto tra numeri in virgola mobile (double)
    private static final double EPSILON = 1e-9;
    private static final double R_CHAOTIC = 4.0;

    /**
     * Calcola la simulazione passo-passo: x_{n+1} = r * x_n * (1 - x_n)
     */
    public double stepByStepSimulation(double x0, double r, int n) {
        double x = x0;
        for (int i = 0; i < n; i++) {
            x = r * x * (1.0 - x);
        }
        return x;
    }

    /**
     * Calcola la forma chiusa per r = 4.0: x_n = sin^2(2^n * arcsin(sqrt(x0)))
     */
    public double closedFormR4(double x0, int n) {
        if (x0 < 0.0 || x0 > 1.0) {
            throw new IllegalArgumentException("x0 deve essere compreso nell'intervallo [0, 1]");
        }
        double theta0 = Math.asin(Math.sqrt(x0));
        double thetaN = Math.pow(2.0, n) * theta0;
        double sinVal = Math.sin(thetaN);
        return sinVal * sinVal;
    }

    @Test
    public void testSimulationMatchesClosedFormForMultiplePoints() {
    	/*
    	 * Non essendoci @ParameterizedTest nativo semplice come in JUnit 5 
    	 * senza runner dedicati (Parameterized), si itera su un array di initialConditions 
    	 * all'interno di un unico metodo test.
    	 */
        double[] initialConditions = {0.1, 0.25, 0.33, 0.6, 0.85};
        int steps = 10;

        for (double x0 : initialConditions) {
            double expectedClosedForm = closedFormR4(x0, steps);
            double actualSimulation = stepByStepSimulation(x0, R_CHAOTIC, steps);

            CommUtils.outcyan("expectedClosedForm:" + expectedClosedForm + " actualSimulation" + actualSimulation);
            
            Assert.assertEquals(
                "Divergenza trovata per x0 = " + x0 + " dopo " + steps + " passi",
                expectedClosedForm,
                actualSimulation,
                EPSILON
            );
        }
    }

    @Test
    public void testCentralSingularity() {
        double x0 = 0.5;
        int steps = 5;

        // Per x0 = 0.5, x1 = 1.0 e x_n = 0.0 per n >= 2
        double expected = 0.0;
        double actualSimulation = stepByStepSimulation(x0, R_CHAOTIC, steps);
        double actualClosedForm = closedFormR4(x0, steps);

        Assert.assertEquals("La simulazione deve azzerarsi", expected, actualSimulation, EPSILON);
        Assert.assertEquals("La forma chiusa deve azzerarsi", expected, actualClosedForm, EPSILON);
    }

    @Test
    public void testFixedPointR4() {
        double x0 = 0.75; // 3/4
        int steps = 20;

        double expected = 0.75;
        double actualSimulation = stepByStepSimulation(x0, R_CHAOTIC, steps);
        double actualClosedForm = closedFormR4(x0, steps);

        Assert.assertEquals("Il punto fisso deve rimanere 0.75 nella simulazione", expected, actualSimulation, EPSILON);
        Assert.assertEquals("Il punto fisso deve rimanere 0.75 nella forma chiusa", expected, actualClosedForm, EPSILON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInitialConditionNegative() {
        closedFormR4(-0.1, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInitialConditionGreaterThanOne() {
        closedFormR4(1.2, 5);
    }
}