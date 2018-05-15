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
package optimize.cten.convert.SignalPlans2Matsim;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import optimize.cten.convert.SignalPlans2Matsim.data.CtenCrossingSolution;
import optimize.cten.convert.SignalPlans2Matsim.data.FixCrossingSolution;
import optimize.cten.convert.SignalPlans2Matsim.data.FlexCrossingSolution;
import optimize.cten.convert.SignalPlans2Matsim.data.FlexibleLight;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;

/**
 * @author tthunig
 */
public class RunCtenSignalPlans2MatsimConversion {

	// set this based on the parameters in the header of the btu_solution
	private static final int CYCLE_TIME = 90;
	private static final int STEP_TIME = 3;
	
	private static void convertOptimalSignalPlans(String directory, String inputFile) {
		CtenSignalPlanXMLParser solutionParser = new CtenSignalPlanXMLParser();
		solutionParser.readFile(directory + inputFile);
		Map<Id<DgCrossing>, CtenCrossingSolution> crossings = solutionParser.getCrossings();
		
		Scenario scenario = loadScenario(directory);
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		DgIdPool idPool = DgIdPool.readFromFile(directory + "id_conversions.txt");

		convertSignalPlans(crossings, idPool, signalsData, scenario.getNetwork(), scenario.getLanes());
		writeOptimizedSignalControl(directory, inputFile, signalsData);
	}
	
