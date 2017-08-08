package playground.santiago.analysis;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.santiago.analysis.others.SantiagoCarLegsAnalysis;
import playground.santiago.analysis.others.SantiagoStuckAgentsAnalysis;
import playground.santiago.analysis.trafficVolumes.SantiagoTrafficVolumesAnalysis;
import playground.santiago.analysis.travelDistances.SantiagoTravelDistancesAnalysis;
import playground.santiago.analysis.travelTimes.SantiagoTravelTimesAnalysis;

public class SantiagoRunAnalysis {
	//Fields related to the scenario and its steps - they must be changed depending on the step
	private static final String CASE_NAME = "policyRuns/10pct";
	private static final String STEP_NAME = "StepTriangle0";
	private static final int IT_TO_EVALUATE = 200; //From the local counter
	private static final int REFERENCE_IT = 600; //Reference iteration (same as first iteration of the step)
//	private static final int LAST_IT = 600;
	
	
	public static void main (String[]args){
//		SantiagoStuckAgentsAnalysis stuckAnalysis = new SantiagoStuckAgentsAnalysis(CASE_NAME,STEP_NAME);
		

		int it = IT_TO_EVALUATE;
		int itAux = IT_TO_EVALUATE+REFERENCE_IT;
		
//		while(itAux<=LAST_IT){
			
//			List<Id<Person>> stuckAgents = stuckAnalysis.getStuckAgents(it);
			
//			SantiagoTravelTimesAnalysis timesAnalysis = new SantiagoTravelTimesAnalysis(CASE_NAME, STEP_NAME, stuckAgents);
//			timesAnalysis.writeFileForTravelTimesByMode(it,itAux);
		
//			SantiagoTrafficVolumesAnalysis trafficAnalysis = new SantiagoTrafficVolumesAnalysis(CASE_NAME,STEP_NAME);
//			trafficAnalysis.writeFileForLinkVolumes(it, itAux);
		
			SantiagoCarLegsAnalysis carLegAnalysis = new SantiagoCarLegsAnalysis(CASE_NAME, STEP_NAME);
			carLegAnalysis.writeCarLegs(it, itAux);
			
//			SantiagoTravelDistancesAnalysis distancesAnalysis = new SantiagoTravelDistancesAnalysis(CASE_NAME, STEP_NAME, stuckAgents);
//			distancesAnalysis.writeFileForTravelDistancesByMode(it,itAux);
			
//			it+=50;
//			itAux+=50;	
//		}
				
		
	}
	
	
}
