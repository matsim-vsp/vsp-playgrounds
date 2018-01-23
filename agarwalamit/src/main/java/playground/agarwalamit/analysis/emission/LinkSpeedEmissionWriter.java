/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.emission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkSpeed.LinkSpeedHandler;
import playground.agarwalamit.analysis.emission.filtering.FilteredColdEmissionHandler;
import playground.agarwalamit.analysis.emission.filtering.FilteredWarmEmissionHandler;
import playground.agarwalamit.analysis.linkSpeed.LinkSpeedHandlerBackwardCompatible;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Created by amit on 18.01.18.
 */

public class LinkSpeedEmissionWriter {

    public static void main(String[] args) {
        //Patna
        String patnaDir = FileUtils.RUNS_SVN + "/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
        new LinkSpeedEmissionWriter().run(patnaDir + "output_config.xml.gz",
                patnaDir + "output_network.xml.gz",
                patnaDir + "output_events.xml.gz",
                patnaDir + "output_emissions_events.xml.gz",
                patnaDir + "analysis/linkSpeedEmissionStats.txt", false);

        //munich
        //TODO LinkSpeedHandler will not work for following files becuase vehicleEntersTrafficEvent does not exists.
        String munichDir = FileUtils.RUNS_SVN + "/detEval/emissionCongestionInternalization/iatbr/output/bau/";
        new LinkSpeedEmissionWriter().run(munichDir + "output_config.xml.gz",
                munichDir + "output_network.xml.gz",
                munichDir + "ITERS/it.1500/1500.events.xml.gz",
                munichDir + "ITERS/it.1500/1500.emission.events.xml",
                munichDir + "analysis/linkSpeedEmissionStats.txt", true);
    }


    public void run(String configFile, String networkFile, String eventsFile, String emissionEventsFile, String outFile, boolean analysis4Munich) {
        double simEndTime = LoadMyScenarios.getSimulationEndTime(configFile);
        Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
        int noOfTimeBins = (int) ( simEndTime/(3600*2) );

        EventsManager eventsManager = EventsUtils.createEventsManager();
        SortedMap<Double, Map<Id<Link>, Double>> time2link2speed = null;

        LinkSpeedHandler linkSpeedHandlerOthers = null;
        LinkSpeedHandlerBackwardCompatible linkSpeedHandlerMunich = null;
        if(analysis4Munich) {
            linkSpeedHandlerMunich = new LinkSpeedHandlerBackwardCompatible(simEndTime,
                    noOfTimeBins,
                    network,
                    Arrays.asList("car", "motorbike"));
            eventsManager.addHandler(linkSpeedHandlerMunich);
        } else {
            linkSpeedHandlerOthers = new LinkSpeedHandler(simEndTime,
                    noOfTimeBins,
                    network,
                    Arrays.asList("car", "motorbike"));
            eventsManager.addHandler(linkSpeedHandlerOthers);
        }

        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(eventsFile);

        if(analysis4Munich){
            time2link2speed = linkSpeedHandlerMunich.getTime2link2AverageTimeMeanSpeed();
        } else {
            time2link2speed = linkSpeedHandlerOthers.getTime2link2AverageTimeMeanSpeed();
        }

        EventsManager eventsManager2 = EventsUtils.createEventsManager();
        FilteredWarmEmissionHandler warmEmissionHandler = new FilteredWarmEmissionHandler(simEndTime, noOfTimeBins);
        FilteredColdEmissionHandler coldEmissionHandler = new FilteredColdEmissionHandler(simEndTime, noOfTimeBins);
        eventsManager2.addHandler(warmEmissionHandler);
        eventsManager2.addHandler(coldEmissionHandler);
        EmissionEventsReader reader2 = new EmissionEventsReader(eventsManager2);
        reader2.readFile(emissionEventsFile);

        Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = warmEmissionHandler.getWarmEmissionsPerLinkAndTimeInterval();
        Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = coldEmissionHandler.getColdEmissionsPerLinkAndTimeInterval();

        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write(
                    "endOfTimeInterval\tlinkId\ttimeMeanSpeedKPH\twarmNO2EmissionGram\tcoldNO2EmissionGram\ttotalEmissionsGram\n");
            for (double time : time2link2speed.keySet()) {
                if (time>86400.) continue;
                for (Id<Link> linkId : time2link2speed.get(time).keySet()) {
                    double warmNO2 = time2warmEmissionsTotal.getOrDefault(time, new HashMap<>())
                                                            .getOrDefault(linkId, new HashMap<>())
                                                            .getOrDefault(WarmPollutant.NO2, 0.);
                    double coldNO2 = time2coldEmissionsTotal.getOrDefault(time, new HashMap<>())
                                                            .getOrDefault(linkId, new HashMap<>())
                                                            .getOrDefault(ColdPollutant.NO2, 0.);
                    double totalNO2 = warmNO2 + coldNO2;
                    double speed = NumberUtils.round(time2link2speed.get(time).get(linkId), 2);
                    writer.write(time + "\t" + linkId + "\t" + speed + "\t" + warmNO2 + "\t" + coldNO2 + "\t" + totalNO2 + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
