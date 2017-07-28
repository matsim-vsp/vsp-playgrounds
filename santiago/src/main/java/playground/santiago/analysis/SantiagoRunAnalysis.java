package playground.santiago.analysis;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.santiago.analysis.others.SantiagoStuckAgentsAnalysis;
import playground.santiago.analysis.travelDistances.SantiagoTravelDistancesAnalysis;
import playground.santiago.analysis.travelTimes.SantiagoTravelTimesAnalysis;

public class SantiagoRunAnalysis {
	//Fields related to the scenario and its steps - they must be changed depending on the step
	private static final String CASE_NAME = "policyRuns/1pct";
	private static final String STEP_NAME = "StepOuter0";
	private static final int FIRST_IT = 600;
	private static final int LAST_IT = 800;
	
	
	public static void main (String[]args){
		SantiagoStuckAgentsAnalysis stuckAnalysis = new SantiagoStuckAgentsAnalysis(CASE_NAME,STEP_NAME);
		

		int it = 0;
		int itAux = FIRST_IT;
		
		while(itAux<=LAST_IT){
			
			List<Id<Person>> stuckAgents = stuckAnalysis.getStuckAgents(it);
			
			SantiagoTravelTimesAnalysis timesAnalysis = new SantiagoTravelTimesAnalysis(CASE_NAME, STEP_NAME, stuckAgents);
			timesAnalysis.writeFileForTravelTimesByMode(it,itAux);
			
//			SantiagoTravelDistancesAnalysis distancesAnalysis = new SantiagoTravelDistancesAnalysis(CASE_NAME, STEP_NAME, stuckAgents);
//			distancesAnalysis.writeFileForTravelDistancesByMode(it,itAux);
			
			it+=50;
			itAux+=50;	
		}
				
		
	}
	
	
}
