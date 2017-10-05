package playground.santiago.departureTimeChoice;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;

import org.matsim.core.controler.AbstractModule;

import playground.santiago.utils.StgoPopulationTools;

/**
 * @author LeoCamus based on playground.ikaddoura.agentSpecificActivityScheduling.StgoAgentSpecificActivitySchedulingModule
 *
 */
public class StgoAgentSpecificActivitySchedulingModule extends AbstractModule {
	
	private static final Logger log = Logger.getLogger(StgoAgentSpecificActivitySchedulingModule.class);	
	private final StgoAgentSpecificActivitySchedulingConfigGroup asasConfigGroup;
	
	public StgoAgentSpecificActivitySchedulingModule(Scenario scenario){
		asasConfigGroup = (StgoAgentSpecificActivitySchedulingConfigGroup) scenario.getConfig().getModules().get(StgoAgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);		
		if (asasConfigGroup.isUseAgentSpecificActivityScheduling()) {
//			adjustConfig(scenario.getConfig()); no longer needed.
			
			if (asasConfigGroup.isAdjustPopulation()) {
				log.info("Including the start and end times of the selected plans in the person attributes...");
				
				Population population = scenario.getPopulation();
				
				if (population != null) {
					StgoPopulationTools.setScoresToZero(population); // TODO: I don't know why this is necessary... ?
//					StgoPopulationTools.setActivityTypesAccordingToDurationAndMergeOvernightActivities(population, asasConfigGroup.getActivityDurationBin());	no longer needed.		
					StgoPopulationTools.addActivityTimesOfSelectedPlanToPersonAttributes(population); //this is the critical step.
					StgoPopulationTools.analyze(population); //TODO: I think this is not needed.
						
					if (asasConfigGroup.isRemoveNetworkSpecificInformation()) StgoPopulationTools.removeNetworkSpecificInformation(population);

				} else {
					throw new RuntimeException("Cannot adjust the population if the population is null. Aborting...");
				}
				
				log.info("Adjusting the population... Done.");
			
			} else {
				log.info("Not adjusting the population."
						+ " Opening and closing times are expected to be provided as person attributes in the plans file.");
			}
			
		} else {
			log.info("Agent-specific activity scheduling disabled. Config and population are not adjusted.");
		}
		
				
	}
	
	
	@Override
	public void install() {
		// TODO Auto-generated method stub

	}

}
