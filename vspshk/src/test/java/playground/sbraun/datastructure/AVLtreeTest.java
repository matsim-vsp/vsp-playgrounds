package playground.sbraun.datastructure;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author sbraun
 *
 */


public class AVLtreeTest {
	
	@Test
	public void test() {
		AVLTree<Integer> test = new AVLTree<Integer>();
		
		// insert
		int[] keys = new int[7];
		for (int i = 0 ; i< keys.length ; i++) {
			keys[i] = (int)(Math.random()*100);
			test.insert(keys[i], new Integer(keys[i]));
		}
		//some deletions
		test.delete(1);
		test.delete(2);
		
		//Tree-Representation --- uncomment
		//test.print();
		
		//The tree is balanced if the Heights of the children differ in not more then 1
		Assert.assertEquals("Rootbalance should be <=|1|", true , Math.abs(test.getRootbalance())<=1);
	}
	
}
