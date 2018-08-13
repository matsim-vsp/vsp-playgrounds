/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesWriter;

import java.util.SortedMap;

/**
 * double flow capacities of all signalized links and lanes
 * 
 * @author tthunig
 *
 */
public class ModifyNetwork {
	
	private static final String INPUT_BASE_DIR = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v2.xml.gz");
		config.qsim().setUseLanes( true );
		config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v2.1.xml");
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v2.1.xml");
		signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_v2.xml");
		signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13_v2.xml");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
	
		doubleFlowCapOfSignalizedLinksAndLanes(scenario);
		
		new NetworkWriter(scenario.getNetwork()).write(INPUT_BASE_DIR + "network_wgs84_utm33n_v3.xml");
		new LanesWriter(scenario.getLanes()).write(INPUT_BASE_DIR + "lanes_v3.xml");
	}
	
	public static void doubleFlowCapOfSignalizedLinksAndLanes(Scenario scenario){
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		
		// iterate over all signal systems. double flow capacities of signalized links and lanes
		for (SignalSystemData ssd : signalSystems.getSignalSystemData().values()){
			for (SignalData sd : ssd.getSignalData().values()){
				Link signalizedLink = scenario.getNetwork().getLinks().get(sd.getLinkId());
				signalizedLink.setCapacity(signalizedLink.getCapacity() * 2);
				if (sd.getLaneIds() != null) {
					SortedMap<Id<Lane>, Lane> lanesOfThisLink = scenario.getLanes().getLanesToLinkAssignments().get(signalizedLink.getId()).getLanes();
					for (Id<Lane> signalizedLaneId : sd.getLaneIds()) {
						Lane signalizedLane = lanesOfThisLink.get(signalizedLaneId);
						signalizedLane.setCapacityVehiclesPerHour(signalizedLane.getCapacityVehiclesPerHour() * 2);
					}
				}
			}
		}
	}
	
	public static void lengthenAllLanes(Scenario scenario){
		for (LanesToLinkAssignment l2l : scenario.getLanes().getLanesToLinkAssignments().values()){
			Link link = scenario.getNetwork().getLinks().get(l2l.getLinkId());
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getStartsAtMeterFromLinkEnd() == link.getLength()){
					// original lane does not have to be extended
					continue;
				}
				// maximal lane length, should be >0
				double maxLaneLength = Math.max(link.getLength()-5, 1);
				// laemmer signal control needs to look up to 10 seconds into the future
				double desiredLaneLength = link.getFreespeed() * 10;
				if (lane.getStartsAtMeterFromLinkEnd() < desiredLaneLength) {
					// when link is long enough, set lane length such that agents travel on the lane for 10 seconds
					// (also see MA Nico Kuehnel, 2017. He used slightly different values but the same logic)
					lane.setStartsAtMeterFromLinkEnd(Math.min(maxLaneLength, desiredLaneLength));
				}
			}
		}
	}

}
