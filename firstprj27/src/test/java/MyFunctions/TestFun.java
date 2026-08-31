package MyFunctions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import MyFunctions.FSin;
import unibo.basicomm23.utils.CommUtils;

public class TestFun {  

	private double h( double x ) {
		//Funzione equivalente a add_sin_cos PERIODICA di 2PI
		double val = Math.sqrt(2) * Math.sin( x + Math.PI/4);
		return Math.round( val * 1000.0) / 1000.0;
	}
	
	private EvalFun evaluator ;
	
	@Before
	public void setup() {
		evaluator = new EvalFun();
		CommUtils.outgreen("TestFun  | setup ");			
 	}
 
	@After
	public void down() {
 		CommUtils.outgreen("TestFun | down");
	}
 	
	@Test   
	public void test_add_sin_cos() {
		CommUtils.outblue("test_add_sin_cos(PI/4)");
		
		double v = evaluator.add_sin_cos(Math.PI/4);
		double vh = h( Math.PI/4 );
		assertTrue( v == vh );
	} 

	@Test   
	public void test_period() {
		CommUtils.outblue("test_period");
		
		double v1 = evaluator.add_sin_cos(Math.PI/2);
		CommUtils.outblue("test_period v1="+v1);
		double v2 = evaluator.add_sin_cos(Math.PI/2 + Math.PI*2);
		CommUtils.outblue("test_period v2="+v2);
		assertTrue( v1 == v2 );
	} 

}
