/* *********************************************************************** *
 * project: org.matsim.*
 * FundamentalDiagramsNmodes											   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.VehicleType;

/**
 * @author ssix, amit
 * A class supposed to go attached to the {@link FundamentalDiagramDataGenerator}.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average
 * velocity for each velocity group.
 */

public final class GlobalFlowDynamicsUpdator implements
		LinkEnterEventHandler, PersonDepartureEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final Map<Id<Person>, String> person2Mode = new HashMap<>();
	private final Id<Link> flowDynamicsUpdateLink;
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final FDDataContainer fdDataContainer;
	private final StabilityTester stabilityTester;

	/**
	 * container to store static properties of vehicles and dynamic flow properties during simulation
	 */
	@Inject
	GlobalFlowDynamicsUpdator(Scenario scenario, FDDataContainer fdDataContainer,
							  StabilityTester stabilityTester, FDNetworkGenerator fdNetworkGenerator){

		this.fdDataContainer = fdDataContainer;
		this.stabilityTester = stabilityTester;

		Collection<String> mainModes = scenario.getConfig().qsim().getMainModes();

		mainModes.forEach(mode -> {
			VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
			this.fdDataContainer.getTravelModesFlowData()
								.put(vehicleType.getId().toString(),
										new TravelModesFlowDynamicsUpdator(vehicleType, scenario, fdNetworkGenerator));
		});

		this.fdDataContainer.setGlobalData(new TravelModesFlowDynamicsUpdator(null, scenario, fdNetworkGenerator));
		this.fdDataContainer.getGlobalData().resetBins();
		this.flowDynamicsUpdateLink = fdNetworkGenerator.getFirstLinkIdOfTrack();
	}

	@Override
	public void reset(int iteration) {		
		this.delegate.reset(iteration);
		this.person2Mode.clear();

		this.fdDataContainer.getTravelModesFlowData().values().forEach(TravelModesFlowDynamicsUpdator::resetBins);
		this.fdDataContainer.getGlobalData().resetBins();

		this.stabilityTester.setStabilityAchieved(false);
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		person2Mode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!(this.stabilityTester.isStabilityAchieved())){
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			String travelMode = person2Mode.get(personId);

			this.fdDataContainer.getTravelModesFlowData().get(travelMode).handle(event);
			double pcuPerson = this.fdDataContainer.getTravelModesFlowData().get(travelMode).getVehicleType().getPcuEquivalents();

			//Aggregated data update
			double nowTime = event.getTime();
			if (event.getLinkId().equals(flowDynamicsUpdateLink)){
				this.fdDataContainer.getGlobalData().updateFlow15Min(nowTime, pcuPerson);
				this.fdDataContainer.getGlobalData().updateSpeedTable(nowTime,personId);
				//Waiting for all agents to be on the track before studying stability
				if ((this.fdDataContainer.getGlobalData().getNumberOfDrivingAgents() == this.fdDataContainer.getGlobalData().getnumberOfAgents()) && (nowTime > FundamentalDiagramDataGenerator.MAX_ACT_END_TIME * 2)){
					/*//Taking speed check out, as it is not reliable on the global speed table
					 *  Maybe making a list of moving averages could be smart,
					 *  but there is no reliable converging process even in that case. (ssix, 25.10.13)
					 * if (!(this.globalData.isSpeedStable())){
						this.globalData.checkSpeedStability();
						System.out.println("Checking speed stability in global data for: "+this.globalData.getSpeedTable());
					}*/
					if (!(this.fdDataContainer.getGlobalData().isFlowStable())){
						this.fdDataContainer.getGlobalData().checkFlowStability15Min();
					}

					//Checking modes stability
					boolean modesStable = true;
					for (String vehTyp : fdDataContainer.getTravelModesFlowData().keySet()){
						if (this.fdDataContainer.getTravelModesFlowData().get(vehTyp).getnumberOfAgents() != 0){
							if (! this.fdDataContainer.getTravelModesFlowData().get(vehTyp).isSpeedStable() || ! this.fdDataContainer.getTravelModesFlowData().get(vehTyp).isFlowStable() ) {
								modesStable = false;
								break;
							}
						}
					}
					if (modesStable){
						//Checking global stability
						if ( /*this.globalFlowData.isSpeedStable() &&*/ this.fdDataContainer.getGlobalData().isFlowStable() ){
							FundamentalDiagramDataGenerator.LOG.info("========== Global permanent regime is attained");
							for (String vehTyp : fdDataContainer.getTravelModesFlowData().keySet()){
								this.fdDataContainer.getTravelModesFlowData().get(vehTyp).saveDynamicVariables();
							}
							this.fdDataContainer.getGlobalData().setPermanentAverageVelocity(this.fdDataContainer.getGlobalData().getActualAverageVelocity());
							this.fdDataContainer.getGlobalData().setPermanentFlow(this.fdDataContainer.getGlobalData().getCurrentHourlyFlow());
							double globalDensity = this.fdDataContainer.getTravelModesFlowData().values()
																		   .stream()
																		   .mapToDouble(TravelModesFlowDynamicsUpdator::getPermanentDensity)
																		   .sum();
							this.fdDataContainer.getGlobalData().setPermanentDensity(globalDensity);
							this.stabilityTester.setStabilityAchieved(true);
							//How to: end simulation immediately? => solved by granting the mobsim agents access to permanentRegime
							//and making them exit the simulation as soon as permanentRegime is true.
						}
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
		this.fdDataContainer.getTravelModesFlowData().get(person2Mode.get(event.getPersonId())).handle(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
		this.fdDataContainer.getTravelModesFlowData().get(person2Mode.remove(event.getPersonId())).handle(event);
	}

//	public boolean isPermanent(){
//		return permanentRegime;
//	}

//	public TravelModesFlowDynamicsUpdator getGlobalData(){
//		return this.fdDataContainer.getGlobalData();
//	}

//	public int getSpeedTableSie(String mode){
//		return this.fdDataContainer.getTravelModesFlowData().get(mode).getSpeedTableSize();
//	}

//	Map<String, TravelModesFlowDynamicsUpdator> getTravelModesFlowData() {
//		return fdDataContainer.getTravelModesFlowData();
//	}
}