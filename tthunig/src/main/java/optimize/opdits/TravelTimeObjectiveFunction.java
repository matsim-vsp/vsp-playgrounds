/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package optimize.opdits;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.opdyts.MATSimState;

import com.google.inject.Inject;

import analysis.TtTotalTravelTime;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * total travel time as objective function for opdyts
 * 
 * @author tthunig
 */
public class TravelTimeObjectiveFunction implements ObjectiveFunction {

	@Inject private TtTotalTravelTime eventHandlerTT;
	
	@Override
	public double value(SimulatorState state) {
		
		MATSimState matSimState = (MATSimState) state;
        Set<Id<Person>> persons = matSimState.getPersonIdView();
        
        // based on the last iteration where it was selected
        double totalExpectedScore = 0.;
        double totalExpectedTT = 0.; 

        for (Id<Person> personId : persons) {
            Plan plan = matSimState.getSelectedPlan(personId);
            totalExpectedScore += plan.getScore();
            for (PlanElement pe : plan.getPlanElements()) {
            		if (pe instanceof Leg) {
            			totalExpectedTT += ((Leg)pe).getTravelTime();
            		}
            }
        }
//        return -totalExpectedScore;
        return totalExpectedTT;
		
		// we have to use previous total travel time (total travel time of the last measured iteration) here, because reset(...) is called before opdyts calls value(...)
//		return eventHandlerTT.getPreviousTotalTT();
	}

}
