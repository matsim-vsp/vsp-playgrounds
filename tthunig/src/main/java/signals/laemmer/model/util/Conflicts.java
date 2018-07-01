package signals.laemmer.model.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class Conflicts {
	static org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(Conflicts.class);
	private Id<Link> linkId;
	private Id<Lane> laneId = null;
	private TreeMap<Id<Link>, List<Id<Lane>>> conflicts = new TreeMap<>();
	private TreeMap<Id<Link>, List<Id<Lane>>> allowedConflictsNonPriority = new TreeMap<>();
	private TreeMap<Id<Link>, List<Id<Lane>>> allowedConflictsPriority = new TreeMap<>();
	
	public Conflicts(Id<Link> link) {
		this.linkId = link;
	}
	
	public Conflicts(Id<Link> link, Id<Lane> lane) {
		this(link);
		this.laneId = lane;
	}
	
	private void add(Id<Link> conflictingLink, TreeMap<Id<Link>, List<Id<Lane>>> map){
		if (map.containsKey(conflictingLink))
			log.warn("Replacing prior created, possibly lane-wise, conflicts from "+this.linkId+(this.laneId == null? "" : ", "+ this.laneId)+" to "+conflictingLink+". Now all lanes of "+conflictingLink+" conflicts to "+this.linkId+".");
		map.put(conflictingLink, null);
	}
	
	private void add(Id<Link> conflictingLink, Id<Lane> conflictingLane, TreeMap<Id<Link>, List<Id<Lane>>> map){
		if (conflictingLane == null) {
			log.warn("Explicit adding of null-object for lane is not recommended");
			add(conflictingLink, map);
		}
		else {
			if (!map.containsKey(conflictingLink) || conflicts.get(conflictingLink) == null)
				map.put(conflictingLink, new LinkedList<Id<Lane>>(Arrays.asList(conflictingLane)));
			else
				if (!map.get(conflictingLink).contains(conflictingLane))
					map.get(conflictingLink).add(conflictingLane);
		}
	}
	
	public void addConflict(Id<Link> conflictingLink) {
		this.add(conflictingLink, this.conflicts);
	}
	
	public void addConflict(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		this.add(conflictingLink, conflictingLane, this.conflicts);
	}
	
	public void addAllowedConflictNonPriority(Id<Link> conflictingLink) {
		this.add(conflictingLink, this.allowedConflictsNonPriority);
	}
	
	public void addAllowedConflictNonPriority(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		this.add(conflictingLink, conflictingLane, this.allowedConflictsNonPriority);
	}
	public void addAllowedConflictPriority(Id<Link> conflictingLink) {
		this.add(conflictingLink, this.allowedConflictsPriority);
	}
	
	public void addAllowedConflictPriority(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		this.add(conflictingLink, conflictingLane, this.allowedConflictsPriority);
	}
	
	private boolean genericCheckConflict(Id<Link> conflictingLink, TreeMap<Id<Link>, List<Id<Lane>>> map) {
		if(map.containsKey(conflictingLink) && map.get(conflictingLink) == null)
			return true;
		else
			return false;
	}
	private boolean genericCheckConflict(Id<Link> conflictingLink, Id<Lane> conflictingLane, TreeMap<Id<Link>, List<Id<Lane>>> map) {
		if(genericCheckConflict(conflictingLink, map))
			return true;
		else
			if(map.containsKey(conflictingLink) && map.get(conflictingLink).contains(conflictingLane))
				return true;
			else 
				return false;
	}

	public boolean hasConflict(Id<Link> conflictingLink) {
		return genericCheckConflict(conflictingLink, conflicts);
	}
	
	public boolean hasConflict(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		return genericCheckConflict(conflictingLink, conflictingLane, conflicts);
	}
	
	public boolean hasAllowedConflictWithNonPriorityAgainst(Id<Link> conflictingLink) {
		return genericCheckConflict(conflictingLink, allowedConflictsNonPriority);
	}
	
	public boolean hasAllowedConflictWithNonPriorityAgainst(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		return genericCheckConflict(conflictingLink, conflictingLane, allowedConflictsNonPriority);
	}
	
	public boolean hasAllowedConflictWithPriorityAgainst(Id<Link> conflictingLink) {
		return genericCheckConflict(conflictingLink, allowedConflictsPriority);
	}
	
	public boolean hasAllowedConflictWithPriorityAgainst(Id<Link> conflictingLink, Id<Lane> conflictingLane) {
		return genericCheckConflict(conflictingLink, conflictingLane, allowedConflictsPriority);
	}

	/**
	 * @return the conflicts, used for serialisation
	 */
	TreeMap<Id<Link>, List<Id<Lane>>> getConflicts() {
		return conflicts;
	}

	/**
	 * @return the allowedConflictsNonPriority, used for serialisation
	 */
	TreeMap<Id<Link>, List<Id<Lane>>> getAllowedConflictsNonPriority() {
		return allowedConflictsNonPriority;
	}

	/**
	 * @return the allowedConflictsPriority, used for serialisation
	 */
	TreeMap<Id<Link>, List<Id<Lane>>> getAllowedConflictsPriority() {
		return allowedConflictsPriority;
	}

	/**
	 * Method for serialization. For easier handling the data type is same as for the conflicts.
	 * @return TreeMap with ids for which this conflicts are defined. linkId as key and, if present, a LinkedList as with only the laneId as value. 
	 */
	public TreeMap<Id<Link>, List<Id<Lane>>> getIdsForSerialisation() {
		TreeMap<Id<Link>, List<Id<Lane>>> ids = new TreeMap<>();
		if(this.laneId == null) {
			ids.put(this.linkId, null);
		} else {
			ids.put(this.linkId, new LinkedList<Id<Lane>>(Arrays.asList(this.laneId)));
		}
		return ids;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Id<Lane> getLaneId() {
		return laneId;
	}
	
	
}
