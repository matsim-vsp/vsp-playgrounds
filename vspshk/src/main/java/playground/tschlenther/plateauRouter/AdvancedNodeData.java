package playground.tschlenther.plateauRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class AdvancedNodeData {

	private Id<Node> prevId = null;
	private Id<Node> nextId = null;
	private double cost = Double.MAX_VALUE;
	private double time = 0;
	
	public AdvancedNodeData(Id<Node> prevNodeId, Id<Node> nextNodeId, double cost, double time) {
		this.prevId = prevNodeId;
		this.nextId = nextNodeId;
		this.cost = cost;
		this.time = time;
	}
	
	public double getCost() {
		return this.cost;
	}

	public double getTime() {
		return this.time;
	}

	public Id<Node> getPrevNodeId() {
		return this.prevId;
	}
	
	public Id<Node> getNextNodeId() {
		return this.nextId;
	}
	
	public void setPrevNodeId(Id<Node> prevNodeId) {
		this.prevId = prevNodeId;
	}
	
	public void setNextNodeId(Id<Node> nextNodeId) {
		this.nextId = nextNodeId;
	}
}
