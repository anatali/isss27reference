package MyCode;

import unibo.basicomm23.utils.CommUtils;

public class Function0 {
	
	public static double eval(double x) {
//		if (x > 4.0) {
//			CommUtils.outmagenta( "eval | Simulo ritardo per x=" + x);
//			CommUtils.delay(8000);
//		}
		return Math.sin(x) + Math.cos( Math.sqrt(3)*x);
	} 

}
