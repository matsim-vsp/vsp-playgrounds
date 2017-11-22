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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.emissions.onRoadExposure.EventsComparatorForEmissions;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureConfigGroup;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis.PatnaEmissionsInputGenerator;
import playground.agarwalamit.utils.FileUtils;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;

/**
 * Created by amit on 17.11.17.
 */

public class PatnaOnRoadExposure {

    private static final Logger LOG = Logger.getLogger(PatnaOnRoadExposure.class);

    private static final boolean writeEmissionEvntsFirst = false;
    private static final EventsComparatorForEmissions.EventsOrder EVENTS_ORDER = EventsComparatorForEmissions.EventsOrder.NATURAL_ORDER;

    public static void main(String[] args) {
        Map<String, Map<String, Double>> modeToInhaledMass_bau = new HashMap<>();
        Map<String, Map<String, Double>> modeToInhaledMass_BSH_b = new HashMap<>();

        Map<Id<Link>, Map<String, Double>> linkToInhaledMass_bau = new HashMap<>();
        Map<Id<Link>, Map<String, Double>> linkToInhaledMass_BSH_b = new HashMap<>();

        LOG.info("Using "+ EVENTS_ORDER.toString());
        PatnaOnRoadExposure patnaOnRoadExposure = new PatnaOnRoadExposure();

        {
            String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/";

            if (writeEmissionEvntsFirst) {
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
                String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
            }
            patnaOnRoadExposure.run(outputDir+"/output/output_events.xml.gz", modeToInhaledMass_bau, linkToInhaledMass_bau);
        }
        {
            // write data
            String outFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/analysis/linkToOnRoadExposure_bau_"+EVENTS_ORDER.toString()+".txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("link\t");
                Set<String> pollutants = new TreeSet<>(linkToInhaledMass_bau.entrySet().iterator().next().getValue().keySet());
                for(String poll : pollutants) {
                    writer.write(poll+"\t");
                }
                writer.newLine();

                for (Id<Link> link : linkToInhaledMass_bau.keySet()) {
                    writer.write(link.toString());
                    for(String poll : pollutants) {
                        writer.write(linkToInhaledMass_bau.get(link).get(poll)+"\t" );
                    }
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);

        }
        {
            String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/BT-b_lastItr/";

            if (writeEmissionEvntsFirst) {
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/";
                String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
            }

            patnaOnRoadExposure.run(outputDir+"/output/output_events.xml.gz", modeToInhaledMass_BSH_b, linkToInhaledMass_BSH_b);
        }
        {
            // write data
            String outFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/analysis/mdoeToOnRoadExposure_"+EVENTS_ORDER.toString()+".txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("mode\tpollutant\tvalue_bau\tvalue_BSH_b\n");
                for (String mode : modeToInhaledMass_bau.keySet()) {
                    for (String emiss : modeToInhaledMass_bau.get(mode).keySet()) {
                        writer.write(mode + "\t" + emiss + "\t" + modeToInhaledMass_bau.get(mode)
                                                                                       .get(emiss) + "\t" + modeToInhaledMass_BSH_b
                                .get(mode)
                                .get(emiss) + "\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }
        {
            // write data
            String outFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/analysis/linkToOnRoadExposure_BSH-B_"+EVENTS_ORDER.toString()+".txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("link\t");
                Set<String> pollutants = new TreeSet<>(linkToInhaledMass_BSH_b.entrySet().iterator().next().getValue().keySet());
                for(String poll : pollutants) {
                    writer.write(poll+"\t");
                }
                writer.newLine();

                for (Id<Link> link : linkToInhaledMass_BSH_b.keySet()) {
                    writer.write(link.toString());
                    for(String poll : pollutants) {
                        writer.write(linkToInhaledMass_BSH_b.get(link).get(poll)+"\t" );
                    }
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }
    }

    private void run(String eventsFile, Map<String, Map<String, Double>> modeToInhaledMass, Map<Id<Link>, Map<String, Double>> linkToInhaledMass){
        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
        onRoadExposureConfigGroup.setUsingMicroGramUnits(false);

        onRoadExposureConfigGroup.getPollutantToBackgroundConcentration().put(WarmPollutant.PM.toString(), 236.0);
        onRoadExposureConfigGroup.getPollutantToBackgroundConcentration().put(WarmPollutant.CO.toString(), 1690.0);
        onRoadExposureConfigGroup.getPollutantToBackgroundConcentration().put(WarmPollutant.NO2.toString(), 93.8);
        onRoadExposureConfigGroup.getPollutantToBackgroundConcentration().put(WarmPollutant.SO2.toString(), 5.1);

        onRoadExposureConfigGroup.getPollutantToPenetrationRate("motorbike");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("truck");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("bike");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("car");

        onRoadExposureConfigGroup.getModeToOccupancy().put("motorbike",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("truck",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("bike",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("car",1.2);

        onRoadExposureConfigGroup.getModeToBreathingRate().put("motorbike",0.66/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("truck",0.66/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("bike",3.06/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("car",0.66/3600.0 );

        EventsManager eventsManager = EventsUtils.createEventsManager();

        // this will include exposure to agent which leave in the same time step.
        OnRoadExposureHandler onRoadExposureHandler = new OnRoadExposureHandler(onRoadExposureConfigGroup, EVENTS_ORDER);
        eventsManager.addHandler(onRoadExposureHandler);

        CombinedMatsimEventsReader eventsReader = new CombinedMatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

       modeToInhaledMass.putAll(onRoadExposureHandler.getOnRoadExposureTable().getModeToInhaledMass());
       linkToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getLinkToInhaledMass();

       onRoadExposureHandler.reset(0);
    }
}
