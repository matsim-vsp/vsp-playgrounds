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

package playground.agarwalamit.analysis.speed.tripSpeed;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 20.04.18.
 */

public class Trip {

    private final Id<Person> personId;
    private final String mode;
    private final double tripStartTime;
    private final Id<Link> departureLink;
    private final int tripIndex;

    private Id<Vehicle> vehicleId;
    private double tripEndTime = 0.;
    private double distance = 0; //m
    private double travelTime = 0.; // sec
    private double speed = 0.; //m_per_s
    private Id<Link> arrivalLink ;

    //links for network modes only
    private final List<Id<Link>> travelledLinks = new ArrayList<>();

    public Trip (Id<Person> personId, String mode, double departureTime, Id<Link> departureLink, int index){
        this.personId = personId;
        this.mode = mode;
        this.tripStartTime = departureTime;
        this.departureLink = departureLink;
        this.tripIndex = index;
    }

    public void arrival(Id<Link> arrivalLink, double arrivalTime){
        this.arrivalLink = arrivalLink;
        this.tripEndTime = arrivalTime;
    }

    /**
     * @return travel time in sec.
     */
    public double getTravelTime(){
        return this.tripStartTime - this.tripStartTime;
    }

    /**
     * @return distance in meters.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return average speed in m/s
     */
    public double getAverageSpeed() {
        return getDistance() / getTravelTime();
    }

    public void travelledOn(Id<Link> linkId, double length){
        this.travelledLinks.add(linkId);
        this.distance += length;
    }

    public void teleportation(double time, double distance){
        this.tripEndTime = time;
        this.distance = distance;
    }

    public void setVehicleId(Id<Vehicle> vehicleId) {
        this.vehicleId = vehicleId;
    }

    // ===================================================================
    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public Id<Person> getPersonId() {
        return personId;
    }

    public String getTripMode() {
        return mode;
    }

    public double getTripStartTime() {
        return tripStartTime;
    }

    public Id<Link> getDepartureLink() {
        return departureLink;
    }

    public double getTripEndTime() {
        return tripEndTime;
    }

    public Id<Link> getArrivalLink() {
        return arrivalLink;
    }

    public List<Id<Link>> getTravelledLinks() {
        return travelledLinks;
    }

    public int getTripIndex() {
        return tripIndex;
    }
}
