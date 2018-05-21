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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.estimation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDStabilityTester;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaMarker;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaRatio;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Created by amit on 29.06.17.
 */

public class ChandraSikdarPCUUpdator implements VehicleEntersTrafficEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler, IterationEndsListener {

    private final Scenario scenario;
    private final Id<Link> trackingStartLink;
    private final Id<Link> trackingEndLink;
    private final double lengthOfTrack ;

    private final Map<Id<Vehicle>,Double> vehicleId2EnterTime = new HashMap<>();
    private final Map<Id<Vehicle>,String> vehicleId2Mode = new HashMap<>();

    private final Map<String, Double> vehicleTypeToLastNotedSpeed = new HashMap<>();
    private final Map<String, Double> vehicleTypeToProjectedAreaRatio = new HashMap<>();

    private final FDDataContainer fdDataContainer;
    private final FDStabilityTester fdStabilityTester;

    @Inject
    public ChandraSikdarPCUUpdator(final Scenario scenario, final FDNetworkGenerator fdNetworkGenerator
    , FDDataContainer fdDataContainer, FDStabilityTester fdStabilityTester){
        this.scenario = scenario;
        this.fdDataContainer = fdDataContainer;
        this.fdStabilityTester = fdStabilityTester;
        this.trackingStartLink = fdNetworkGenerator.getFirstLinkIdOfTrack();
        this.trackingEndLink = fdNetworkGenerator.getLastLinkIdOfTrack();
        this.resetVehicleTypeToSpeedMap();
        this.lengthOfTrack = fdNetworkGenerator.getLengthOfTrack();
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.vehicleId2Mode.put(event.getVehicleId(), event.getNetworkMode());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (event.getLinkId().equals(trackingEndLink)) {
            if (vehicleId2EnterTime.containsKey(event.getVehicleId())) {
                double enterTime = vehicleId2EnterTime.remove(event.getVehicleId());
                double speed = this.lengthOfTrack / ( event.getTime() - enterTime ) ;

                String mode = this.vehicleId2Mode.get(event.getVehicleId());
                this.vehicleTypeToLastNotedSpeed.put(mode, speed);

                double pcu = NumberUtils.round( calculatePCU(mode) , 3);
                scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class)).setPcuEquivalents(pcu);
            } else {
                // link leave after departure event, exclude such agents.
            }
        }
    }

    @Override
    public void reset(int iteration) {
        this.vehicleId2EnterTime.clear();
        this.resetVehicleTypeToSpeedMap();
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (event.getLinkId().equals( trackingStartLink) ) {
            if(vehicleId2EnterTime.containsKey(event.getVehicleId())) {
                throw new RuntimeException("no enter time should be stored. Event: "+ event.toString());
            } else {
                vehicleId2EnterTime.put(event.getVehicleId(), event.getTime());
            }
        }
    }

    private double calculatePCU (final String mode) {
        double speedRatio = this.vehicleTypeToLastNotedSpeed.get(TransportMode.car) / this.vehicleTypeToLastNotedSpeed.get(mode) ;
        double areaRatio = this.vehicleTypeToProjectedAreaRatio.get(TransportMode.car) / this.vehicleTypeToProjectedAreaRatio.get(mode);
        return speedRatio / areaRatio ;
    }

    private void resetVehicleTypeToSpeedMap(){
        for (VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values() ) {
            vehicleTypeToLastNotedSpeed.put(vehicleType.getId().toString(), vehicleType.getMaximumVelocity());

            if (vehicleType.getDescription()==null ||
                    (! vehicleType.getDescription().contains(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA.toString()))  ) {
//                throw new RuntimeException("Vehicle projected area ratio is not provided in the vehicle description. This is required if using dynamic PCU settings. Aborting...");
            FDModule.LOG.warn("Vehicle projecte area ratio is not provided, setting it to default values.");
                vehicleType.setDescription(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA
                        + String.valueOf( VehicleProjectedAreaRatio.getProjectedAreaRatio(vehicleType.getId().toString())  )
                        +VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA);
            }


            int startIndex = vehicleType.getDescription().indexOf(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA.toString()) + VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA.toString().length();
            int endIndex = vehicleType.getDescription().lastIndexOf(VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA.toString());

            double projectedAreaRatio = Double.valueOf( vehicleType.getDescription().substring(startIndex, endIndex) );

            vehicleTypeToLastNotedSpeed.put(vehicleType.getId().toString(), vehicleType.getMaximumVelocity());
            vehicleTypeToProjectedAreaRatio.put(vehicleType.getId().toString(), projectedAreaRatio);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        //arrival only possible once stability is achieved
        if (this.fdStabilityTester.isStabilityAchieved() ){
            writeResults(this.scenario.getConfig().controler().getOutputDirectory() + "/modeToDynamicPCUs.txt");
        }
    }

    private void writeResults(String outFile){
        boolean writeHeaders = !(new File(outFile).exists());
        try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outFile)) {
            if (writeHeaders) {
                writer.write("density\tspeed\tflow\t");
                for (VehicleType vt : scenario.getVehicles().getVehicleTypes().values()) {
                    writer.write("pcu_" + vt.getId().toString()+"\t");
                }
                writer.newLine();
            } else{
                FDModule.LOG.warn("Appending data to the existing file.");
            }
            writer.write(this.fdDataContainer.getGlobalData().getPermanentDensity()+"\t");
            writer.write(this.fdDataContainer.getGlobalData().getPermanentAverageVelocity()+"\t");
            writer.write(this.fdDataContainer.getGlobalData().getPermanentFlow()+"\t");
            for (VehicleType vt : scenario.getVehicles().getVehicleTypes().values()) {
                writer.write( vt.getPcuEquivalents()+"\t");
            }
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
