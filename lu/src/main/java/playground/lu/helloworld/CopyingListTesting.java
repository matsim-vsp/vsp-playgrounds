package playground.lu.helloworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CopyingListTesting {
	public static void main(String[] args) {
		
		Random rnd = new Random();
		
		System.out.println("Copy list testing");
		List<Integer> list1 = new ArrayList<>();
		list1.add(1);
		list1.add(2);
		list1.add(3);
		list1.add(4);
		
		List<Integer> list2 = new ArrayList<>(list1);
		
		list1.remove(3);
		
		System.out.println("list 1 is: ");
		System.out.println(list1);
		System.out.println("list 2 is: ");
		System.out.println(list2);

		
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		System.out.println(rnd.nextInt(20));
		
	}
}
