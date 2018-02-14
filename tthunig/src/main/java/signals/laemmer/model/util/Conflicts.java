package signals.laemmer.model.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;

public class Conflicts {
	static org.apache.log4j.Logger log = org.apache.log4j.LogManager.getLogger(Conflicts.class);
	private Id<Link> link;
	private Id<Lane> lane = null;
	private TreeMap<Id<Link>, List<Id<Lane>>> conflicts = new TreeMap<>();
	private TreeMap<Id<Link>, List<Id<Lane>>> allowedConflictsNonPriority = new TreeMap<>();
	private TreeMap<Id<Link>, List<Id<Lane>>> allowedConflictsPriority = new TreeMap<>();
	
	public Conflicts(Id<Link> link) {
		this.link = link;
	}
	
	public Conflicts(Id<Link> link, Id<Lane> lane) {
		this(link);
		this.lane = lane;
	}
	
	private void add(Id<Link> conflictingLink, TreeMap<Id<Link>, List<Id<Lane>>> map){
		if (map.containsKey(conflictingLink))
			log.warn("Replacing prior created, possibly lane-wise, conflicts from "+this.link+(this.lane == null? "" : ", "+ this.lane)+" to "+conflictingLink+". Now all lanes of "+conflictingLink+" conflicts to "+this.link+".");
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
	
}
