package MyCode;

import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import unibo.basicomm23.utils.CommUtils;

public class TestFSin {  

	@Before
	public void setup() {
		CommUtils.outgreen("TestFSin  | setup ");			
 	}
 
	@After
	public void down() {
 		CommUtils.outgreen("TestFSin | down");
	}
 	
	@Test   
	public void test1() {
		CommUtils.outblue("test1 sin(0)");
		double v = FSin.eval( 0 );
		assertTrue( v == 0.0 );
	} 
	@Test   
	public void test2() {
		CommUtils.outblue("test2 sin(pi/2)");
		double v = FSin.eval( Math.PI/2 );
		assertTrue( v == 1.0 );
	} 
	@Test   
	public void test3() {
		CommUtils.outblue("test3 sin(pi/4) ");
		double v = FSin.eval( Math.PI/ 4 );
		CommUtils.outblue("test3 v= " + v	);
		assertTrue( v == 0.71 );
	} 
	@Test   
	public void test4() {
		CommUtils.outblue("test4 sin(pi/6) ");
		double v = FSin.eval( Math.PI/ 6 );
		assertTrue( v == 0.5 );
	} 
 
}
