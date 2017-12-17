package playground.sbraun.algorithm;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author sbraun
 *
 */


public class HeapSortTest {

	@Test
	public void test1() {
		//boolean sorted = false;
		boolean sorted = true;
		
		//Integerarray with Length 10 and range 0-99
		Integer[] testarray = new Integer[50];
		for (int i = 0; i < testarray.length ; i++) {
			testarray[i] = new Integer((int)(Math.random()*100));
		}

		//Print before Sorting
		/*
		System.out.println("This is a random integer array:");
		for (int j = 0; j < testarray.length ; j++) {
			System.out.print(testarray[j].toString() + ", ");
		}
		 */
		
		
		//Sort it
		HeapSort heap = new HeapSort(testarray);
		heap.buildHeap();
		heap.heapSort();
		
		
		// test if sorted
		int i=0;
		while(sorted && i<testarray.length-1) {
			if(testarray[i].compareTo(testarray[i+1])>0) sorted=false;
			i++;
		}
		
		
		//Print after Sorting
		/*
		System.out.println("the sorted array is:");
		for (int j = 0; j < testarray.length ; j++) {
			System.out.print(testarray[j].toString() + ", ");
		}
		*/
		
		Assert.assertEquals("Array should be sorted", true , sorted);
	}
	
	
	//for generic Heapsort
	public void test2() {
		boolean sorted = true;
		
		//Integerarray with Length 10 and range 0-99
		Double[] testarray = new Double[100];
		for (int i = 0; i < testarray.length ; i++) {
			testarray[i] = new Double(Math.random()*100);
		}

		//Sort it
		HeapSortGeneric<Double> heap = new HeapSortGeneric<Double>(testarray);
		heap.buildHeap();
		heap.heapSort();
		
		
		// test if sorted		
		int i=0;
		while(sorted && i<testarray.length-1) {
			if(testarray[i].compareTo(testarray[i+1])>0) sorted=false;
			i++;
		}

		Assert.assertEquals("Array should be sorted", true , sorted);
	}
}
