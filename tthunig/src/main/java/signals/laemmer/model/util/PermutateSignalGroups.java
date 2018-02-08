package signals.laemmer.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.SignalPhase;

public class PermutateSignalGroups {
	/**
	 * 
	 * Creates permutations of all list elements with a maximum length of length 
	 * 
	 * @param signalGroups list with signalGroups to permutate 
	 * @param length maximum length of permutations
	 * @return list of lists with all possible permutations, may content duplicates and empty lists
 	 * @author pschade
	 *
	 */
	private static ArrayList<ArrayList<SignalGroup>> permutate(ArrayList<SignalGroup> signalGroups, int length) {
		ArrayList<ArrayList<SignalGroup>> out = new ArrayList<>();
		
		//Abbruchbedingung, wenn nur noch Länge 1 alle erzeugen und zurückgeben
		if (length == 1) {
			for (SignalGroup e : signalGroups) {
				out.add(new ArrayList<SignalGroup>(Arrays.asList(e)));
			}
			return out;
		}
		
		//Wenn (Elementanzahl = Länge) ein Set erzeugen und hinzufügen

		if (length == signalGroups.size()) {
			ArrayList<SignalGroup> tmp = new ArrayList<>();
			for (SignalGroup e : signalGroups) {
				tmp.add(e);
			}
			out.add(tmp);
		}
		
		//wenn (Elementanzahl > Länge) 
		if (length < signalGroups.size()) {	
			for (int elementIdxToSkip = 0; elementIdxToSkip < signalGroups.size(); elementIdxToSkip++) {
				ArrayList<SignalGroup> tmp = new ArrayList<>();
				for (int curElementIdx=0; curElementIdx < signalGroups.size(); curElementIdx++) {
					if (curElementIdx!=elementIdxToSkip)
						tmp.add(signalGroups.get(curElementIdx));
				}
				out.add(tmp);
			}
		}

		// nun für oben erzeugte Subsets erneut aufrufen mit (länge-1)
		ArrayList<ArrayList<SignalGroup>> tmp = new ArrayList<>();
		for (ArrayList<SignalGroup> subList : out) {
			tmp.addAll(permutate(subList, length-1));
		}
		out.addAll(tmp);
		return out;
	}
	
