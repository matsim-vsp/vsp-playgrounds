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

import java.util.HashMap;
import java.util.Map;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaMarker;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Created by amit on 29.06.17.
 */

public class ChandraSikdarPCUUpdator implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

    private final Scenario scenario;
    private final Id<Link> trackingStartLink;
    private final Id<Link> trackingEndLink;
    private final double lengthOfTrack ;

    private final Map<Id<Vehicle>,Double> vehicleId2EnterTime = new HashMap<>();
    private final Map<Id<Vehicle>,String> vehicleId2Mode = new HashMap<>();

    private final Map<String, Double> vehicleTypeToLastNotedSpeed = new HashMap<>();
    private final Map<String, Double> vehicleTypeToProjectedAreaRatio = new HashMap<>();

    public ChandraSikdarPCUUpdator(final Scenario scenario, final Id<Link> trackingStartLink, final Id<Link> trackingEndLink, final double lengthOfTrack){
        this.scenario = scenario;
        this.trackingStartLink = trackingStartLink;
        this.trackingEndLink = trackingEndLink;
        this.resetVehicleTypeToSpeedMap();
        this.lengthOfTrack = lengthOfTrack;
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
                throw new RuntimeException("Vehicle projected area ratio is not provided in the vehicle description. This is required if using dynamic PCU settings. Aborting...");
            }

            int startIndex = vehicleType.getDescription().indexOf(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA.toString()) + VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA.toString().length();
            int endIndex = vehicleType.getDescription().lastIndexOf(VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA.toString());

            double projectedAreaRatio = Double.valueOf( vehicleType.getDescription().substring(startIndex, endIndex) );

            vehicleTypeToLastNotedSpeed.put(vehicleType.getId().toString(), vehicleType.getMaximumVelocity());
            vehicleTypeToProjectedAreaRatio.put(vehicleType.getId().toString(), projectedAreaRatio);
        }
    }


}
