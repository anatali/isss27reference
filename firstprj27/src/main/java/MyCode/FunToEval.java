package MyCode;

import unibo.basicomm23.utils.CommUtils;

public class FunToEval {
	
	public static double getValue(String xs) {
		double x = Double.parseDouble(xs);
		return getValue( x );
	}
	
	public static double getValue(double x) {
		double v = Math.sin(x)+Math.cos(x);
		// Arrotonda a 3 cifre decimali
		return Math.round(v * 1000.0) / 1000.0;
	}

}
