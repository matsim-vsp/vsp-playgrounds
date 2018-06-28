package signals.laemmer.model.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.Controler;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.SignalPhase;

public class SignalUtils {
	/**
	 * 
	 * Creates permutations of all list elements with a maximum length of length. Deprecated, use {@link permutate(ArrayList<SignalGroup>, Network, Lanes} instead.
	 * 
	 * @param signalGroups list with signalGroups to permutate 
	 * @param length maximum length of permutations
	 * @return list of lists with all possible permutations, may content duplicates and empty lists
	 * @author pschade
	 *
	 */
	@Deprecated /** deprecated since runtime is unacceptable bad **/
	private static ArrayList<ArrayList<SignalGroup>> permutate(ArrayList<SignalGroup> signalGroups, int length) {
		ArrayList<ArrayList<SignalGroup>> out = new ArrayList<>();

		//Abbruchbedingung, wenn nur noch Länge 1 alle erzeugen und zurückgeben
		if (length == 1) {
			for (SignalGroup e : signalGroups) {
				out.add(new ArrayList<SignalGroup>(Arrays.asList(e)));
			}
			return out;
		}

		//Wenn (Elementanzahl = Länge) eine Kombination erzeugen und hinzufügen

		if (length == signalGroups.size()) {
			ArrayList<SignalGroup> tmp = new ArrayList<>();
			for (SignalGroup e : signalGroups) {
				tmp.add(e);
			}
			out.add(tmp);
		}

		//wenn (Elementanzahl > Länge) allo kombinationen hinzufügen, bei denen jeweils 1 übersprungen wird
		if (length < signalGroups.size()) {	
			for (int elementIdxToSkip = 0; elementIdxToSkip < signalGroups.size(); elementIdxToSkip++) {
				ArrayList<SignalGroup> tmp = new ArrayList<>();
				for (int curElementIdx=0; curElementIdx < signalGroups.size(); curElementIdx++) {
					if (curElementIdx!=elementIdxToSkip)
						tmp.add(signalGroups.get(curElementIdx));
				}
				if (!out.contains(tmp))
					out.add(tmp);
			}
		}

		// nun für oben erzeugte Subsets erneut aufrufen mit (länge-1)
		ArrayList<ArrayList<SignalGroup>> tmpCombinations = new ArrayList<>();
		for (ArrayList<SignalGroup> subList : out) {
			ArrayList<ArrayList<SignalGroup>> newCombinations = permutate(subList, length-1);
			for (ArrayList<SignalGroup> combination : newCombinations) {
				if (!tmpCombinations.contains(combination))
					tmpCombinations.add(combination);
			}
		}
		out.addAll(tmpCombinations);
		return out;
	}

	private static ArrayList<ArrayList<SignalGroup>> createAllValidSignalGroupsCombinations(ArrayList<SignalGroup> signalGroups, Network network, Lanes lanes) {

		//first create a combination with only each single SignalGroup
		Set<Entry<ArrayList<SignalGroup>, Integer>> createdCombinations = new LinkedHashSet<>();

		for (int idx = 0; idx < signalGroups.size(); idx++) {
			ArrayList<SignalGroup> newCombination = new ArrayList<>(signalGroups.subList(idx, idx+1));
			createdCombinations.add(new AbstractMap.SimpleEntry<>(newCombination, idx));
		}
		//now iterate over all known combinations and over all signalGroups and create know combination from known combination and signalGroup, if maxIdx of known combination < signalGroupIdx
		//store them, if they are not illegal


		for (int newSgIdx = 1; newSgIdx < signalGroups.size(); newSgIdx++) {
			Set<Entry<ArrayList<SignalGroup>, Integer>> newCombinations = new LinkedHashSet<>();
			knownCombinationsLoop:
				for (Entry<ArrayList<SignalGroup>, Integer> knownCombination : createdCombinations) {
					//to prevent doubled combinations only number-higher signalGroups will be added. E.g., if signalGroup with idx 1 exist, we will find combination with idx 8 (1-8). We dont need to check for combination 8-1, since its the same.
					if (newSgIdx <= knownCombination.getValue())
						continue;
					//now check, if an illegal combination will occur, if another Sg are added.
					SignalGroup newSg = signalGroups.get(newSgIdx);
					for (SignalGroup knownSg : knownCombination.getKey()) {
						//if one combination in this known combination, we cannot add a new signalGroup with current Idx so this known combination is skipped
						if (!isConflictFreeCombination(newSg, knownSg, network, lanes)) {
							continue knownCombinationsLoop;
						}
					}
					//if the known combination is not skipped above we will keep the new combinations for adding them to the created combinations
					ArrayList<SignalGroup> newCombination = new ArrayList<>(knownCombination.getKey());
					newCombination.add(newSg);
					newCombinations.add(new AbstractMap.SimpleEntry<>(newCombination, newSgIdx));
				}
			createdCombinations.addAll(newCombinations);
		}

		ArrayList<ArrayList<SignalGroup>> returnList = new ArrayList<ArrayList<SignalGroup>>();
		for (Entry<ArrayList<SignalGroup>, Integer> e : createdCombinations) {
			returnList.add(e.getKey());
		}
		return returnList;
	}


