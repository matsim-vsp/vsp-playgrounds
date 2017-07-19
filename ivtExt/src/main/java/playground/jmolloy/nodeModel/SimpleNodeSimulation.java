/*
 * Decompiled with CFR 0_122.
 * 
 * Could not load the following classes:
 *  org.matsim.api.core.v01.Id
 *  org.matsim.api.core.v01.Scenario
 *  org.matsim.api.core.v01.network.Link
 *  org.matsim.api.core.v01.network.Network
 *  org.matsim.api.core.v01.network.Node
 *  org.matsim.core.population.routes.LinkNetworkRouteImpl
 *  org.matsim.core.population.routes.NetworkRoute
 */
package playground.jmolloy.nodeModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import playground.jmolloy.nodeModel.AbstractNodeSimulation;

public class SimpleNodeSimulation
extends AbstractNodeSimulation {
    private final int[] capacities;

    public static void main(String[] args) {
        int[] capacities = new int[]{2000, 4000, 4000, 0};
        String network_filename = "networks/network_simple_node.xml";
        String outputDirectory = "output/simple_node";
        int timesteps = 5;
        List<Id<Link>> links13 = Arrays.asList(Id.createLinkId("1"), Id.createLinkId("3"));
        List<Id<Link>> links14 = Arrays.asList(Id.createLinkId("1"), Id.createLinkId("4"));
        List<Id<Link>> links23 = Arrays.asList(Id.createLinkId("2"), Id.createLinkId("3"));
        List<Id<Link>> links24 = Arrays.asList(Id.createLinkId("2"), Id.createLinkId("4"));
        LinkNetworkRouteImpl route13 = new LinkNetworkRouteImpl(Id.createLinkId("x_1"), links13, Id.createLinkId("x_3"));
        LinkNetworkRouteImpl route14 = new LinkNetworkRouteImpl(Id.createLinkId("x_1"), links14, Id.createLinkId("x_4"));
        LinkNetworkRouteImpl route23 = new LinkNetworkRouteImpl(Id.createLinkId("x_2"), links23, Id.createLinkId("x_3"));
        LinkNetworkRouteImpl route24 = new LinkNetworkRouteImpl(Id.createLinkId("x_2"), links24, Id.createLinkId("x_4"));
        HashMap<NetworkRoute, Integer> flows = new HashMap<NetworkRoute, Integer>();
        int[] route_flows = new int[]{1000, 0, 4000, 0};
        flows.put((NetworkRoute)route13, route_flows[0]);
        flows.put((NetworkRoute)route14, route_flows[1]);
        flows.put((NetworkRoute)route23, route_flows[2]);
        flows.put((NetworkRoute)route24, route_flows[3]);
        SimpleNodeSimulation nodeSim = new SimpleNodeSimulation(flows, capacities, network_filename, outputDirectory, timesteps);
        nodeSim.editNetwork();
        nodeSim.run();
    }

    public SimpleNodeSimulation(Map<NetworkRoute, Integer> flows, int[] capacities, String networkFile, String outputDirectory, int timesteps) {
        super(networkFile, outputDirectory, timesteps);
        this.capacities = capacities;
        this.addFlows(flows, true);
    }

    private void editNetwork() {
        for (int i = 0; i < this.capacities.length; ++i) {
            int link_id = i + 1;
            Link link = (Link)this.scenario.getNetwork().getLinks().get(Id.createLinkId(link_id));
            if (this.capacities[i] == 0) {
                Link linkb = (Link)this.scenario.getNetwork().getLinks().get(Id.createLinkId("x_" + link_id));
                this.scenario.getNetwork().removeLink(link.getId());
                this.scenario.getNetwork().removeNode(linkb.getFromNode().getId());
                this.scenario.getNetwork().removeNode(linkb.getToNode().getId());
                this.scenario.getNetwork().removeLink(linkb.getId());
                continue;
            }
            link.setCapacity((double)this.capacities[i]);
        }
    }
}
