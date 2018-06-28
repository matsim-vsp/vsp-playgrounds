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
package optimize.cten.convert.Restrictions2Matsim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.LanesToLinkAssignment;

/**
 * can be run after a conflicting directions file for matsim was created
 * to validate it with the given base case fixed time signals
 * and change all conflicting directions to non-conflictings that show green at the same time in the base case fixed time plan
 * 
 * @author tthunig
 */
public class AdaptConflictsBasedOnBaseCaseFixedTimePlans {

	private static final String DIR = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	private static final String CONFL_FILE = "conflictData_fromBtu2018-05-03_basedOnMSconflicts_v4.xml";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(DIR + "network_wgs84_utm33n_v4.xml");
		config.network().setLaneDefinitionsFile(DIR + "lanes_v1-4.xml");
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		signalConfigGroup.setConflictingDirectionsFile(DIR + CONFL_FILE);
		signalConfigGroup.setUseConflictingDirections(true);
		signalConfigGroup.setSignalControlFile(DIR + "signal_control_no_13_v4.xml");
		signalConfigGroup.setSignalGroupsFile(DIR + "signal_groups_no_13_v4.xml");
		signalConfigGroup.setSignalSystemFile(DIR + "signal_systems_no_13_v4.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		// go through all conflicting directions and check their signal times in the plan, eventually change them to non-conflicting
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		ConflictData conflictData = signalsData.getConflictingDirectionsData();
		for (IntersectionDirections directions : conflictData.getConflictsPerSignalSystem().values()) {
			Id<SignalSystem> sysId = directions.getSignalSystemId();
			// identify one signal group per direction for this system
			Map<Tuple<Id<Link>, Id<Link>>, Id<SignalGroup>> direction2groupsMap = identifyOneGroupPerDirection(scenario, sysId);
			SignalPlanData signalPlan = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(sysId).getSignalPlanData().values().iterator().next();
			SortedMap<Id<SignalGroup>, SignalGroupSettingsData> fixedTimeSettings = signalPlan.getSignalGroupSettingsDataByGroupId();
			for (Direction direction : directions.getDirections().values()) {
				// identify signal group that the direction belongs to
				Id<SignalGroup> signalGroupId = direction2groupsMap.get(new Tuple<>(direction.getFromLink(), direction.getToLink()));
				
				SignalGroupSettingsData groupSettings = fixedTimeSettings.get(signalGroupId);
				// use iterator to be able to remove elements while iterating
				Iterator<Id<Direction>> iterator = direction.getConflictingDirections().iterator();
				while (iterator.hasNext()) {
					Direction conflictingDir = conflictData.getConflictsPerSignalSystem().get(sysId).getDirections().get(iterator.next());
					// identify signal group that the direction belongs to
					Id<SignalGroup> conflictingSignalGroupId = direction2groupsMap.get(new Tuple<>(conflictingDir.getFromLink(), conflictingDir.getToLink()));
					SignalGroupSettingsData conflictingGroupSettings = fixedTimeSettings.get(conflictingSignalGroupId);
					if (isOverlapping(signalPlan.getCycleTime(), groupSettings, conflictingGroupSettings)) {
						direction.addNonConflictingDirection(conflictingDir.getId());
						iterator.remove();
					}
				}
			}
		}
		
		// write out the modified conflicting directions file
		new ConflictingDirectionsWriter(conflictData).write(DIR + CONFL_FILE.trim().split("\\.")[0] + "_modifiedBasedOnMS.xml");
	}

	private static Map<Tuple<Id<Link>, Id<Link>>, Id<SignalGroup>> identifyOneGroupPerDirection(Scenario scenario, Id<SignalSystem> sysId) {
		Map<Tuple<Id<Link>, Id<Link>>, Id<SignalGroup>> direction2groupsMap = new HashMap<>();
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemData signalSystemData = signalsData.getSignalSystemsData().getSignalSystemData().get(sysId);
		
		// iterate through all groups and assign corresponding directions
		for (SignalGroupData group : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(sysId).values()) {
			for (Id<Signal> signalId : group.getSignalIds()) {
				SignalData signal = signalSystemData.getSignalData().get(signalId);
				if (signal.getTurningMoveRestrictions() != null && !signal.getTurningMoveRestrictions().isEmpty()) {
					// use turning move restrictions
					for (Id<Link> toLink : signal.getTurningMoveRestrictions()) {
						direction2groupsMap.put(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), toLink), group.getId());
					}
				} else if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
					// use lanes
					LanesToLinkAssignment lanesOfThisLink = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId());
					for (Id<Lane> laneId : signal.getLaneIds()) {
						Lane lane = lanesOfThisLink.getLanes().get(laneId);
						for (Id<Link> toLink : lane.getToLinkIds()) {
							direction2groupsMap.put(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), toLink), group.getId());
						}
					}
				} else {
					// no turning move restrictions, no lanes - all possible out links
					Link fromLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
					for (Id<Link> toLink : fromLink.getToNode().getOutLinks().keySet()) {
						direction2groupsMap.put(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), toLink), group.getId());
					}
				}
			}
		}
		
		return direction2groupsMap;
	}

	private static boolean isOverlapping(int cycleTime, SignalGroupSettingsData groupSettings,
			SignalGroupSettingsData conflictingGroupSettings) {
		// fill array with one bucket per second in a cycle. if a buckets is touched twice, the signal plans are overlapping
		boolean[] cycleGreen = new boolean[cycleTime];
		
		// fill array with first green phase
		int dropping = groupSettings.getDropping();
		// shift dropping if green phase includes the cycle shift
		if (dropping < groupSettings.getOnset()) dropping += cycleTime;
		for (int t = groupSettings.getOnset(); t < dropping; t++) {
			cycleGreen[t % cycleTime] = true;
		}
		
		// check overlapping with second green phase
		dropping = conflictingGroupSettings.getDropping();
		// shift dropping if green phase includes the cycle shift
		if (dropping < conflictingGroupSettings.getOnset()) dropping += cycleTime;
		for (int t = conflictingGroupSettings.getOnset(); t < dropping; t++) {
			if (cycleGreen[t % cycleTime] == true)
				return true;
		}
		
		// no overlapping has been found
		return false;
	}

}
