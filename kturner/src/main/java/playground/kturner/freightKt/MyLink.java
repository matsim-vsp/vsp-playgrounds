package playground.kturner.freightKt;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Set;

class MyLink implements Link {
	private final TravelTime travelTime;
	Link linkDelegate ;
	
	MyLink( Link linkDelegate, TravelTime travelTime ) {
		this.linkDelegate = linkDelegate ;
		this.travelTime = travelTime ;
	}

	public boolean setFromNode(Node node) {
		return linkDelegate.setFromNode(node);
	}

	public boolean setToNode(Node node) {
		return linkDelegate.setToNode(node);
	}

	public Node getToNode() {
		return linkDelegate.getToNode();
	}

	public Node getFromNode() {
		return linkDelegate.getFromNode();
	}

	public double getLength() {
		return linkDelegate.getLength();
	}

	public double getNumberOfLanes() {
		return linkDelegate.getNumberOfLanes();
	}

	public double getNumberOfLanes(double time) {
		return linkDelegate.getNumberOfLanes(time);
	}

	public double getFreespeed() {
		return linkDelegate.getFreespeed();
	}

	public double getFreespeed(double time) {
		// TODO really return speed and not travel time!
			return travelTime.getLinkTravelTime( linkDelegate, time, null, null) ;
	}

	public double getCapacity() {
		return linkDelegate.getCapacity();
	}

	public double getCapacity(double time) {
		return linkDelegate.getCapacity(time);
	}

	public void setFreespeed(double freespeed) {
		linkDelegate.setFreespeed(freespeed);
	}
	// TODO: I think that the setters can throw an exception.

	public void setLength(double length) {
		linkDelegate.setLength(length);
	}

	public void setNumberOfLanes(double lanes) {
		linkDelegate.setNumberOfLanes(lanes);
	}

	public void setCapacity(double capacity) {
		linkDelegate.setCapacity(capacity);
	}

	public void setAllowedModes(Set<String> modes) {
		linkDelegate.setAllowedModes(modes);
	}

	public Set<String> getAllowedModes() {
		return linkDelegate.getAllowedModes();
	}

	public double getFlowCapacityPerSec() {
		return linkDelegate.getFlowCapacityPerSec();
	}

	public double getFlowCapacityPerSec(double time) {
		return linkDelegate.getFlowCapacityPerSec(time);
	}

	public Coord getCoord() {
		return linkDelegate.getCoord();
	}

	public Id<Link> getId() {
		return linkDelegate.getId();
	}

	public Attributes getAttributes() {
		return linkDelegate.getAttributes();
	}

}
