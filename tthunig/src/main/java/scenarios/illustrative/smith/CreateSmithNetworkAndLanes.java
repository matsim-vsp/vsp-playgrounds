/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package scenarios.illustrative.smith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;

/**
 * Class to create network and lanes for Smith' scenario.
 *
 * Set the capacity before calling the method createNetworkWithLanes().
 *
 * Network:
 *
 *                           (3)         (6)
 *                          ´   `       ´   `
 *                        ´       `   ´       `
 * (0)-------(1)-------(2)         (5)         (8)-------(9)-------(10)
 *                        `       ´   `       ´
 *		                    `   ´       `   ´
 *			                 (4)         (7)
 *
 *
 *
 * @author tthunig
 * 
 */
final class CreateSmithNetworkAndLanes {

	private static final Logger log = Logger.getLogger(CreateSmithNetworkAndLanes.class);

	private Scenario scenario;

	private static final double LINK_LENGTH = 300.0; // m
	private static final double FREESPEED = 10.0; // m/s

	private double capacity = 3600; // veh/h

	private Map<String, Id<Link>> links = new HashMap<>();

	public CreateSmithNetworkAndLanes(Scenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Creates the Network for Smith' scenario and the required lanes.
     */
	public void createNetworkWithLanes() {
        log.info("Create network and lanes ...");

		Network net = this.scenario.getNetwork();
		if (net.getCapacityPeriod() != 3600.0){
			throw new IllegalStateException();
		}
		net.setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();

		// create nodes
		double scale = LINK_LENGTH;
		Node n0, n1, n2, n3, n4, n5, n6, n7, n8, n9, n10;
        net.addNode(n0 = fac.createNode(Id.create(0, Node.class), new Coord(0.0, 0.0)));
		net.addNode(n1 = fac.createNode(Id.create(1, Node.class), new Coord(1.0 * scale, 0.0)));
		net.addNode(n2 = fac.createNode(Id.create(2, Node.class), new Coord(2.0 * scale, 0.0)));
		net.addNode(n3 = fac.createNode(Id.create(3, Node.class), new Coord(3.0 * scale, 1.0 * scale)));
		net.addNode(n4 = fac.createNode(Id.create(4, Node.class), new Coord(3.0 * scale, -1.0 * scale)));
		net.addNode(n5 = fac.createNode(Id.create(5, Node.class), new Coord(4.0 * scale, 0.0)));
		net.addNode(n6 = fac.createNode(Id.create(6, Node.class), new Coord(5.0 * scale, 1.0 * scale)));
		net.addNode(n7 = fac.createNode(Id.create(7, Node.class), new Coord(5.0 * scale, -1.0 * scale)));
		net.addNode(n8 = fac.createNode(Id.create(8, Node.class), new Coord(6.0 * scale, 0.0)));
		net.addNode(n9 = fac.createNode(Id.create(9, Node.class), new Coord(7.0 * scale, 0.0)));
		net.addNode(n10 = fac.createNode(Id.create(10, Node.class), new Coord(8.0 * scale, 0.0)));
		
		// create links
		initLinkIds();
        Link l = fac.createLink(links.get("0_1"), n0, n1);
        setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
        net.addLink(l);
		l = fac.createLink(links.get("1_2"), n1, n2);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_3"), n2, n3);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_4"), n2, n4);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("3_5"), n3, n5);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("4_5"), n4, n5);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_6"), n5, n6);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_7"), n5, n7);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("6_8"), n6, n8);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("7_8"), n7, n8);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("8_9"), n8, n9);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("9_10"), n9, n10);
		setLinkAttributes(l, capacity, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		createLanes();
	}

	private void initLinkIds() {
        links.put("0_1", Id.create("0_1", Link.class));
		links.put("1_2", Id.create("1_2", Link.class));
		links.put("2_3", Id.create("2_3", Link.class));
		links.put("2_4", Id.create("2_4", Link.class));
		links.put("3_5", Id.create("3_5", Link.class));
		links.put("4_5", Id.create("4_5", Link.class));
		links.put("5_6", Id.create("5_6", Link.class));
		links.put("5_7", Id.create("5_7", Link.class));
		links.put("6_8", Id.create("6_8", Link.class));
		links.put("7_8", Id.create("7_8", Link.class));
		links.put("8_9", Id.create("8_9", Link.class));
		links.put("9_10", Id.create("9_10", Link.class));
	}

	private static void setLinkAttributes(Link link, double capacity,
			double length, double freeSpeed) {
		
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(freeSpeed + 0.1);
	}

	/**
	 * create lanes at node 5 to forbid turns there
	 */
	private void createLanes() {
		Lanes lanes = this.scenario.getLanes();
		LanesFactory fac = lanes.getFactory();

		// create link assignment of link 3_5
		LanesToLinkAssignment linkAssignment = fac.createLanesToLinkAssignment(links.get("3_5"));
		LanesUtils.createAndAddLane(linkAssignment, fac, Id.create("3_5.ol", Lane.class), capacity, LINK_LENGTH, 0, 1,
				null, Collections.singletonList(Id.create("3_5", Lane.class)));
		LanesUtils.createAndAddLane(linkAssignment, fac, Id.create("3_5", Lane.class), capacity, 100, 0, 1,
				Collections.singletonList(links.get("5_7")), null);
		lanes.addLanesToLinkAssignment(linkAssignment);

		// create link assignment of link 4_5
		linkAssignment = fac.createLanesToLinkAssignment(links.get("4_5"));
		LanesUtils.createAndAddLane(linkAssignment, fac, Id.create("4_5.ol", Lane.class), capacity, LINK_LENGTH, 0, 1,
				null, Collections.singletonList(Id.create("4_5", Lane.class)));
		LanesUtils.createAndAddLane(linkAssignment, fac, Id.create("4_5", Lane.class), capacity, 100, 0, 1,
				Collections.singletonList(links.get("5_6")), null);
		lanes.addLanesToLinkAssignment(linkAssignment);
	}

	public void writeNetworkAndLanes(String directory) {
		new NetworkWriter(scenario.getNetwork()).write(directory + "network.xml");
		new LanesWriter(scenario.getLanes()).write(directory + "lanes.xml");
	}

    /**
     * Adapts the link capacity to the number of persons.
     *
     * @param numberOfPersons demand of each OD pair
     */
	public void setCapacity(double numberOfPersons) {
		this.capacity = numberOfPersons;
	}

}