	private static void convertSignalPlans(Map<Id<DgCrossing>, CtenCrossingSolution> crossings, DgIdPool idPool,
			SignalsData signalsData, Network network, Lanes lanes) {
		
		DgIdConverter idConverter = new DgIdConverter(idPool);
		// go through all crossings and fill signal control
		for (CtenCrossingSolution crossing : crossings.values()) {
			// use conflict information to get the system id based on the node id
			// (TODO if no conflict data exists, one would need to iterate over all systems in before and save the relation between system and node id)
			Id<Node> nodeId = idConverter.convertCrossingId2NodeId(crossing.getId());
			Id<SignalSystem> signalSystemId = signalsData.getConflictingDirectionsData().getConflictsPerNode().get(nodeId).getSignalSystemId();
			SignalSystemControllerData systemControl = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(signalSystemId);
			systemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			if (crossing instanceof FlexCrossingSolution) {
				// create a plan
				SignalPlanData plan = signalsData.getSignalControlData().getFactory().createSignalPlanData(Id.create(1, SignalPlan.class));
				systemControl.addSignalPlanData(plan);
				plan.setCycleTime(CYCLE_TIME);
				
				FlexCrossingSolution flexCrossing = (FlexCrossingSolution) crossing;
				for (SignalGroupData signalGroup : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemId).values()) {
					SignalGroupSettingsData groupSetting = signalsData.getSignalControlData().getFactory().createSignalGroupSettingsData(signalGroup.getId());
					plan.addSignalGroupSettings(groupSetting);
					FlexibleLight correspondingLight = getFlexLightForSignalGroup(signalsData, network, lanes, idConverter, flexCrossing, signalSystemId, signalGroup);
					groupSetting.setOnset(correspondingLight.getGreenStart() * STEP_TIME);
					groupSetting.setDropping(correspondingLight.getGreenEnd() * STEP_TIME);
				} 
			} else if (crossing instanceof FixCrossingSolution) {
				// get the existing plan (assumes there is only one)
				SignalPlanData plan = systemControl.getSignalPlanData().values().iterator().next();
				
				FixCrossingSolution fixCrossing = (FixCrossingSolution) crossing;
				plan.setOffset(fixCrossing.getOffset());
			}
		}
	}

	private static FlexibleLight getFlexLightForSignalGroup(SignalsData signalsData, Network network, Lanes lanes,
			DgIdConverter idConverter, FlexCrossingSolution flexCrossing, Id<SignalSystem> signalSystemId,
			SignalGroupData signalGroup) {
		
		SignalData signal = signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId).getSignalData().get(signalGroup.getSignalIds().iterator().next());
		Id<Link> fromLinkId = signal.getLinkId();
		Id<Link> toLinkId = null; 
		FlexibleLight correspondingLight = null;
		
		if (signal.getTurningMoveRestrictions() != null && !signal.getTurningMoveRestrictions().isEmpty()) {
			if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
				throw new RuntimeException("This converter assumes that there are no lanes if turning move restrictions for signals exist. Adapt the converter if this use case is necessary for your scenario.");
			}
			Iterator<Id<Link>> turningMoveRestrictionsIterator = signal.getTurningMoveRestrictions().iterator();
			while (correspondingLight == null) {
				toLinkId = turningMoveRestrictionsIterator.next();
				Id<DgStreet> correspondingLightId = idConverter.convertFromLinkIdToLinkId2LightId(fromLinkId, null, toLinkId);
				correspondingLight = flexCrossing.getLights().get(Id.create(correspondingLightId, DgGreen.class));
				// correspondingGreen stays null until a fromLink-toLink relation has been found for which a light exists in cten (no u-turns)
			}
		} else if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
			Set<Id<Link>> toLinksOfThisSignal = new HashSet<>();
			Iterator<Id<Lane>> lanesIterator = signal.getLaneIds().iterator();
			while (correspondingLight == null && lanesIterator.hasNext()) {
				Id<Lane> fromLaneId = lanesIterator.next();
				Lane lane = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(fromLaneId);
				Iterator<Id<Link>> toLinkIterator = lane.getToLinkIds().iterator();
				while (correspondingLight == null && toLinkIterator.hasNext()) {
					toLinkId = toLinkIterator.next();
					toLinksOfThisSignal.add(toLinkId);
					Id<DgStreet> correspondingLightId = idConverter.convertFromLinkIdToLinkId2LightId(fromLinkId, fromLaneId, toLinkId);
					correspondingLight = flexCrossing.getLights().get(Id.create(correspondingLightId, DgGreen.class));
					// correspondingGreen stays null until a fromLink-toLink relation has been found for which a light exists in cten (no u-turns)
				}
			} if (correspondingLight == null) {
				/* correspondingLight is still null -> no fromLink-fromLane-toLink-Relation of this signal has been found for which a light exists.
				 * Probably, another lane covers the same fromLink-toLink-Relation and was used to create the light. Find it!
				 */
				// go through all other lanes of the fromLink and find one with a fromLink-toLink-Relation that is covered by the signal
				Iterator<Lane> otherLaneIterator = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().values().iterator();
				while (correspondingLight == null) {
					Lane otherLane = otherLaneIterator.next();
					if (signal.getLaneIds().contains(otherLane.getId()) // this lanes where already checked above
							|| otherLane.getToLinkIds() == null || otherLane.getToLinkIds().isEmpty()) { // no outgoing lanes
						continue;
					}
					Iterator<Id<Link>> toLinkIterator = otherLane.getToLinkIds().iterator();
					while (correspondingLight == null && toLinkIterator.hasNext()) {
						toLinkId = toLinkIterator.next();
						if (toLinksOfThisSignal.contains(toLinkId)) {
							Id<DgStreet> correspondingLightId = idConverter.convertFromLinkIdToLinkId2LightId(fromLinkId, otherLane.getId(), toLinkId);
							correspondingLight = flexCrossing.getLights().get(Id.create(correspondingLightId, DgGreen.class));
						}
					}
				}
			}
		} else { // no turning move restrictions and no lanes
			Link fromLink = network.getLinks().get(fromLinkId);
			Iterator<? extends Link> outLinkIterator = fromLink.getToNode().getOutLinks().values().iterator();					
			while (correspondingLight == null) {
				toLinkId = outLinkIterator.next().getId();
				Id<DgStreet> correspondingLightId = idConverter.convertFromLinkIdToLinkId2LightId(fromLinkId, null, toLinkId);
				correspondingLight = flexCrossing.getLights().get(Id.create(correspondingLightId, DgGreen.class));
				// correspondingGreen stays null until a fromLink-toLink relation has been found for which a light exists in cten (no u-turns)
			}
		}
		return correspondingLight;
	}
	
	private static Scenario loadScenario(String directory) {
		Config config = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalConfig = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfig.setUseSignalSystems(true);
		signalConfig.setSignalSystemFile(directory + "output_signal_systems_v2.0.xml");
		signalConfig.setSignalGroupsFile(directory + "output_signal_groups_v2.0.xml");
		signalConfig.setSignalControlFile(directory + "output_signal_control_v2.0.xml");
		signalConfig.setConflictingDirectionsFile(directory + "output_conflicting_directions_v1.0.xml");
		signalConfig.setUseConflictingDirections(true);
		config.network().setLaneDefinitionsFile(directory + "lanes_network_small.xml.gz");
		config.network().setInputFile(directory + "network_small_simplified.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		return scenario;
	}
	
	private static void writeOptimizedSignalControl(String directoryPath, String inputFilename,
			SignalsData signalsData) {
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		String basefilename = inputFilename.substring(inputFilename.lastIndexOf("/")+1,
				inputFilename.lastIndexOf("."));
		String subdirectory = inputFilename.substring(0,inputFilename.lastIndexOf("/")+1);
		
		writer.setSignalSystemsOutputFilename(directoryPath + subdirectory
				+ "signal_systems_" + basefilename + ".xml");
		writer.setSignalGroupsOutputFilename(directoryPath + subdirectory
				+ "signal_groups_" + basefilename + ".xml");
		writer.setSignalControlOutputFilename(directoryPath + subdirectory
				+ "signal_control_" + basefilename + ".xml");
		writer.writeSignalSystemsData(signalsData.getSignalSystemsData());
		writer.writeSignalGroupsData(signalsData.getSignalGroupsData());
		writer.writeSignalControlData(signalsData.getSignalControlData());
	}
	
	public static void main(String[] args) {
		convertOptimalSignalPlans("../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-05-4_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/",
				"btu/btu_solution.xml");
	}

}
