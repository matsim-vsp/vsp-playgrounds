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
package signals.laemmer.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

/**
 * @author tthunig based on code by Pierre Schade for his master thesis at VSP 2017
 */
class SignalCombinationBasedOnConflicts {
	
	private static final Logger log = Logger.getLogger(SignalCombinationBasedOnConflicts.class);
	
	private SignalSystemData systemData;
	private SignalSystem system;
	private IntersectionDirections intersectionDirections;

	private Map<Id<Signal>, Set<Tuple<Id<Link>, Id<Link>>>> setOfLinkTuplesPerSignal = new HashMap<>();
	private Map<Id<SignalGroup>, Set<Direction>> setOfDirectionsPerGroup = new HashMap<>();

	public SignalCombinationBasedOnConflicts(SignalsData signalsData, SignalSystem system, Network network, Lanes lanes) {
		this.systemData = signalsData.getSignalSystemsData().getSignalSystemData().get(system.getId());
		this.system = system;
		if (signalsData.getConflictingDirectionsData() == null || !signalsData.getConflictingDirectionsData()
				.getConflictsPerSignalSystem().containsKey(system.getId())) {
			throw new RuntimeException("No conflict data is specified for system " + system.getId()
					+ ". Signal phases cannot be created.");
		}
		this.intersectionDirections = signalsData.getConflictingDirectionsData().getConflictsPerSignalSystem()
				.get(system.getId());

		// remember relation of signals to directions
		for (SignalData signal : this.systemData.getSignalData().values()) {
			setOfLinkTuplesPerSignal.put(signal.getId(), new HashSet<>());
			if (signal.getTurningMoveRestrictions() != null && !signal.getTurningMoveRestrictions().isEmpty()) {
				// use turning move restrictions if possible
				for (Id<Link> nextLinkId : signal.getTurningMoveRestrictions()) {
					setOfLinkTuplesPerSignal.get(signal.getId())
							.add(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), nextLinkId));
				}
			} else if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
				// if no turning move restrictions exist, use lane information
				for (Id<Lane> signalizedLaneId : signal.getLaneIds()) {
					Lane signalizedLane = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes()
							.get(signalizedLaneId);
					for (Id<Link> toLinkId : signalizedLane.getToLinkIds()) {
						setOfLinkTuplesPerSignal.get(signal.getId())
								.add(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), toLinkId));
					}
				}
			} else {
				// no turning move restrictions and no lanes exist. all outgoing links of the
				// node are possible next links
				for (Id<Link> outgoingLinkId : network.getLinks().get(signal.getLinkId()).getToNode().getOutLinks()
						.keySet()) {
					setOfLinkTuplesPerSignal.get(signal.getId())
							.add(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), outgoingLinkId));
				}
			}
		}

		// group directions to signal groups
		for (SignalGroupData group : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(system.getId())
				.values()) {
			setOfDirectionsPerGroup.put(group.getId(), new HashSet<>());
			for (Id<Signal> signalIdOfThisGroup : signalsData.getSignalGroupsData()
					.getSignalGroupDataBySystemId(system.getId()).get(group.getId()).getSignalIds()) {
				for (Tuple<Id<Link>, Id<Link>> from2toLink : setOfLinkTuplesPerSignal.get(signalIdOfThisGroup)) {
					Direction direction = intersectionDirections.getDirection(from2toLink.getFirst(), from2toLink.getSecond());
					// direction is null, if it does not exist (e.g. for u-turns in the cottbus scenario)
					if (direction != null) {
						setOfDirectionsPerGroup.get(group.getId()).add(direction);
					}
				}
			}
			log.info("Group " + group.getId() + " corresponds to " + setOfDirectionsPerGroup.get(group.getId()).size()
					+ " directions.");
		}
	}
	
	private ArrayList<ArrayList<SignalGroup>> createAllValidSignalGroupCombinations() {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		// first create a combination with only each single SignalGroup
		Set<Entry<ArrayList<SignalGroup>, Integer>> createdCombinations = new LinkedHashSet<>();

		for (int idx = 0; idx < signalGroups.size(); idx++) {
			ArrayList<SignalGroup> newCombination = new ArrayList<>(signalGroups.subList(idx, idx + 1));
			createdCombinations.add(new AbstractMap.SimpleEntry<>(newCombination, idx));
		}
		// now iterate over all known combinations and over all signalGroups and create
		// know combination from known combination and signalGroup, if maxIdx of known
		// combination < signalGroupIdx
		// store them, if they are not illegal

		for (int newSgIdx = 1; newSgIdx < signalGroups.size(); newSgIdx++) {
			Set<Entry<ArrayList<SignalGroup>, Integer>> newCombinations = new LinkedHashSet<>();
			knownCombinationsLoop: for (Entry<ArrayList<SignalGroup>, Integer> knownCombination : createdCombinations) {
				// to prevent duplicated combinations only number-higher signalGroups will be
				// added. E.g., if signalGroup with idx 1 exist, we will find combination with
				// idx 8 (1-8). We don't need to check for combination 8-1, since it's the same.
				if (newSgIdx <= knownCombination.getValue())
					continue;
				// check, if an illegal combination will occur, when another group is added
				SignalGroup newSg = signalGroups.get(newSgIdx);
				for (SignalGroup knownSg : knownCombination.getKey()) {
					// if one combination in this known combination is conflicting, we cannot add a
					// new signalGroup with current Idx so this known combination is skipped
					if (!isConflictFreeCombination(newSg.getId(), knownSg.getId())) {
						continue knownCombinationsLoop;
					}
				}
				// if the known combination is not skipped above we will keep the new
				// combinations for adding them to the created combinations
				ArrayList<SignalGroup> newCombination = new ArrayList<>(knownCombination.getKey());
				newCombination.add(newSg);
				newCombinations.add(new AbstractMap.SimpleEntry<>(newCombination, newSgIdx));
			}
			createdCombinations.addAll(newCombinations);
		}

		ArrayList<ArrayList<SignalGroup>> returnList = new ArrayList<>();
		for (Entry<ArrayList<SignalGroup>, Integer> e : createdCombinations) {
			returnList.add(e.getKey());
		}
		return returnList;
	}

	public ArrayList<SignalPhase> createSignalCombinations() {
		
		ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = createAllValidSignalGroupCombinations();
		ArrayList<SignalPhase> validPhases = new ArrayList<>();

		for(ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			SignalPhase newPhase = new SignalPhase();
			for (SignalGroup sg : sgs) {
				List<Id<Lane>> signalLanes = new LinkedList<>();
				for (Signal s : sg.getSignals().values()) {
					if (s.getLaneIds() != null) {
						signalLanes.addAll(s.getLaneIds());
					}
				}
				newPhase.addGreenSignalGroup(sg);
			}
			validPhases.add(newPhase);
		}
		return validPhases;
	}
	
	public boolean isConflictFreeCombination(Id<SignalGroup> firstSg, Id<SignalGroup> secondSg) {
		for (Direction greenDirection : setOfDirectionsPerGroup.get(firstSg)) {
			for (Direction directionToSwitchGreen : setOfDirectionsPerGroup.get(secondSg)) {
				if (greenDirection.getConflictingDirections().contains(directionToSwitchGreen.getId())
						|| directionToSwitchGreen.getConflictingDirections().contains(greenDirection.getId())) {
					// groups are conflicting
					return false;
				}
			}
		}
		// groups are either non-conflicting, or have different priorities but can show green together (must yield; right of way)
		return true;
	}

}
