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
import java.util.TreeMap;
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
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDStabilityTester;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.PCUMethod;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaRatio;
import playground.agarwalamit.fundamentalDiagrams.headwayMethod.HeadwayHandler;
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

    private final FDDataContainer fdDataContainer;
    private final FDStabilityTester fdStabilityTester;

    @Inject(optional=true)
    private PCUMethod pcuMethod = PCUMethod.SPEED_AREA_RATIO;

    private HeadwayHandler delegate;
    private final Map<String, VehicleTypeToPCU> modeToPCU = new TreeMap<>();
    private final double qsimDefaultHeadway;

    //a local field, should be configurable
    private boolean updatePCU =false;

    @Inject
    public ChandraSikdarPCUUpdator(final Scenario scenario, final FDNetworkGenerator fdNetworkGenerator
    , FDDataContainer fdDataContainer, FDStabilityTester fdStabilityTester, FDConfigGroup fdConfigGroup){
        this.scenario = scenario;
        this.fdDataContainer = fdDataContainer;
        this.fdStabilityTester = fdStabilityTester;
        this.trackingStartLink = fdNetworkGenerator.getFirstLinkIdOfTrack();
        this.trackingEndLink = fdNetworkGenerator.getLastLinkIdOfTrack();
        this.lengthOfTrack = fdNetworkGenerator.getLengthOfTrack();
        this.delegate = new HeadwayHandler(scenario.getVehicles(), fdNetworkGenerator, fdStabilityTester, fdDataContainer, scenario.getConfig().controler());
        this.qsimDefaultHeadway = 3600. / fdConfigGroup.getTrackLinkCapacity();
    }

    @Override
    public void reset(int iteration) {
        this.delegate.reset(iteration);
        this.vehicleId2EnterTime.clear();
        this.modeToPCU.clear();
        this.vehicleId2Mode.clear();
        this.vehicleTypeToLastNotedSpeed.clear();
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.vehicleId2Mode.put(event.getVehicleId(), event.getNetworkMode());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        this.delegate.handleEvent(event);
        if (event.getLinkId().equals(trackingEndLink)) {
            if (vehicleId2EnterTime.containsKey(event.getVehicleId())) {
                double enterTime = vehicleId2EnterTime.remove(event.getVehicleId());
                double speed = this.lengthOfTrack / ( event.getTime() - enterTime ) ;

                String mode = this.vehicleId2Mode.get(event.getVehicleId());
                this.vehicleTypeToLastNotedSpeed.put(mode, speed);

                {
                    // AREA_SPEED_RATIO_method
                    double pcu = NumberUtils.round(calculateAreaSpeedPCU(mode), 3);
                    addVehicleTypeToPCU(mode, PCUMethod.SPEED_AREA_RATIO, pcu);
                }
                {
                    // AREA_SPEED_RATIO_method
                    double pcu = NumberUtils.round(calculateHeadwayPCU(mode), 3);
                    addVehicleTypeToPCU(mode, PCUMethod.HEADWAY_RATIO, pcu);

                }
                if (updatePCU) {
                    scenario.getVehicles()
                            .getVehicleTypes()
                            .get(Id.create(mode, VehicleType.class))
                            .setPcuEquivalents(modeToPCU.get(mode).pcuMethodToPCU.get(pcuMethod));
                }
            } else {
                // link leave after departure event, exclude such agents.
            }
        }
    }

    private void addVehicleTypeToPCU(String mode, PCUMethod pcuMethod, double pcu){
        VehicleTypeToPCU vehicleTypeToPCU=  this.modeToPCU.getOrDefault(mode, new VehicleTypeToPCU(mode));
        vehicleTypeToPCU.pcuMethodToPCU.put(pcuMethod,pcu);
        modeToPCU.put(mode, vehicleTypeToPCU);
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        this.delegate.handleEvent(event);
        if (event.getLinkId().equals( trackingStartLink) ) {
            if(vehicleId2EnterTime.containsKey(event.getVehicleId())) {
                throw new RuntimeException("no enter time should be stored. Event: "+ event.toString());
            } else {
                vehicleId2EnterTime.put(event.getVehicleId(), event.getTime());
            }
        }
    }

    private double calculateHeadwayPCU(final String mode){
        Map<String, Double> headwayMap = this.delegate.getModeToAverageHeadway();
        return headwayMap.getOrDefault(mode, qsimDefaultHeadway) / headwayMap.getOrDefault(TransportMode.car, qsimDefaultHeadway);
    }

    private double calculateAreaSpeedPCU(final String mode) {
        double speedRatio = this.vehicleTypeToLastNotedSpeed.getOrDefault(TransportMode.car,
                scenario.getVehicles()
                        .getVehicleTypes()
                        .get(Id.create(TransportMode.car, VehicleType.class))
                        .getMaximumVelocity()) / this.vehicleTypeToLastNotedSpeed.getOrDefault(mode,
                scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class)).getMaximumVelocity());
        double areaRatio = VehicleProjectedAreaRatio.getProjectedAreaRatio(TransportMode.car) / VehicleProjectedAreaRatio.getProjectedAreaRatio(mode);
        return speedRatio / areaRatio ;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        this.delegate.notifyIterationEnds(event);
        //arrival only possible once stability is achieved
        String file = this.scenario.getConfig().controler().getOutputDirectory() + "/modeToDynamicPCUs.txt";
        if (event.getIteration()==this.scenario.getConfig().controler().getFirstIteration()){
            if ( new File(file).delete() ){
                FDModule.LOG.warn("Removing existing file: "+file);
            }
        }
        if (this.fdStabilityTester.isStabilityAchieved() ){
            writeResults(file);
        }
    }

    private void writeResults(String outFile){
        boolean writeHeaders = !(new File(outFile).exists());
        try (BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outFile)) {
            if (writeHeaders) {
                writer.write("streamDensity\tstreamSpeed\tstreamFlow\tmode\tpcu_method\tpcu_value\n");
            } else{
                FDModule.LOG.warn("Appending data to the existing file.");
            }
            for (String mode :this.modeToPCU.keySet()) {
                for (PCUMethod pcuMethod : PCUMethod.values()){
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentDensity()+"\t");
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentAverageVelocity()+"\t");
                    writer.write(this.fdDataContainer.getGlobalData().getPermanentFlow()+"\t");
                    writer.write(mode+"\t");
                    writer.write(pcuMethod+"\t");
                    writer.write(this.modeToPCU.get(mode).pcuMethodToPCU.get(pcuMethod)+"\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
