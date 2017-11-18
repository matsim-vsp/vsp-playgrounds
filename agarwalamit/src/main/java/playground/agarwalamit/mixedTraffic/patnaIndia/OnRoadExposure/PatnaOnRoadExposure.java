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
import java.util.Map;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureConfigGroup;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureEventHandler;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;

/**
 * Created by amit on 17.11.17.
 */

public class PatnaOnRoadExposure {

    public static void main(String[] args) {
        Map<String, Map<String, Double>> modeToInhaledMass_bau;
        Map<String, Map<String, Double>> modeToInhaledMass_BSH_b;

        PatnaOnRoadExposure patnaOnRoadExposure = new PatnaOnRoadExposure();

        {
            String eventsFile_bau = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_events.xml.gz";
//            String configFile = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_config.xml.gz";
            modeToInhaledMass_bau = patnaOnRoadExposure.run(eventsFile_bau);


        }
        {
            String eventsFile_BSH_b = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/output_events.xml.gz";
//            String configFile_BSH_b = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/output_config.xml.gz";
            modeToInhaledMass_BSH_b = patnaOnRoadExposure.run(eventsFile_BSH_b);
        }

        // write data
        String outDir = "/Users/amit/Documents/repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/analysis/onRoadExposure.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outDir);
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


    }

    private static Map<String, Map<String, Double>> run(String eventsFile){
        Map<String, Map<String, Double>> modeToInhaledMass;

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
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
        OnRoadExposureEventHandler onRoadExposureEventHandler = new OnRoadExposureEventHandler(onRoadExposureConfigGroup);
        eventsManager.addHandler(onRoadExposureEventHandler);

        CombinedMatsimEventsReader eventsReader = new CombinedMatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

       return  modeToInhaledMass= onRoadExposureEventHandler.getOnRoadExposureTable().getModeToInhaledMass();

    }


}
