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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.headwayMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramDataGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.StabilityTester;

/**
 * Created by amit on 14.05.18.
 */

public class HeadwayHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, IterationEndsListener {

    @Inject private Vehicles vehicles;
    @Inject private FDNetworkGenerator fdNetworkGenerator;
    @Inject private StabilityTester stabilityTester;
    @Inject private FDDataContainer fdDataContainer;
    @Inject private ControlerConfigGroup config;

    private final Map<String, List<Double>> modeToPCUList = new TreeMap<>();
    private final Map<Id<Vehicle>, Double> linkEnterTime = new HashMap<>();

    @Override
    public void reset(int iteration){
        this.linkEnterTime.clear();
        this.modeToPCUList.clear();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (event.getLinkId().equals( this.fdNetworkGenerator.getLastLinkIdOfTrack())) {
            double speed = this.fdNetworkGenerator.getLengthOfTrack() / (event.getTime() - this.linkEnterTime.get(event.getVehicleId()));
            Vehicle veh = vehicles.getVehicles().get(event.getVehicleId());
            double pcu = getReactionTime() + veh.getType().getLength() / speed ; //it is better to estimate here so that it can be logged too.
            ((AttributableVehicle) vehicles.getVehicles().get(event.getVehicleId())).getAttributes().putAttribute("vehicle_pcu", pcu);

            // store PCUs
            String mode = veh.getType().getId().toString();
            List<Double> pcus = this.modeToPCUList.get(mode);

            if (pcus == null) {
                pcus = new ArrayList<>(Collections.nCopies(fdDataContainer.getTravelModesFlowData().get(mode).getSpeedTableSize(), 0.));
            }

            pcus.remove(0);//remove from top
            pcus.add(pcu); // add to end of list
            this.modeToPCUList.put(mode, pcus);
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
        if ( stabilityTester.isStabilityAchieved() ){
            writeResults(config.getOutputDirectory() + "/modeToDynamicHeadways.txt");
        }
    }

    private void writeResults(String outFile){
        boolean writeHeaders = !(new File(outFile).exists());
        try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outFile)) {
            if (writeHeaders) writer.write("density\tspeed\tflow\tmode\theadway\n");
            else{
                FundamentalDiagramDataGenerator.LOG.warn("Appending data to the existing file.");
            }
            for (String mode : this.modeToPCUList.keySet()){
                for (Double d : this.modeToPCUList.get(mode)) {
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
        return 0.5;
    }
}
