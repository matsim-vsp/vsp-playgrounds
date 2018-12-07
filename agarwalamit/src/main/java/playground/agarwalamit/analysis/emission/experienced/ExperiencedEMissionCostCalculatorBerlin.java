/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.emission.experienced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.vsp.airPollution.CombinedEmissionEventsReader;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.IntervalHandler;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * Created by amit on 15.05.17.
 */


public class ExperiencedEMissionCostCalculatorBerlin {

    public static void main (String args []) {

		final Integer noOfXCells = 677;
		final Integer noOfYCells = 446;
		final double xMin = 4565039.;
		final double xMax = 4632739.;
		final double yMin = 5801108.;
		final double yMax = 5845708.;

        final Double timeBinSize = 300.;
        final int noOfTimeBins = 360;

        // berlin
		String dir = "/Users/ihab/Desktop/ils4a/kaddoura/cne_berlin10pct/output/";

        // berlin
		String [] cases = {
//				"m_r_output_c"
//				,"m_r_output_c_0.01"
//				,"m_r_output_cne"
//				,"m_r_output_cne_0.01"
//				,"m_r_output_e"
//				,"m_r_output_n"
//				,"m_r_output_run0_baseCase"
				
				"r_output_c"
				,"r_output_c_0.01"
				,"r_output_cne"
				,"r_output_cne_0.01"
				,"r_output_e"
				,"r_output_n"
//				,"r_output_run0_baseCase"
		};
		int [] its = {200};
 
        try(BufferedWriter writer = IOUtils.getBufferedWriter(dir +"airPolluationExposureCosts.txt")) {
            writer.write("case \t itNr \t costsInEur \t tollValuesEUR \n");

            for(String str : cases) {
                for(int itr : its) {
                    String networkFile = dir+str+"/output_network.xml.gz";
                    String configFile = dir+str+"/output_config.xml";
                    String eventsFile = dir + str + "/ITERS/it." + itr + "/" + itr + ".events.xml.gz";

                    if( ! new File(networkFile).exists() || ! new File(configFile).exists() || ! new File(eventsFile).exists() ) {
                        continue;
                    }

                    double simulationEndtime = LoadMyScenarios.getSimulationEndTime(configFile);

                    GridTools gt = new GridTools(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
                    IntervalHandler intervalHandler = new IntervalHandler(timeBinSize, simulationEndtime, gt);

                    final Map<Id<Person>, Double> person2toll = new HashMap<>();
                    EventsManager eventsManager = EventsUtils.createEventsManager();
                    eventsManager.addHandler(intervalHandler);
                    eventsManager.addHandler(new PersonMoneyEventHandler() {
                        @Override
                        public void handleEvent(PersonMoneyEvent event) {
                            if(person2toll.containsKey(event.getPersonId())) {
                                person2toll.put(event.getPersonId(), person2toll.get(event.getPersonId()) + event.getAmount());
                            } else {
                                person2toll.put(event.getPersonId(), event.getAmount());
                            }
                        }
                        @Override
                        public void reset(int iteration) {

                        }
                    });
                    new MatsimEventsReader(eventsManager).readFile(eventsFile);

                    ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
                    rgt.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());

                    EmissionsConfigGroup emissionsConfigGroup  = new EmissionsConfigGroup();
                    emissionsConfigGroup.setConsideringCO2Costs(true);
                    emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

                    EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(emissionsConfigGroup, rgt);
                    ExperiencedEmissionCostHandler handler = new ExperiencedEmissionCostHandler(emissionCostModule, null, simulationEndtime, noOfTimeBins);

                    EventsManager events = EventsUtils.createEventsManager();
                    events.addHandler(handler);
                    CombinedEmissionEventsReader reader = new CombinedEmissionEventsReader(events);
                    reader.readFile(eventsFile);

                    handler.getUserGroup2TotalEmissionCosts()
                           .forEach((key, value) -> System.out.println(key + "\t" + value));
                    writer.write(str+"\t"+itr+"\t"+ MapUtils.doubleValueSum(handler.getUserGroup2TotalEmissionCosts())+"\t");

                    writer.write(MapUtils.doubleValueSum(person2toll)+"\n");

                    // writing time bin 2 costs
                    BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(dir + "timeBin2AirPollutionExposureCosts_"+str+"_timeBinSize_" + timeBinSize + ".txt");

                    Map<Double, Double> time2costs = handler.getTimeBin2TotalCosts();
                    bufferedWriter.write("timeBin\tairPollutionExposureCostsEUR\n");
                    for(Double d : time2costs.keySet()) {
                        bufferedWriter.write(d+"\t"+time2costs.get(d)+"\n");
                    }
                    bufferedWriter.close();
                }
            }
            writer.close();
        } catch(IOException e) {
            throw new RuntimeException("Data is not written.");
        }
    }

}
