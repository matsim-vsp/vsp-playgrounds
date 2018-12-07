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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkVolume.ModeFilterLinkVolumeHandler;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 06.06.18.
 */

public class ModalLinkVolumeWriter {


    public static void main(String[] args) {

        String outFile = "../../runs-svn/patnaIndia/run111/onRoadExposure/analysis/linkVols/link2Vol_bau_BSH-b_diff.txt";

        Map<Id<Link>, Double> link2BikeCounts_bau;

        {
            String eventsFile = "../../runs-svn/patnaIndia/run111/onRoadExposure/bauLastItr/output/output_events.xml.gz";

            EventsManager eventsManager = EventsUtils.createEventsManager();

            ModeFilterLinkVolumeHandler handler = new ModeFilterLinkVolumeHandler(Collections.singletonList("bike"));
            eventsManager.addHandler(handler);

            new MatsimEventsReader(eventsManager).readFile(eventsFile);

            link2BikeCounts_bau = handler.getLinkId2TimeSlot2LinkCount().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e-> MapUtils.doubleValueSum( e.getValue())));
        }

        Map<Id<Link>, Double> link2BikeCounts_BSH; //BSH-b

        {
            String eventsFile = "../../runs-svn/patnaIndia/run111/onRoadExposure/BT-b_lastItr/output/output_events.xml.gz";

            EventsManager eventsManager = EventsUtils.createEventsManager();

            ModeFilterLinkVolumeHandler handler = new ModeFilterLinkVolumeHandler(Collections.singletonList("bike"));
            eventsManager.addHandler(handler);

            new MatsimEventsReader(eventsManager).readFile(eventsFile);

            link2BikeCounts_BSH = handler.getLinkId2TimeSlot2LinkCount().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e-> MapUtils.doubleValueSum( e.getValue())));
        }

        try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile) ) {
            writer.write("linkId\tbikeCounts_Diff\n");
            for(Id<Link> linkId : link2BikeCounts_BSH.keySet()) {
                double diff= link2BikeCounts_BSH.get(linkId) - link2BikeCounts_bau.getOrDefault(linkId,0.);
                writer.write(linkId+"\t"+String.valueOf(diff)+"\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

}
