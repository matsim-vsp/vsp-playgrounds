package playground.sbraun.algorithm;

import org.junit.Assert;
import org.junit.Test;


public class HeapSortTest {

	@Test
	public void test1() {
		boolean sorted = false;
		
		//Integerarray with Length 10 and range 0-99
		Integer[] testarray = new Integer[5];
		for (int i = 0; i < testarray.length ; i++) {
			testarray[i] = new Integer((int)(Math.random()*100));
		}

		/* Print before Sorting
		for (int i = 0; i < testarray.length ; i++) {
			System.out.print(testarray[i].toString() + ", ");
		}
		 */ 
		
		
		//Sort it
		HeapSort heap = new HeapSort(testarray);
		heap.buildHeap();
		heap.heapSort();
		
		
		// test if sorted
		for (int i = 0; i < testarray.length-1 ; i++) {
			if(testarray[i].compareTo(testarray[i+1])<0) {
				sorted = true;
			} else sorted = false;
		}
		
		
		//Print after Sorting
		/*
		System.out.println();
		for (int i = 0; i < testarray.length ; i++) {
			System.out.print(testarray[i].toString() + ", ");
		}
		*/
		
		Assert.assertEquals("Array should be sorted", true , sorted);
	}
	
	public void test2() {
		boolean sorted = false;
		
		//Integerarray with Length 10 and range 0-99
		Double[] testarray = new Double[5];
		for (int i = 0; i < testarray.length ; i++) {
			testarray[i] = new Double(Math.random()*100);
		}

		//Sort it
		HeapSortGeneric<Double> heap = new HeapSortGeneric<Double>(testarray);
		heap.buildHeap();
		heap.heapSort();
		
		
		// test if sorted
		for (int i = 0; i < testarray.length-1 ; i++) {
			if(testarray[i].compareTo(testarray[i+1])<0) {
				sorted = true;
			} else sorted = false;
		}

		Assert.assertEquals("Array should be sorted", true , sorted);
	}
}
