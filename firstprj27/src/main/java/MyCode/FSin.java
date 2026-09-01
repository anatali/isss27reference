package MyCode;

import unibo.basicomm23.utils.CommUtils;

public class FSin {
	
	public static String evalStr(String xs) {
		//CommUtils.outmagenta( "eval | xs=" + xs);
		double x = Double.parseDouble(xs);
		//CommUtils.outmagenta( "eval | x=" + x);
		return ""+eval(x);
	}
	
	public static double eval(double x) {
//		if (x > 4.0) {
//			CommUtils.outmagenta( "eval | Simulo ritardo per x=" + x);
//			CommUtils.delay(8000);
//		}
		double sinValue = Math.sin(x)  ;
		// Arrotonda a 2 cifre decimali
		return Math.round(sinValue * 100.0) / 100.0;
	} 

 
}
