package playground.julia.distribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class ExposureUtils {

	public void printTimeTables(List<PersonalExposure> popExposure, String outPathForTimeTables) {
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outPathForTimeTables));

			// header: Person base case compare case difference
			buffW.write("Timetable");
			buffW.newLine();

			for (PersonalExposure perEx : popExposure) {
				buffW.write("Person id: " + perEx.getPersonalId().toString());
				buffW.newLine();
				buffW.write("Average exposure:" + perEx.getAverageExposure());
				buffW.newLine();
				buffW.write("activity type \t start time \t end time \t duration \t exposure value \t exposure x duration");
				buffW.newLine();

				TimeDependendExposure next = perEx.getNextTimeDependendExposure(null);

				while (next != null) {
					buffW.write(perEx.getStringForInterval(next));
					buffW.newLine();
					next = perEx.getNextTimeDependendExposure(next);
				}

				buffW.newLine();
			}

			buffW.close();

		} catch (IOException e) {
			System.err.println("Error creating " + outPathForTimeTables + ".");
		}
	}


	
	public void printPersonalExposureTimeTables(ArrayList<ExposureEvent> exposure,
			String outPathForTimeTables) {
//TODO
		
	}

	public void printPersonalResponibility(ArrayList<ResponsibilityEvent> responsibility,
			String outputPath) {
		// sum, average
		
		// sum = #persons x concentration x time
		// timeXpersons = #persons x time
		
		Double sum =0.0;
		Double timeXpersons =0.0;
		
		Map<Id, Double> person2timeXconcentration = new HashMap<Id, Double>();
		Map<Id, Double> person2time = new HashMap<Id, Double>();
		Map<Id, Double> person2average = new HashMap<Id, Double>();
		
		for(ResponsibilityEvent re : responsibility){
			Id personId = re.getPersonId();
			if(person2timeXconcentration.containsKey(personId)){
				Double newValue = person2timeXconcentration.get(personId)+re.getExposureValue();
				person2timeXconcentration.put(personId, newValue);
				Double newTime = person2time.get(personId) + re.getDuration();
				person2time.put(personId, newTime);
			}else{
				person2timeXconcentration.put(personId, re.getExposureValue());
				person2time.put(personId, re.getDuration());
			}
		}
		
		Double personalTotalMax = -1.0;
		Double personalTotalMin = Double.MAX_VALUE;
		
		for(Id personId: person2timeXconcentration.keySet()){
			Double value = person2timeXconcentration.get(personId);
			sum += value;
			timeXpersons += person2time.get(personId);
			person2average.put(personId, (value/person2time.get(personId))); // wie viel sinn macht es, das auszurechnen?
			if(value<personalTotalMin)personalTotalMin=value;
			if(value>personalTotalMax)personalTotalMax=value;
			
		}
		
		Double personalAverageAverage =0.0;
		Double personalAverageMin = Double.MAX_VALUE;
		Double personalAverageMax = -1.0;
		
		for(Id personId: person2average.keySet()){
			Double pa = person2average.get(personId);
			personalAverageAverage+= pa;
			if(pa<personalAverageMin)personalAverageMin=pa;
			if(pa>personalAverageMax)personalAverageMax=pa;
		}
		personalAverageAverage /= person2average.size();
		
		// write results
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outputPath));

			// header: Person base case compare case difference
			buffW.write("Responsibility Information");
			buffW.newLine();
			
			buffW.write("Person x time x concentration : " + sum);
			buffW.newLine();
			buffW.write("Average of average responsibility : " + personalAverageAverage);
			buffW.newLine();
			buffW.write("Maximal average responsibility : " + personalAverageMax);
			buffW.newLine();
			buffW.write("Minimal average responsibility : " +personalAverageMin);
			buffW.newLine();
			buffW.write("Average total responsibility : " + (sum/person2timeXconcentration.size()));
			buffW.newLine();
			buffW.write("Maximal total responsibility : " + personalTotalMax);
			buffW.newLine();
			buffW.write("Minimal total responsibility : " +personalTotalMin);
			buffW.newLine();
			buffW.write("Total exposure time : " +timeXpersons);
			buffW.newLine();
			
			buffW.close();
		} catch (IOException e) {
			System.err.println("Error creating " + outputPath + ".");
		}
			
	}

	public void printExposureInformation(ArrayList<ExposureEvent> exposure,
			String outputPath) {
		
		// sum = #persons x time x concentration
		// average concentration = sum/time/#persons
		// personal average average 
		// personal average min, max 
		
		Double sum =0.0; // sum over person2timeXconcentration
		Double timeXpersons = 0.0; // sum over person2time
		Map<Id, Double> person2timeXconcentration = new HashMap<Id, Double>();
		Map<Id, Double> person2time = new HashMap<Id, Double>();
		Map<Id, Double> person2average = new HashMap<Id, Double>();
		
		for(ExposureEvent exe: exposure){
			Id personId = exe.getPersonId();
			if (person2timeXconcentration.containsKey(exe.getPersonId())) {
				Double newValue = person2timeXconcentration.get(personId) + exe.getExposure();
				person2timeXconcentration.put(personId, newValue);
				Double newTime = person2time.get(personId)+exe.getDuration();
				person2time.put(personId, newTime);
			}else{
				person2timeXconcentration.put(personId,	exe.getExposure());
				person2time.put(personId, exe.getDuration());
			}
		}
		
		// calculate average, sum, time sum ...
		
		for(Id personId: person2timeXconcentration.keySet()){
			sum += person2timeXconcentration.get(personId);
			timeXpersons += person2time.get(personId);
			person2average.put(personId, (person2timeXconcentration.get(personId)/person2time.get(personId)));
		}
		
		Double personalAverageAverage =0.0;
		Double personalAverageMin = Double.MAX_VALUE;
		Double personalAverageMax = -1.0;
		
		for(Id personId: person2average.keySet()){
			Double pa = person2average.get(personId);
			personalAverageAverage+= pa;
			System.out.println(pa);
			if(pa<personalAverageMin)personalAverageMin=pa;
			if(pa>personalAverageMax)personalAverageMax=pa;
		}
		personalAverageAverage /= person2average.size();
		
		
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outputPath));

			// header: Person base case compare case difference
			buffW.write("Exposure Information");
			buffW.newLine();
			
			buffW.write("Person x time x concentration : " + sum);
			buffW.newLine();
			buffW.write("Average of average exposure : " + personalAverageAverage);
			buffW.newLine();
			buffW.write("Maximal average exposure : " + personalAverageMax);
			buffW.newLine();
			buffW.write("Minimal average exposure : " +personalAverageMin);
			buffW.newLine();
			buffW.write("Total exposure time : " +timeXpersons);
			buffW.newLine();
			
			buffW.close();
		} catch (IOException e) {
			System.err.println("Error creating " + outputPath + ".");
		}
		
	}



	public void printResponsibilityInformation(
			ArrayList<ResponsibilityEvent> responsibility, String outputPath) {
		
		// responsibility sum = #responsible persons x time x concentration
		// average responsibility = sum/#responsible persons
		// personal average min, max 
		
		
		Double sum = 0.0; 
		Map<Id, Double> person2responsibility = new HashMap<Id, Double>();
		Double maxResponsibility=-1.0;
		Double minResponsibility=Double.MAX_VALUE;
		Double avgResponsibility=0.0;
		
		// calculate total sum, personal sum
		for(ResponsibilityEvent re: responsibility){
			sum += re.getExposureValue();
			Id responsiblePerson = re.getPersonId();
			if(!person2responsibility.containsKey(responsiblePerson)){
				person2responsibility.put(responsiblePerson, new Double(re.getExposureValue()));
			}else{
				Double oldValue = person2responsibility.get(responsiblePerson);
				person2responsibility.put(responsiblePerson, oldValue+re.getExposureValue());
			}
		}
		
		// find min and max
		for(Id person: person2responsibility.keySet()){
			Double currentValue = person2responsibility.get(person);
			if(currentValue>maxResponsibility)maxResponsibility=currentValue;
			if(currentValue<minResponsibility)minResponsibility=currentValue;
		}
		
		avgResponsibility = sum/person2responsibility.size();
		
		BufferedWriter buffW;
		try{
			buffW = new BufferedWriter(new FileWriter(outputPath));

			// header: Person base case compare case difference
			buffW.write("Responsibility Information");
			buffW.newLine();
			buffW.write("Responsible persons x concentration value x exposure time: " + sum);
			buffW.newLine();
			buffW.write("Average responsibility: " + avgResponsibility);
			buffW.newLine();
			buffW.write("Minimal responsibility: " + minResponsibility);
			buffW.newLine();
			buffW.write("Maximal responsibility: " + maxResponsibility);
			buffW.close();
		}catch(IOException e){
			System.err.println("Error creating " + outputPath + ".");
		}
		
		/*
		 *Double sum =0.0; // sum over person2timeXconcentration
	
		
		
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outputPath));

			// header: Person base case compare case difference
			buffW.write("Exposure Information");
			buffW.newLine();
			
			buffW.write("Person x time x concentration : " + sum);
			buffW.newLine();
			buffW.write("Average of average exposure : " + personalAverageAverage);
			buffW.newLine();
			buffW.write("Maximal average exposure : " + personalAverageMax);
			buffW.newLine();
			buffW.write("Minimal average exposure : " +personalAverageMin);
			buffW.newLine();
			buffW.write("Total exposure time : " +timeXpersons);
			buffW.newLine();
			
			buffW.close();
		} catch (IOException e) {
			System.err.println("Error creating " + outputPath + ".");
		} 
		 */
		
	}



}
