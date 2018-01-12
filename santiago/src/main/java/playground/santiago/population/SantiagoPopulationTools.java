package playground.santiago.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class SantiagoPopulationTools {
	private static final Logger log = Logger.getLogger(SantiagoPopulationTools.class);
	
	public static void moveInfoBetweenPops(Population originalPopulation, Population basePopulation){
		log.info("Copying the info. of activity schedule from "
				+ "randomized plans to baseCase plans...");
		
		for (Person originalPerson: originalPopulation.getPersons().values()){
			
			Id<Person> originalPersonId = originalPerson.getId();
			
			for (Person basePerson: basePopulation.getPersons().values()){
				
				Id<Person> basePersonId = basePerson.getId();
				
				if(originalPersonId.equals(basePersonId)){
					
					String actStartEndTimes = (String) originalPerson.getAttributes().getAttribute("OpeningClosingTimes");
					basePerson.getAttributes().putAttribute("OpeningClosingTimes", actStartEndTimes);
					break;
					
				}
				
			}
			
		}		
		
	}

}
