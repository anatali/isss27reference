package MyCode;

public class EvalFun {
	
	public double sin(double x) {
		double sinValue = Math.sin(x)  ;
		// Arrotonda a 2 cifre decimali
		return Math.round(sinValue * 100.0) / 100.0;
	} 
	
	public double add_sin_cos(double x) {
		double v = Math.sin(x)+Math.cos(x);
		// Arrotonda a 3 cifre decimali
		return Math.round(v * 1000.0) / 1000.0;
	}
}
