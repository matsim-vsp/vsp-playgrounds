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

package playground.agarwalamit.fundamentalDiagrams.headwayMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDStabilityTester;
import playground.agarwalamit.utils.ListUtils;

/**
 * Created by amit on 14.05.18.
 */

public class HeadwayHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, IterationEndsListener {

    @Inject
    public HeadwayHandler(Vehicles vehicles, FDNetworkGenerator fdNetworkGenerator, FDStabilityTester stabilityTester, FDDataContainer fdDataContainer, ControlerConfigGroup config) {
        this.vehicles = vehicles;
        this.fdNetworkGenerator = fdNetworkGenerator;
        this.stabilityTester = stabilityTester;
        this.fdDataContainer = fdDataContainer;
        this.config = config;
    }

    private Vehicles vehicles;
    private FDNetworkGenerator fdNetworkGenerator;
    private FDStabilityTester stabilityTester;
    private FDDataContainer fdDataContainer;
    private ControlerConfigGroup config;

    private final Map<String, List<Double>> modeToHeadwayList = new TreeMap<>();
    private final Map<Id<Vehicle>, Double> linkEnterTime = new HashMap<>();

    @Override
    public void reset(int iteration){
        this.linkEnterTime.clear();
        this.modeToHeadwayList.clear();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (event.getLinkId().equals( this.fdNetworkGenerator.getLastLinkIdOfTrack())) {
            double speed = this.fdNetworkGenerator.getLengthOfTrack() / (event.getTime() - this.linkEnterTime.get(event.getVehicleId()));
            Vehicle veh = vehicles.getVehicles().get(event.getVehicleId());
            double headway = getReactionTime() + veh.getType().getLength() / speed ; //it is better to estimate here so that it can be logged too.
            ((AttributableVehicle) vehicles.getVehicles().get(event.getVehicleId())).getAttributes().putAttribute("headway", headway);

            // store headways
            String mode = veh.getType().getId().toString();
            List<Double> headways = this.modeToHeadwayList.get(mode);

            if (headways == null) {
                headways = new ArrayList<>(Collections.nCopies(fdDataContainer.getTravelModesFlowData().get(mode).getSpeedTableSize(), 0.));
            }

            headways.remove(0);//remove from top
            headways.add(headway); // add to end of list
            this.modeToHeadwayList.put(mode, headways);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (event.getLinkId().equals(this.fdNetworkGenerator.getFirstLinkIdOfTrack())) {
            this.linkEnterTime.put(event.getVehicleId(), event.getTime());
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        //arrival only possible once stability is achieved
        String file = config.getOutputDirectory() + "/modeToDynamicHeadways.txt";
        if (event.getIteration()==config.getFirstIteration()){
            if ( new File(file).delete() ){
                FDModule.LOG.warn("Removing existing file: "+file);
            }
        }
        if ( stabilityTester.isStabilityAchieved() ){
            writeResults(file);
        }
    }

    public Map<String, Double> getModeToAverageHeadway(){
        return this.modeToHeadwayList.keySet()
                                     .stream()
                                     .collect(Collectors.toMap(e -> e,
                                             e -> ListUtils.doubleMean(this.modeToHeadwayList.get(e)),
                                             (a, b) -> b));
    }

    private void writeResults(String outFile){
        boolean writeHeaders = ! (new File(outFile).exists());
        try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outFile)) {
            if (writeHeaders) writer.write("streamDensity\tstreamSpeed\tstreamFlow\tmode\theadway\n");
            else{
                FDModule.LOG.warn("Appending data to the existing file.");
            }
            for (String mode : this.modeToHeadwayList.keySet()){
                for (Double d : this.modeToHeadwayList.get(mode)) {
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentDensity()+"\t");
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentAverageVelocity()+"\t");
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentFlow()+"\t");
                    writer.write(mode+"\t"+d+"\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private static double getReactionTime(){
        //TODO could be mode (driver) specific
//        return 0.5 + 0.7 * MatsimRandom.getRandom().nextDouble(); //between 0.5 and 1.2
        return 0.5;
    }
}