	/**
	 * Removes duplicate combinations of signalGroups
	 * this should be moved to permutate(…) (with parameter?)
	 * @param permutationsWithDuplicates
	 * @return
	 * @author pschade
	 */
	private static ArrayList<ArrayList<SignalGroup>> removeDuplicateCombinations(ArrayList<ArrayList<SignalGroup>> permutationsWithDuplicates) {
		ArrayList<ArrayList<SignalGroup>> clearedPermutes = new ArrayList<>();
		for (ArrayList<SignalGroup> permut : permutationsWithDuplicates) {
			if (permut.size()==0)
				continue;
			boolean hasPermutInClearedList = false;
			for (ArrayList<SignalGroup> existPermut : clearedPermutes) {
				if (existPermut.equals(permut)) {
					hasPermutInClearedList = true;
					break;
				}
			}
			if(hasPermutInClearedList)
				continue;
			clearedPermutes.add(permut);
		}
		return clearedPermutes;
	}

	public static ArrayList<SignalPhase> createSignalPhasesFromSignalGroups(SignalSystem system, Network network, Lanes lanes) {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		//ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = removeDuplicates(permutate(signalGroups, signalGroups.size()));
		ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = removeDuplicateCombinations(createAllValidSignalGroupsCombinations(signalGroups, network, lanes));
		ArrayList<SignalPhase> validPhases = new ArrayList<>();

		//System.out.println("size of siggroups before illegal; "+allSignalGroupPerms.size());

		//check for illegal combinations
		ArrayList<ArrayList<SignalGroup>> illegalSignalGroups = new ArrayList<>();
		for (ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			//check every signalGroup in this combination for illegal flows
			outerSgLoop:
				for (SignalGroup outerSg : sgs) {
					for (SignalGroup innerSg : sgs) {
						if(!isConflictFreeCombination(outerSg, innerSg, network, lanes)) {
							illegalSignalGroups.add(sgs);
							break outerSgLoop;
						}
					}
				}
		}
		//System.out.println("removing "+illegalSignalGroups.size()+" illegal ones");
		allSignalGroupPerms.removeAll(illegalSignalGroups);

		//		System.out.println("size after removing illegals: "+allSignalGroupPerms.size());
		//		System.out.print("Phases: ");
		for(ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			SignalPhase newPhase = new SignalPhase();
			for (SignalGroup sg : sgs) {
				//				System.out.print(sg.getId()+", ");
				List<Id<Lane>> signalLanes = new LinkedList<>();
				for (Signal s : sg.getSignals().values()) {
					if (s.getLaneIds() != null) {
						signalLanes.addAll(s.getLaneIds());
					}
				}
				newPhase.addGreenSignalGroup(sg);
			}
			//			System.out.print("\n");
			validPhases.add(newPhase);
		}
		return validPhases;
	}

