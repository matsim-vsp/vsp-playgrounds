package signals.laemmer.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lane;

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
	
	@Deprecated //use createPhasesFromSignalGroups instead
    private static ArrayList<ArrayList<SignalGroup>> createAllSignalPermutations(SignalSystem system, HashMap<Id<Lane>, Lane> lanemap) {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		ArrayList<ArrayList<SignalGroup>> allSignalGroupPerms = removeDuplicates(permutate(signalGroups, signalGroups.size()));
		
		//check for illegal combinations
		ArrayList<SignalGroup> illegalGroups = new ArrayList<>();
		for (ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
			//check every signalGroup in this combination for illegal flows
			for (SignalGroup sg : sgs) {
				//collect all lanes from this signalgroup's signals
				ArrayList<Id<Lane>> lanesOfCurrSg = new ArrayList<>();
				for (Signal signal : sg.getSignals().values()) {
					lanesOfCurrSg.addAll(signal.getLaneIds());
				}
				//iterate over all lanes an their conflictingLanes and check, if one of these illegal lanes are also in the collected landes of this phase
				for (Id<Lane> l : lanesOfCurrSg) {
					for (Id<Lane> conflictingLane : ( (ArrayList<Id<Lane>>) (lanemap.get(l).getAttributes().getAttribute("conflictingLanes")) ) ) {
						if (lanesOfCurrSg.contains(conflictingLane)) {
							illegalGroups.add(sg);
							break;
						}
					}
				}
			}
		}
		allSignalGroupPerms.removeAll(illegalGroups);
		return allSignalGroupPerms;
		
//		System.out.println("SIZE OF SIGNALGROUPPERMS: "+allSignalGroupPerms.size());
//		for (ArrayList<SignalGroup> sgs : allSignalGroupPerms) {
//			System.out.println();;
//		}
	}
	
    public static ArrayList<SignalPhase> createPhasesFromSignalGroups(SignalSystem system, Map<Id<Lane>, Lane> lanemap) {
		ArrayList<SignalGroup> signalGroups = new ArrayList<>(system.getSignalGroups().values());
		//FIXME remove debug output
		System.err.println("All permuations size w/o duplicates: "+removeDuplicates(permutate(signalGroups, signalGroups.size())).size());
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
		//FIXME remove debug output
		System.err.println("After remove illegal ones: "+allSignalGroupPerms.size());
		
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
			//FIXME remove debug output
			System.out.println(newPhase.toString());
		}
		return validPhases;
	}
}