	/**
	 * Removes duplicate combinations of signalGroups
	 * this should be moved to permutate(…) (with parameter?)
	 * @param permutationsWithDuplicates
	 * @return
	 * @author pschade
	 */
	private static ArrayList<ArrayList<SignalGroup>> removeDuplicates(ArrayList<ArrayList<SignalGroup>> permutationsWithDuplicates) {
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
	
    public static ArrayList<SignalPhase> createPhasesFromSignalGroups(SignalSystem system, Network network, Lanes lanes) {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = removeDuplicates(permutate(signalGroups, signalGroups.size()));
		ArrayList<SignalPhase> validPhases = new ArrayList<>();

		System.out.println("size of siggroups before illegal; "+allSignalGroupPerms.size());
		
		//check for illegal combinations
		ArrayList<ArrayList<SignalGroup>> illegalSignalGroups = new ArrayList<>();
		for (ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			//check every signalGroup in this combination for illegal flows
			for (SignalGroup outerSg : sgs) {
				//further tests can skipped after at least one illegal combination where found
				boolean isIllegal = false;
				for (Signal outerSignal : outerSg.getSignals().values()) {
					Conflicts outerSignalLinkConflicts = (Conflicts) network.getLinks().get(outerSignal.getLinkId()).getAttributes().getAttribute("conflicts"); 
					for (SignalGroup innerSg : sgs) {
						for (Signal innerSignal : innerSg.getSignals().values()) {
							Conflicts innerSignalLinkConflicts = (Conflicts) network.getLinks().get(innerSignal.getLinkId()).getAttributes().getAttribute("conflicts");
							//skip if inner and outer signal are the same signal
							if (outerSignal.equals(innerSignal))
								continue;
							if ((outerSignalLinkConflicts != null && outerSignalLinkConflicts.hasConflict(innerSignal.getLinkId()))
									|| (innerSignalLinkConflicts != null && innerSignalLinkConflicts.hasConflict(outerSignal.getLinkId()))) {
								isIllegal = true;
								break;
							}
							for (Id<Lane> outerSignalLane : outerSignal.getLaneIds()) {
								Conflicts outerSignalLaneConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(outerSignal.getLinkId()).getLanes().get(outerSignalLane).getAttributes().getAttribute("conflicts");
								if ((outerSignalLaneConflicts != null && outerSignalLaneConflicts.hasConflict(innerSignal.getLinkId()))
										|| (innerSignalLinkConflicts != null &&innerSignalLinkConflicts.hasConflict(outerSignal.getLinkId(), outerSignalLane))) {
									isIllegal = true;
									break;
								}
								for(Id<Lane> innerSignalLane : innerSignal.getLaneIds()) {
									Conflicts innerSignalLaneConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(innerSignal.getLinkId()).getLanes().get(innerSignalLane).getAttributes().getAttribute("conflicts");
									if ((innerSignalLaneConflicts != null && (innerSignalLaneConflicts.hasConflict(outerSignal.getLinkId())
											|| innerSignalLaneConflicts.hasConflict(outerSignal.getLinkId(), outerSignalLane)))
											|| (outerSignalLinkConflicts != null && outerSignalLinkConflicts.hasConflict(innerSignal.getLinkId(), innerSignalLane))
											|| (outerSignalLaneConflicts != null && outerSignalLaneConflicts.hasConflict(innerSignal.getLinkId(), innerSignalLane))) {
										isIllegal = true;
										break;
									}
								}
								if (isIllegal)
									break;
							}
							if (isIllegal)
								break;
						}
						if(isIllegal)
							break;
					}
					if(isIllegal)
						break;
				}
				if(isIllegal) {
					illegalSignalGroups.add(sgs);
					isIllegal = true;
					break;
				}
			}
		}
		System.out.println("removing "+illegalSignalGroups.size()+" illegal ones");
		allSignalGroupPerms.removeAll(illegalSignalGroups);
		
		System.out.println("size after removing illegals: "+allSignalGroupPerms.size());
		
		for(ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			SignalPhase newPhase = new SignalPhase();
			for (SignalGroup sg : sgs) {
				List<Id<Lane>> signalLanes = new LinkedList<>();
				for (Signal s : sg.getSignals().values()) {
					signalLanes.addAll(s.getLaneIds());
				}
				newPhase.addGreenSignalGroupsAndLanes(sg.getId(), signalLanes);
			}
			validPhases.add(newPhase);
		}
		return validPhases;
		}
	
    public static ArrayList<SignalPhase> createPhasesFromSignalGroups(SignalSystem system, Map<Id<Lane>, Lane> lanemap) {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = removeDuplicates(permutate(signalGroups, signalGroups.size()));
		ArrayList<SignalPhase> validPhases = new ArrayList<>();
		//check for illegal combinations
		ArrayList<ArrayList<SignalGroup>> illegalGroups = new ArrayList<>();
		for (ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			ArrayList<Id<Lane>> lanesOfAllSg = new ArrayList<>();
			//collect all lanes in this permutation
			for (SignalGroup sg : sgs) {
				for (Signal signal : sg.getSignals().values()) {
					lanesOfAllSg.addAll(signal.getLaneIds());
				}
			}
//			I think the following loop isn't needed since sg is not used and we only have to check if there are tow conflicting lanes in a permutation, pschade Dec 17
//			for (SignalGroup sg : sgs) {
				for (Id<Lane> l : lanesOfAllSg) {
					boolean hasIllegal = false;
					for (Id<Lane> conflictingLane : ( (ArrayList<Id<Lane>>) (lanemap.get(l).getAttributes().getAttribute("conflictingLanes")) ) ) {
						if (lanesOfAllSg.contains(conflictingLane)) {
							illegalGroups.add(sgs);
							hasIllegal = true;
							break;
						}
					}
					//if one lane already conflicts with another we can skip further checks and mark this group as illegal, pschade Dec 17
					if (hasIllegal)
						break;
				}
//			}
		}
		allSignalGroupPerms.removeAll(illegalGroups);
		
		for(ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			SignalPhase newPhase = new SignalPhase();
			for (SignalGroup sg : sgs) {
				List<Id<Lane>> lanes = new LinkedList<>();
				for (Signal s : sg.getSignals().values()) {
					lanes.addAll(s.getLaneIds());
				}
				newPhase.addGreenSignalGroupsAndLanes(sg.getId(), lanes);
			}
			validPhases.add(newPhase);
		}
		return validPhases;
	}

	public static ArrayList<SignalPhase> removeSubPhases(ArrayList<SignalPhase> signalPhases) {
		ArrayList<SignalPhase> phasesToRemove = new ArrayList<>();
		for (SignalPhase signalPhase : signalPhases) {
			for (SignalPhase otherSignalPhase : signalPhases) {
				if (!signalPhase.equals(otherSignalPhase) && signalPhase.getGreenLanes().containsAll(otherSignalPhase.getGreenLanes())){
					phasesToRemove.add(otherSignalPhase);
				}
			}
		}
		signalPhases.removeAll(phasesToRemove);
		return signalPhases;
	}
}
