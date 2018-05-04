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

package playground.agarwalamit.fundamentalDiagrams.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * SSix, Amit
 */

public class FDTrackMobsimAgent implements MobsimAgent, MobsimDriverAgent {

    private final Id<Link> firstLinkIdOfMiddelBranchOfTrack;
    private final Id<Link> lastLinkIdOfBase;
    private final Id<Link> lastLinkIdOfTrack;
    private final Id<Link> firstLinkLinkIdOfBase;
    private final Id<Link> originLinkId;
    private final Id<Link> destinationLinkId;

    private final Id<Person> personId;
    private final Id<Vehicle> plannedVehicleId;
    private final String mode;
    private final double actEndTime;

    private MobsimVehicle vehicle ;
    private boolean isArriving= false;

    private Id<Link> currentLinkId ;
    private MobsimAgent.State agentState= State.ACTIVITY;

    private GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;

    public FDTrackMobsimAgent(Id<Person> agentId, double actEndTime, String travelMode, FDNetworkGenerator fdNetworkGenerator) {
        personId = agentId;
        mode = travelMode;
        this.actEndTime = actEndTime;
        this.plannedVehicleId = Id.create(agentId, Vehicle.class);

        firstLinkIdOfMiddelBranchOfTrack = fdNetworkGenerator.getFirstLinkIdOfMiddleLinkOfTrack();
        lastLinkIdOfBase = fdNetworkGenerator.getLastLinkIdOfBase();
        lastLinkIdOfTrack = fdNetworkGenerator.getLastLinkIdOfTrack();
        firstLinkLinkIdOfBase =  fdNetworkGenerator.getFirstLinkIdOfTrack();
        originLinkId = fdNetworkGenerator.getTripDepartureLinkId();
        destinationLinkId = fdNetworkGenerator.getTripArrivalLinkId();

        this.currentLinkId = originLinkId;
    }

    public void setGlobalFlowDynamicsUpdator(GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator){
        this.globalFlowDynamicsUpdator = globalFlowDynamicsUpdator;
    }

    @Override
    public Id<Link> getCurrentLinkId() {
        return this.currentLinkId;
    }

    @Override
    public Id<Link> getDestinationLinkId() {
        return destinationLinkId;
    }

    @Override
    public Id<Person> getId() {
        return this.personId;
    }

    @Override
    public Id<Link> chooseNextLinkId() {

        if (this.globalFlowDynamicsUpdator==null){
            throw new RuntimeException("No global flow dynamics updator is available.");
        } else if (this.globalFlowDynamicsUpdator.isPermanent()){
            isArriving = true;
        }

        if( lastLinkIdOfTrack.equals(this.currentLinkId) || originLinkId.equals(this.currentLinkId)){
            //person departing from home OR last link of the track
            return firstLinkLinkIdOfBase;
        } else if(lastLinkIdOfBase.equals(this.currentLinkId)){
            if ( isArriving) {
                return destinationLinkId;
            } else {
                return firstLinkIdOfMiddelBranchOfTrack;
            }
        }  else if (destinationLinkId.equals(this.currentLinkId)){
            return null;// this will send agent for arrival
        } else {
            // TODO: if the link ids are not consecutive numbers, this will not work.
            Id<Link> existingLInkId = this.currentLinkId;
            return Id.createLinkId(Integer.valueOf(existingLInkId.toString())+1);
        }
    }

    @Override
    public void notifyMoveOverNode(Id<Link> newLinkId) {
        this.currentLinkId = newLinkId;
    }

    @Override
    public boolean isWantingToArriveOnCurrentLink() {
        return this.chooseNextLinkId()==null ;
    }

    @Override
    public void setVehicle(MobsimVehicle veh) {
        this.vehicle = veh ;
    }

    @Override
    public MobsimVehicle getVehicle() {
        return this.vehicle ;
    }

    @Override
    public Id<Vehicle> getPlannedVehicleId() {
        return this.plannedVehicleId;
    }

    @Override
    public State getState() {
        return agentState;
    }

    @Override
    public double getActivityEndTime() {
        if(isArriving && this.agentState.equals(State.ACTIVITY)) {
            return Double.POSITIVE_INFINITY; // let agent go to sleep.
        }
        return this.actEndTime;
    }

    @Override
    public void endActivityAndComputeNextState(double now) {
        agentState= State.LEG;
    }

    @Override
    public void endLegAndComputeNextState(double now) {
        agentState= State.ACTIVITY;
    }

    @Override
    public void setStateToAbort(double now) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Double getExpectedTravelTime() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Double getExpectedTravelDistance() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Facility<? extends Facility<?>> getCurrentFacility() {
        throw new RuntimeException("not implemented") ;
    }

    @Override
    public Facility<? extends Facility<?>> getDestinationFacility() {
        throw new RuntimeException("not implemented") ;
    }
}