	public static boolean isConflictFreeCombination(SignalGroup firstSg, SignalGroup secondSg, Network network, Lanes lanes) {
		// further tests can skipped after at least one illegal combination where found
		for (Signal outerSignal : firstSg.getSignals().values()) {
			Conflicts outerSignalLinkConflicts = (Conflicts) network.getLinks().get(outerSignal.getLinkId()).getAttributes().getAttribute("conflicts");
			for (Signal innerSignal : secondSg.getSignals().values()) {
				Conflicts innerSignalLinkConflicts = (Conflicts) network.getLinks().get(innerSignal.getLinkId()).getAttributes().getAttribute("conflicts");
				// skip if inner and outer signal are the same signal
				if (outerSignal.equals(innerSignal))
					continue;
				// check conflicts on link-level
				if ((outerSignalLinkConflicts != null && outerSignalLinkConflicts.hasConflict(innerSignal.getLinkId()))
						|| (innerSignalLinkConflicts != null && innerSignalLinkConflicts.hasConflict(outerSignal.getLinkId()))) {
					return false;
				}
				// check conflicts on lane-level of outerSignal has lanes
				if (outerSignal.getLaneIds() != null && !outerSignal.getLaneIds().isEmpty()) {
					for (Id<Lane> outerSignalLane : outerSignal.getLaneIds()) {
						Conflicts outerSignalLaneConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(outerSignal.getLinkId()).getLanes().get(outerSignalLane)
								.getAttributes().getAttribute("conflicts");
						// check conflicts between outerLane-innerLink
						if ((outerSignalLaneConflicts != null && outerSignalLaneConflicts.hasConflict(innerSignal.getLinkId()))
								|| (innerSignalLinkConflicts != null && innerSignalLinkConflicts.hasConflict(outerSignal.getLinkId(), outerSignalLane))) {
							return false;
						}
						// if also innerSignal has lanes, check conflicts on lane-level against
						// inner/outer signal
						if (innerSignal.getLaneIds() != null && !innerSignal.getLaneIds().isEmpty()) {
							for (Id<Lane> innerSignalLane : innerSignal.getLaneIds()) {
								Conflicts innerSignalLaneConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(innerSignal.getLinkId()).getLanes().get(innerSignalLane)
										.getAttributes().getAttribute("conflicts");
								if ((innerSignalLaneConflicts != null && (innerSignalLaneConflicts.hasConflict(outerSignal.getLinkId())
										|| innerSignalLaneConflicts.hasConflict(outerSignal.getLinkId(), outerSignalLane)))
										|| (outerSignalLinkConflicts != null && outerSignalLinkConflicts.hasConflict(innerSignal.getLinkId(), innerSignalLane))
										|| (outerSignalLaneConflicts != null && outerSignalLaneConflicts.hasConflict(innerSignal.getLinkId(), innerSignalLane))) {
									return false;
								}
							}
						}
					}
					// if outersignal has no lanes but inner signal has, its needed to check if
					// there are conflicts from inner signal lanes to outer singal link
				} else if (innerSignal.getLaneIds() != null && !innerSignal.getLaneIds().isEmpty()) {
					for (Id<Lane> innerSignalLane : innerSignal.getLaneIds()) {
						Conflicts innerSignalLaneConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(innerSignal.getLinkId()).getLanes().get(innerSignalLane)
								.getAttributes().getAttribute("conflicts");
						if ((innerSignalLaneConflicts != null && (innerSignalLaneConflicts.hasConflict(outerSignal.getLinkId())))
								|| (outerSignalLinkConflicts != null && outerSignalLinkConflicts.hasConflict(innerSignal.getLinkId(), innerSignalLane))) {
							return false;
						}
					}

				}
			}
		}
		return true;
	}

	public static ArrayList<SignalPhase> removeSubPhases(ArrayList<SignalPhase> signalPhases) {
		ArrayList<SignalPhase> phasesToRemove = new ArrayList<>();
		for (SignalPhase signalPhase : signalPhases) {
			for (SignalPhase otherSignalPhase : signalPhases) {
				if (!signalPhase.equals(otherSignalPhase) && signalPhase.getGreenSignalGroups().containsAll(otherSignalPhase.getGreenSignalGroups())){
					phasesToRemove.add(otherSignalPhase);
				}
			}
		}
		signalPhases.removeAll(phasesToRemove);
		return signalPhases;
	}

