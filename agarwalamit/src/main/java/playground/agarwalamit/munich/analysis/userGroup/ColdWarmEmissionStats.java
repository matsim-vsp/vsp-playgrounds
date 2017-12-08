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

package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.caused.CausedEmissionCostHandler;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;

/**
 * Created by amit on 06.12.17.
 */

public class ColdWarmEmissionStats {

    private Map<String, Tuple<Double, Double>> userGroupToColdWarmEmissions = new HashMap<>();
    private static PersonFilter personFilter = new MunichPersonFilter();

    public static void main(String[] args) {

        String eventsFilesDir = FileUtils.RUNS_SVN + "/detEval/emissionCongestionInternalization/ijst_branch12Feb2015/output/";
        String[] runCases = {"bau", "ei", "ei5", "ei10"};
        Double [] emissionCostFactor = new Double[] {1.0, 1.0, 5.0, 10.0};
        StringBuilder builder = new StringBuilder();

        for (int index =0; index <runCases.length; index++) {
            String runCase = runCases[index];
            EmissionsConfigGroup configGroup = new EmissionsConfigGroup();
            configGroup.setEmissionCostMultiplicationFactor(emissionCostFactor[index]);
            configGroup.setConsideringCO2Costs(true);

            EmissionCostModule emissionCostModule = new EmissionCostModule(configGroup);
            CausedEmissionCostHandler handler = new CausedEmissionCostHandler(emissionCostModule);
            EventsManager events = EventsUtils.createEventsManager();
            events.addHandler(handler);
            EmissionEventsReader reader = new EmissionEventsReader(events);
            reader.readFile(eventsFilesDir + "/" + runCase + "/ITERS/it.1500/1500.emission.events.xml.gz");

            Map<Id<Vehicle>, Double> userGroupToColdEmiss = handler.getVehicleId2ColdEmissionCosts();
            Map<Id<Vehicle>, Double> userGroupToWarmEmiss = handler.getVehicleId2WarmEmissionCosts();

            Map<String, Double> userGroupToColdCost  = getUserGroupToEmiss(userGroupToColdEmiss);
            Map<String, Double> userGroupToWarmCost  = getUserGroupToEmiss(userGroupToWarmEmiss);

            for (String ug : personFilter.getUserGroupsAsStrings()){
                builder.append(runCase)
                       .append("\t")
                       .append(ug)
                       .append("\t")
                       .append(userGroupToColdCost.get(ug))
                       .append("\t")
                       .append(userGroupToWarmCost.get(ug))
                       .append("\n");
            }
        }

        String outFileName = "coldWarmEmissCosts.txt";

        BufferedWriter writer = IOUtils.getBufferedWriter(eventsFilesDir + "/analysis/" + outFileName);
        try {
            writer.write("policy\tuserGroup\tcoldEmiss\twarmEmiss\n");
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private static Map<String, Double> getUserGroupToEmiss(Map<Id<Vehicle>, Double> userGroupToEmiss){
     return userGroupToEmiss.entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(e -> personFilter.getUserGroupAsStringFromPersonId(Id.createPersonId(
                                e.getKey().toString())), Collectors.summingDouble(Map.Entry::getValue)));
    }
}
