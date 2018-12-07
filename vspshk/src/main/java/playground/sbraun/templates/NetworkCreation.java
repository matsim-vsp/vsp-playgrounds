package playground.sbraun.templates;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;


//Create a simple triangle Network
public class NetworkCreation {
	
	//Capacity
	private static final long NORMAL = 30;	
	//Length of all links in m
	private static final long DIST = 1000;
	//Traveltime default
	private static final long TRAVTIME = 60;
	
	
	
	public static void main(String args[]) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network net = scenario.getNetwork();
		NetworkFactory netfac = net.getFactory();
		
		
		//create Nodes
		Node n0 = netfac.createNode(Id.createNodeId(0), new Coord(0,0));
		net.addNode(n0);
		Node n1 = netfac.createNode(Id.createNodeId(2), new Coord(1000,750));
		net.addNode(n1);
		Node n2 = netfac.createNode(Id.createNodeId(3), new Coord(0,1500));
		net.addNode(n2);
		
		//create Links
		Link l1 = netfac.createLink(Id.createLinkId("0_1"),n0,n1);
		setattribut(l1,NORMAL,DIST,TRAVTIME);
		net.addLink(l1);
		Link l2 = netfac.createLink(Id.createLinkId("1_0"),n1,n0);
		setattribut(l2,NORMAL,DIST,TRAVTIME);
		net.addLink(l2);
		Link l3 = netfac.createLink(Id.createLinkId("0_2"),n0,n2);
		setattribut(l3,NORMAL,DIST,TRAVTIME);
		net.addLink(l3);
		Link l4 = netfac.createLink(Id.createLinkId("2_0"),n2,n0);
		setattribut(l4,NORMAL,DIST,TRAVTIME);
		net.addLink(l4);
		Link l5 = netfac.createLink(Id.createLinkId("1_2"),n1,n2);
		setattribut(l5,NORMAL,DIST,TRAVTIME);
		net.addLink(l5);
		Link l6 = netfac.createLink(Id.createLinkId("2_1"),n2,n1);		
		setattribut(l6,NORMAL,DIST,TRAVTIME);
		net.addLink(l6);

		
		new NetworkWriter(net).write("C:/Users/braun/Desktop/Test/input/inputsimple_triangle_network.xml");
	}
	
	public static void setattribut(Link link, double capacity, double length, double traveltime) {
		link.setCapacity(capacity);
		link.setLength(length);
		link.setFreespeed(link.getLength() / (traveltime - 0.1));	
	}
}