	public static void writeSignaltoLanesAndLinksCombinationsToTempFile(Scenario scenario, Controler controler) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("SignalSystemId; SignalGroupId; SignalId; LinkId; LinkCapacity; LinkLaneCnt; LaneId; LaneCapacity; LaneCnt; toLinks; toLanes\n");
		for (SignalSystemData signalSystemsData : ((SignalsData)controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData().getSignalSystemData().values()) {
			for (SignalGroupData sg : ((SignalsData)controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalGroupsData().getSignalGroupDataBySystemId(signalSystemsData.getId()).values()) {
				for (Id<Signal> signalId : sg.getSignalIds()) {
					SignalData signal = ((SignalsData)controler.getScenario().getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData().getSignalSystemData().get(signalSystemsData.getId()).getSignalData().get(signalId);
					org.matsim.api.core.v01.network.Link link = scenario.getNetwork().getLinks().get(signal.getLinkId());
					if (signal.getLaneIds() != null) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							String toLinks = new String();
							String toLanes = new String();
							Lane lane = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId);
							if (lane.getToLinkIds() != null) {
								for (Id<org.matsim.api.core.v01.network.Link> toLinkId : lane.getToLinkIds())
									toLinks = toLinks.concat(", "+toLinkId.toString());
							}
							if (lane.getToLaneIds() != null) {
								for (Id<Lane> toLaneId : lane.getToLaneIds()) 
									toLanes = toLanes.concat(", "+toLaneId.toString());
							}
							builder.append(signalSystemsData.getId()+"; "+sg.getId()+"; "+ signal.getId() + "; " + link.getId() + "; " + link.getCapacity() + "; " + link.getNumberOfLanes() + "; " + lane.getId() + "; " + lane.getCapacityVehiclesPerHour() + "; "+ lane.getNumberOfRepresentedLanes()+"; "+toLinks+"; "+toLanes+"\n");
						}
					}
					else {
						builder.append(signalSystemsData.getId()+"; "+sg.getId()+"; "+ signal.getId() + "; " + link.getId() + "; " + link.getCapacity() + "; " + link.getNumberOfLanes() + "; " + "null" + "; " + "null" + "; "+ "null" +"; "+ "null" +"; "+ "null" +"\n");
					}
				}
			}
		}
		File tmp = File.createTempFile("signaledNodesdata", ".csv");
		FileWriter writer = new FileWriter(tmp);
		writer.append(builder.toString());
		writer.close();
	}

	/**
	 * Estimates the number of minimal needed phases in a signalSystem. Will not return accurate results if:
	 *  - if two links, but not opposite links, have seperate signaled left-tunring-lanes
	 *  - probably on crossings with more than for links
	 *  - and probably other non-common crossing layouts.
	 * @param signalSystem
	 * @param network
	 * @param lanes
	 * @return minimal number of phases.
	 */
	public static int estimateNumberOfPhases(SignalSystem signalSystem, Network network, Lanes lanes) {
		int numOfPhases = 2; //at least each signalized crossing will have two phases
		TreeMap<Id<Link>, List<Id<Link>>> linksToConflictingLinks = new TreeMap<>(); //map with entry for each link and list with all links, for whom any kind of conflict (linkwise or lanewise) exist
		for (Signal signal : signalSystem.getSignals().values()) {
			List<Id<Link>> thisLinksConflicts;
			if (!linksToConflictingLinks.containsKey(signal.getLinkId())) {
				thisLinksConflicts = new LinkedList<>();
				linksToConflictingLinks.put(signal.getLinkId(), thisLinksConflicts);
			} else {
				thisLinksConflicts = linksToConflictingLinks.get(signal.getLinkId());
			}
			//add all link-level conflicts
			Conflicts linksConflicts = (Conflicts) network.getLinks().get(signal.getLinkId()).getAttributes().getAttribute("conflicts");
			if (linksConflicts != null) {
				for (Id<Link> conflictingLinkId : linksConflicts.getConflicts().keySet()) {
					if(!thisLinksConflicts.contains(conflictingLinkId)) {
						thisLinksConflicts.add(conflictingLinkId);
					}
				}
			}
			if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
				//add all lane-level conflicts
				for (Id<Lane> laneId : signal.getLaneIds()) {
					Conflicts lanesConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId).getAttributes()
							.getAttribute("conflicts");
					if (lanesConflicts != null) {
						for (Id<Link> conflictingLinkId : lanesConflicts.getConflicts().keySet()) {
							if (!thisLinksConflicts.contains(conflictingLinkId)) {
								thisLinksConflicts.add(conflictingLinkId);
							}
						}
					}
				} 
			}
		}
		//for each two links, for whom more than two conflicts exist, we assume a second phase.
		int additionalPhases = 0;
		for (List<Id<Link>> conflictsList : linksToConflictingLinks.values()) {
			if (conflictsList.size() > 2) {
				additionalPhases++;
			}
		}
		numOfPhases += (Math.ceil(additionalPhases/2.0));
		return numOfPhases;
	}
}
