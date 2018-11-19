package playground.mdziakowski.ODMatrixBerlin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

public class CreatingODMatrix {

	public static void main(String[] args) throws IOException {

//		sets input file and output location
		String inputFile = "D:/Arbeit/Berlin/Matrix10.csv";
		String outputFile = "D:/Arbeit/Berlin/10Test.txt";
		
//		for specific modes set allModes to false and add the modes to the List mode
		boolean allModes = true;				
		List<String> modes = Arrays.asList("");

//		for specific activities set allActivities to false and add the activities to the List activities
		boolean allActivities = true;
		List<String> activities = Arrays.asList("");
		
//		for specific start and endTime set vonTime and bisTime in hours and startTime and/or endTime to true
//		example: vonTime = 9 and bisTime = 10; starTime = true and endTime = false 
//			takes all Trips that start  between 9 and 10 o'clock, without checking the time a trip ends
		double vonTime = 0;
		double bisTime = 36;

		boolean startTime = false;
		boolean endTime = false;
		
		double vonTimeInSec = vonTime * 60 * 60;
		double bisTimeInSec = bisTime * 60 * 60;

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		List<MyTrip> listMyTrip = readingFile(inputFile);
		
		Map<String, District> districts = countingDistricts(listMyTrip);

		listMyTrip = filterMode(allModes, modes, listMyTrip);
		listMyTrip = filterActivities(allActivities, activities, listMyTrip);
		listMyTrip = filterTime(vonTimeInSec, bisTimeInSec, startTime, endTime, listMyTrip);		
		List<TripCounter> listTripCounter = countingTrips(listMyTrip, districts);
		
		int[][] countArray = new int[districts.size()][districts.size()];
		
		for (TripCounter tCounter : listTripCounter) {
			countArray[tCounter.getStartDistrict().getId()][tCounter.getEndDistrict().getId()] = tCounter.getCounter();
		}
	
		String headline = "von/nach;";
		int i= 0;
		while (i < districts.size()) {
			headline = headline + getDistrivtWithId(districts, i).getName() + ";";
			i++;
		}
		
		writer.write(headline);
		writer.newLine();
		
		i = 0;

		for (int[] zeile : countArray) {
			writer.write(getDistrivtWithId(districts, i).getName() + ";");
			i++;
			for (int spalte : zeile) {
				writer.write(spalte + ";");
				writer.flush();
			}
			writer.newLine();
		}
		writer.close();
		System.out.println("Done");
		
	}

	private static District getDistrivtWithId(Map<String, District> districts, int i) {
		for(District district : districts.values()) {
			if (district.getId() == i) {
				return district;
			}
		}
		return null;
	}

	private static Map<String, District> countingDistricts(List<MyTrip> listMyTrip) {
		List<String> stringDistricts = new ArrayList<>();
		Map<String, District> districts = new HashMap<>();
		int districtId = 0;
		for (MyTrip trip : listMyTrip) {
			if (!(stringDistricts.contains(trip.getStartdistrict()))) {
				stringDistricts.add(trip.getStartdistrict());
			}
		}
		for (String d : stringDistricts) {
			districts.put(d,new District(d, districtId++));
		}
		return districts;
	}

	private static List<MyTrip> filterMode(boolean allModes, List<String> modes, List<MyTrip> listMyTrip) {
		if (allModes) {
			return listMyTrip;
		} else {
			for (MyTrip trip : listMyTrip) {
				if (!(modes.contains(trip.getMode()))) {
					trip.setCheck(false);
				}		
			}
		}
		return listMyTrip;
	}
	
	private static List<MyTrip> filterActivities(boolean allActivities, List<String> activites, List<MyTrip> listMyTrip) {
		if (allActivities) {
			return listMyTrip;
		} else {
			for (MyTrip trip : listMyTrip) {
				if (!(activites.contains(trip.getActivity()))) {
					trip.setCheck(false);
				}		
			}
		}
		return listMyTrip;
	}
	
	private static List<MyTrip> filterTime(double vonTimeInSec, double bisTimeInSec, boolean startTime, boolean endTime, List<MyTrip> listMyTrip) {
		for (MyTrip trip : listMyTrip) {
			if (startTime && endTime) {
				if (!(trip.getClockTime() > vonTimeInSec && trip.getClockTime() < bisTimeInSec)) {
					trip.setCheck(false);
				}
				if (!(trip.getMovingclockTime() > vonTimeInSec && trip.getMovingclockTime() < bisTimeInSec)) {
					trip.setCheck(false);
				}
			} else if (startTime && !endTime) {
				if (!(trip.getClockTime() > vonTimeInSec && trip.getClockTime() < bisTimeInSec)) {
					trip.setCheck(false);
				}
	
			} else if (!startTime && endTime) {
				if (!(trip.getMovingclockTime() > vonTimeInSec && trip.getMovingclockTime() < bisTimeInSec)) {
					trip.setCheck(false);
				}
			}
		}
		return listMyTrip;
	}

	private static List<TripCounter> countingTrips(List<MyTrip> listMyTrip, Map<String, District> districts) {
		List<TripCounter> listTripCounter = new ArrayList<>();

		for (MyTrip trip : listMyTrip) {
			String startDistrict = trip.getStartdistrict();
			String endDistrict = trip.getEnddistrict();
			boolean inList = false;
			if (listTripCounter.isEmpty()) {
				TripCounter tripCounter = new TripCounter(districts.get(startDistrict), districts.get(endDistrict));
				listTripCounter.add(tripCounter);
				if (trip.isCheck()) {
					tripCounter.count();
				}
			} else {
				for (TripCounter tripCounter : listTripCounter) {
					if (tripCounter.getStartDistrict().getName().equals(startDistrict) && tripCounter.getEndDistrict().getName().equals(endDistrict)) {
						if (trip.isCheck()) {
							tripCounter.count();
						}
						inList = true;
						break;
					}
				}
				if (!(inList)) {
					TripCounter tripCounter = new TripCounter(districts.get(startDistrict), districts.get(endDistrict));
					listTripCounter.add(tripCounter);
					if (trip.isCheck()) {
						tripCounter.count();
					}
				}
			}
		}
		
		return listTripCounter;
	}

	private static List<MyTrip> readingFile(String file) {
		List<MyTrip> listMyTrip = new ArrayList<>();

		String line = "";
		String cvsSplitBy = ";";

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String headerLine = br.readLine();
			while ((line = br.readLine()) != null) {
				String[] matrix = line.split(cvsSplitBy);
				Id<Person> person = null;
				String startdistrict = null;
				String enddistrict = null;
				String mode = null;
				String activity = null;
				double clockTime = 0;
				double movingclockTime = 0;
				double travelTime = 0;

				for (int i = 0; i < matrix.length; i++) {
					if (i == 0)
						person = Id.createPersonId(matrix[i]);
					else if (i == 1)
						startdistrict = matrix[i];
					else if (i == 2)
						enddistrict = matrix[i];
					else if (i == 3)
						mode = matrix[i];
					else if (i == 4)
						activity = matrix[i];
					else if (i == 5)
						clockTime = Double.parseDouble(matrix[i]);
					else if (i == 6)
						movingclockTime = Double.parseDouble(matrix[i]);
					else if (i == 7)
						travelTime = Double.parseDouble(matrix[i]);
				}

				MyTrip trip = new MyTrip(person, startdistrict, enddistrict, mode, activity, clockTime, movingclockTime,
						travelTime);
				listMyTrip.add(trip);


			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listMyTrip;
	}

}
