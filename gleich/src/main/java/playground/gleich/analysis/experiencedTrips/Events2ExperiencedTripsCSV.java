/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gleich.analysis.experiencedTrips;

import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.analysis.TripsCSVWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;

/**
 * 
 * @author vsp-gleich
 *
 */
public final class Events2ExperiencedTripsCSV {

    private Config config;
    private Scenario scenario;
    private String eventsFile;
    private ExperiencedPlansService experiencedPlansService;
	// second level separator
	private final String sep2 = ",";
    
    public static void main(String[] args) {
    	String pathInclRunId = "/home/gregor/git/matsim-berlin/scenarios/berlin-v5.5-1pct/output-berlin-drt-v5.5-1pct/berlin-drt-v5.5-1pct";
        Config config = ConfigUtils.loadConfig(pathInclRunId + ".output_config.xml");
        config.network().setInputFile(pathInclRunId + ".output_network.xml.gz");
        config.transit().setTransitScheduleFile(pathInclRunId + ".output_transitSchedule.xml.gz");
        config.plans().setInputFile(pathInclRunId + ".output_plans.xml.gz");
        
        Events2ExperiencedTripsCSV runner = new Events2ExperiencedTripsCSV(config, 
        		pathInclRunId + ".output_events.xml.gz");
        runner.runAnalysisAndWriteResult(pathInclRunId + ".output_experiencedTrips.csv.gz");
    }

    public Events2ExperiencedTripsCSV(Config config, String eventsFile) {
        this.config = config;
        this.eventsFile = eventsFile;
        
        readEventsAndPrepareExperiencedPlansService(config);
    }

    private void readEventsAndPrepareExperiencedPlansService(Config config) {
        scenario = ScenarioUtils.loadScenario(config);
        Injector injector = org.matsim.core.controler.Injector.createInjector(config,
                new Module[]{
                        new ExperiencedPlansModule(),
                        new ExperiencedPlanElementsModule(),
                        new EventsManagerModule(),
                        new ScenarioByInstanceModule(scenario),
                        new org.matsim.core.controler.ReplayEvents.Module()});
        ((EventsToLegs)injector.getInstance(EventsToLegs.class)).setTransitSchedule(scenario.getTransitSchedule());
        ReplayEvents replayEvents = (ReplayEvents)injector.getInstance(ReplayEvents.class);
        replayEvents.playEventsFile(eventsFile, 0);
        
        experiencedPlansService = ((ExperiencedPlansService)injector.getInstance(ExperiencedPlansService.class));
    }
    
    public void runAnalysisAndWriteResult(String outputExperiencedTripsFile) {
    	TripsCSVWriter.CustomTripsWriterExtension customTripsWriterExtension = new ExperiencedTripsExtension();
    	 new TripsCSVWriter(scenario, customTripsWriterExtension).write(experiencedPlansService.getExperiencedPlans(), outputExperiencedTripsFile);
    }
    
    private class ExperiencedTripsExtension implements TripsCSVWriter.CustomTripsWriterExtension {

		@Override
		public String[] getAdditionalHeader() {
			List<String> header = new ArrayList<>();
			header.add("transitStopsVisited");
			
			Collection<String> monitoredModes = config.planCalcScore().getAllModes();
			for(String mode: monitoredModes){
				header.add(mode + ".InVehicleTime");
				header.add(mode + ".Distance");
				header.add(mode + ".WaitTime");
				header.add(mode + ".maxPerLegWaitTime");
				header.add(mode + ".NumberOfLegs");
			}
			
			return header.toArray(new String[0]);
		}

		@Override
		public List<String> getAdditionalColumns(Trip trip) {
			List<String> values = new ArrayList<>();
			// TODO: add real values
			values.add("transitStopsVisited");
			
			Collection<String> monitoredModes = config.planCalcScore().getAllModes();
			for(String mode: monitoredModes){
				values.add(mode + ".InVehicleTime");
				values.add(mode + ".Distance");
				values.add(mode + ".WaitTime");
				values.add(mode + ".maxPerLegWaitTime");
				values.add(mode + ".NumberOfLegs");
			}
			return values;
		}
    }
}
