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
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
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
    private static final boolean writeEmissionEventsFirst = false;

    public static void main(String[] args) {

        PatnaOnRoadExposure patnaOnRoadExposure = new PatnaOnRoadExposure();

        {
            String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/";

            if (writeEmissionEventsFirst) {
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
                String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
            }
            patnaOnRoadExposure.run(outputDir+"/output/output_events.xml.gz", outputDir+"/analysis/");
        }
        {
            String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/BT-b_lastItr/";

            if (writeEmissionEventsFirst) {
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/";
                String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
            }

            patnaOnRoadExposure.run(outputDir+"/output/output_events.xml.gz",outputDir+"/analysis/");
        }
    }

    private void run(String eventsFile, String outputFilesDir){
        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
        onRoadExposureConfigGroup.setUsingMicroGramUnits(true);

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
        OnRoadExposureHandler onRoadExposureHandler = new OnRoadExposureHandler(onRoadExposureConfigGroup);
        eventsManager.addHandler(onRoadExposureHandler);

        CombinedMatsimEventsReader eventsReader = new CombinedMatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

        TreeSet<String> pollutants = new TreeSet<>(Arrays.stream(WarmPollutant.values())
                                                         .map(e -> e.toString())
                                                         .collect(Collectors.toList()));

        {
            Map<String, Map<String, Double>> modeToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getModeToInhaledMass();
            String outFile = outputFilesDir+"/modeToOnRoadExposure.txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("mode\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (String mode : modeToInhaledMass.keySet()) {
                    writer.write(mode+"\t");
                    for (String poll : pollutants){
                        writer.write( modeToInhaledMass.get(mode).get(poll) + "\t");
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
            Map<Id<Person>, Map<String, Double>> personToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getPersonToInhaledMass();
            String outFile = outputFilesDir+"/personToOnRoadExposure.txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("personId\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (Id<Person> personId : personToInhaledMass.keySet()) {
                    writer.write(personId+"\t");
                    for (String poll : pollutants){
                        writer.write( personToInhaledMass.get(personId).get(poll) + "\t");
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
            Map<Id<Link>, Map<String, Double>> linkToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getLinkToInhaledMass();
            String outFile = outputFilesDir+"/linkToOnRoadExposure.txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("linkId\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (Id<Link> linkId : linkToInhaledMass.keySet()) {
                    writer.write(linkId+"\t");
                    for (String poll : pollutants){
                        writer.write( linkToInhaledMass.get(linkId).get(poll) + "\t");
                    }
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }
       onRoadExposureHandler.reset(0);
    }
}
