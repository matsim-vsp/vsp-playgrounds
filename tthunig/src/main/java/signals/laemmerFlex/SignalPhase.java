package signals.laemmerFlex;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class SignalPhase {
	private List<SignalGroup> signalGroups = new LinkedList<>();
//	private Map<Id<SignalGroup>, List<Id<Lane>>> greenSignalsToLanes = new LinkedHashMap<>();
	private Map<Id<Link>, List<Id<Lane>>> lanesToLinks = new LinkedHashMap<>();
//	private SortedSet<Id<Lane>> lanes = new TreeSet<Id<Lane>>();
	private Id<SignalPhase> id;
		
	public SignalPhase() {
		this.id = Id.create("empty phase", SignalPhase.class);
	}
	
	public SignalPhase(LinkedList<SignalGroup> signalGroups) {
		for (SignalGroup signalGroup : signalGroups) {
			this.addGreenSignalGroup(signalGroup);
		}
		this.id = createIdFromCurrentSignalGroups();
	}
	
	private Id<SignalPhase> createIdFromCurrentSignalGroups() {
		StringBuilder idStringBuilder = new StringBuilder();
		for (SignalGroup group : this.signalGroups)
			idStringBuilder.append(group.getId()+"-");
		idStringBuilder.deleteCharAt(idStringBuilder.length()-1);
		return Id.create(idStringBuilder.toString(), SignalPhase.class);
	}
	
	public void addGreenSignalGroup(SignalGroup signalGroup) {
		if (!this.getGreenSignalGroups().contains(signalGroup.getId())) {
				signalGroups.add(signalGroup);
			for (Signal signal : signalGroup.getSignals().values()) {
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
					lanesToLinks.put(signal.getLinkId(), null);				
				} else {
					LinkedList<Id<Lane>> laneIds = new LinkedList<>(); 
					for (Id<Lane> laneId : signal.getLaneIds())
						laneIds.add(laneId);
					if (lanesToLinks.containsKey(signal.getLinkId())) {
						lanesToLinks.get(signal.getLinkId()).addAll(laneIds);
					} else {
						lanesToLinks.put(signal.getLinkId(), laneIds);
					}
				}
			}
			this.id = createIdFromCurrentSignalGroups();
		}
	}
	
	public Set<Id<SignalGroup>> getGreenSignalGroups(){
		LinkedHashSet<Id<SignalGroup>> sgs = new LinkedHashSet<>();
		for (SignalGroup sg : signalGroups) {
			sgs.add(sg.getId());
		}
		return sgs;
	}
	
	public Map<Id<Link>, List<Id<Lane>>> getGreenLanesToLinks(){
		return lanesToLinks;
	}
	
	public boolean equals(SignalPhase other) {
		if(this.getGreenSignalGroups().equals(other.getGreenSignalGroups()))
			return true;
		else
			return false;
	}

	public SignalGroupState getState() {
		boolean allGreen = true;
		for (SignalGroup sg : signalGroups)
			allGreen &= sg.getState().equals(SignalGroupState.GREEN);
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

	public boolean containsGreenLane(Id<Link> linkId, Id<Lane> laneId) {
		//TODO probably here is a check to null in the lanestolink map for the values needed
		if (lanesToLinks.containsKey(linkId) && (laneId == null || lanesToLinks.get(linkId).contains(laneId)))
			return true;
		else
			return false;
	}

	public int getNumberOfGreenLanes() {
		//TODO could also be a field which is updated while adding new signalGroups
		int numOfGreenLanes = 0;
		for (Entry<Id<Link>, List<Id<Lane>>> e : lanesToLinks.entrySet()) {
			if (e.getValue() != null) {
				numOfGreenLanes += e.getValue().size();
			}
		}
		return numOfGreenLanes;
		//TODO check if null values can be filtered.
		//return lanesToLinks.entrySet().parallelStream().mapToInt(e -> e.getValue().size()).sum();
	}
}
