package signals.laemmer.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;

public class SignalPhase {
	private Map<Id<SignalGroup>, List<Id<Lane>>> greenSignalsToLanes = new HashMap<>();
	private Set<Id<Lane>> lanes = new HashSet<>();
	private Id<SignalPhase> id;
	
	public SignalPhase() {
		
	}
	
	public SignalPhase(Map<Id<SignalGroup>, LinkedList<Id<Lane>>> greenSignalesToLanes) {
		this.greenSignalsToLanes.putAll(greenSignalesToLanes);
		StringBuilder idStringBuilder = new StringBuilder();
		for (Id<SignalGroup> group : greenSignalesToLanes.keySet())
			idStringBuilder.append(group+"-");
		idStringBuilder.deleteCharAt(idStringBuilder.length()-1);
		this.id = Id.create(idStringBuilder.toString(), SignalPhase.class);
		//TODO prüfen ob das funktioniert:
		greenSignalesToLanes.values().forEach(lanes::addAll);
	}
	
	public void addGreenSignalGroupsAndLanes(Id<SignalGroup> signal, List<Id<Lane>> lanes) {
		greenSignalsToLanes.put(signal, lanes);
		lanes.addAll(lanes);
	}
	
	public Set<Id<SignalGroup>> getGreenSignalGroups(){
		return greenSignalsToLanes.keySet();
	}
	
	public Set<Id<Lane>> getGreenLanes(){
		return this.lanes;
	}
	
	public boolean equals(SignalPhase other) {
		if(this.getGreenSignalGroups().equals(other.getGreenSignalGroups())
				&& this.getGreenLanes().equals(other.getGreenLanes()))
			return true;
		else
			return false;
	}

	//TODO Perhaps anybody has a better idea how to define the state better w/o changing the whole matsim-signal-logic
	public SignalGroupState getState(SignalSystem system) {
		boolean allGreen = true;
		for (Id<SignalGroup> sg : getGreenSignalGroups())
			allGreen &= (system.getSignalGroups().get(sg).getState().equals(SignalGroupState.GREEN));
		return (allGreen ? SignalGroupState.GREEN : SignalGroupState.RED);
	}
	
	public Id<SignalPhase> getId() {
		return this.id;
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		for (Id<SignalGroup> sg : getGreenSignalGroups()) {
			string.append(sg.toString()+"; ");
		}
		return string.toString();
	}
}
