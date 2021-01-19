package playground.lu.helloworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CopyingListTesting {

	public static class MyObject {
		private String name;
		private double feature1;
		private int feature2;

		public MyObject(String name, double feature1, int feature2) {
			this.name = name;
			this.feature1 = feature1;
			this.feature2 = feature2;
		}

		public double getFeature1() {
			return feature1;
		}

		public void setFeature1(double feature1) {
			this.feature1 = feature1;
		}

		public void setFeature2(int feature2) {
			this.feature2 = feature2;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getFeature2() {
			return feature2;
		}
	}

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

		System.out.println("One object in different collection test");
		MyObject myObject1 = new MyObject("o1", 100.0, 1);
		MyObject myObject2 = new MyObject("o2", 200.0, 2);
		MyObject myObject3 = new MyObject("o3", 300.0, 3);

		List<MyObject> myObjectList1 = new ArrayList<>();
		myObjectList1.add(myObject1);
		myObjectList1.add(myObject2);

		List<MyObject> myObjectList2 = new ArrayList<>();
		myObjectList2.add(myObject2);
		myObjectList2.add(myObject3);

		MyObject chosenObject = myObjectList2.get(0);
		chosenObject.setFeature1(256);
		chosenObject.setFeature2(222);

		MyObject outputObject = myObjectList1.get(1);
		System.out.println("Output Object is " + outputObject.getName() + " with features: "
				+ Double.toString(outputObject.getFeature1()) + " and " + Integer.toString(outputObject.getFeature2()));

	}
}
