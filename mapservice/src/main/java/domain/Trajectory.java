package domain;

import java.util.Arrays;

/** R3 · Traiettoria x_0 ... x_n: esattamente n+1 valori (D7). */
public record Trajectory(LogisticRequest req, double[] values) {

    public Trajectory {
        if (req == null || values == null) throw new IllegalArgumentException("req e values obbligatori");
        if (values.length != req.n() + 1)
            throw new IllegalArgumentException("attesi " + (req.n() + 1) + " valori, trovati " + values.length);
        values = values.clone();                     // il record resta immutabile
    }

    @Override public double[] values() { return values.clone(); }

    @Override public boolean equals(Object o) {
        return o instanceof Trajectory t && req.equals(t.req) && Arrays.equals(values, t.values);
    }
    @Override public int hashCode() { return 31 * req.hashCode() + Arrays.hashCode(values); }
    @Override public String toString() { return "Trajectory[" + req + ", " + values.length + " valori]"; }
}
