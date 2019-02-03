/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.tschlenther.pave.av;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.data.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * @author tschlenther
 *
 */
public class TSPrivateAVFleetGenerator implements Fleet, BeforeMobsimListener {

	private Map<Id<DvrpVehicle>, DvrpVehicle> vehiclesForIteration;

	private final Population population;
	
	private String mode;

	private Network network;
	
	/**
	 * 
	 */
	public TSPrivateAVFleetGenerator(Scenario scenario) {
		mode = TaxiConfigGroup.get(scenario.getConfig()).getMode();
		this.population= scenario.getPopulation();
		this.network = scenario.getNetwork();
//		this.manager = manager;
		vehiclesForIteration = new HashMap<>();
	}
	
	
	@Override
	public Map<Id<DvrpVehicle>, ? extends DvrpVehicle> getVehicles() {
		return vehiclesForIteration;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		vehiclesForIteration.clear();
		for (Person p : population.getPersons().values()){
			
			Id<Link> vehicleStartLink = null;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(mode)){
						vehicleStartLink = ((Leg) pe).getRoute().getStartLinkId();
						Id<DvrpVehicle> vehicleId = Id.create(p.getId().toString() + "_av", DvrpVehicle.class);
						Link startLink = network.getLinks().get(vehicleStartLink);
						DvrpVehicleSpecification specification = ImmutableDvrpVehicleSpecification.newBuilder()
								.id(vehicleId)
								.startLinkId(startLink.getId())
								.capacity(4)
								.serviceBeginTime((double)0)
								.serviceEndTime((double)(30 * 3600))
								.build();
						DvrpVehicle veh = new DvrpVehicleImpl(specification, startLink);
						vehiclesForIteration.put(veh.getId(), veh);
						break;
					}
				}
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.matsim.contrib.dvrp.data.Fleet#resetSchedules()
	 */
	@Override
	public void resetSchedules() {
		// TODO Auto-generated method stub
		
	}
}
