/*
 * Decompiled with CFR 0_122.
 * 
 * Could not load the following classes:
 *  org.matsim.api.core.v01.Id
 *  org.matsim.api.core.v01.network.Network
 *  org.matsim.core.population.routes.NetworkRoute
 *  org.matsim.core.population.routes.RouteUtils
 */
package playground.jmolloy.nodeModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import playground.jmolloy.nodeModel.AbstractNodeSimulation;

public class TriangleNodeSimulation
extends AbstractNodeSimulation {
    public TriangleNodeSimulation(String network_filename, String outputDirectory, int timesteps) {
        super(network_filename, outputDirectory, timesteps);
    }

    public static void main(String[] args) {
        String network_filename = "networks/network_triangle.xml";
        String outputDirectory = "output/triangle";
        int timesteps = 2;
        TriangleNodeSimulation nodeSim = new TriangleNodeSimulation(network_filename, outputDirectory, timesteps);
        NetworkRoute route1 = RouteUtils.createNetworkRoute(Arrays.asList(new Id[]{Id.createLinkId((String)"x_in_1"), Id.createLinkId((String)"x_out_1")}), (Network)nodeSim.getNetwork());
        NetworkRoute route2 = RouteUtils.createNetworkRoute(Arrays.asList(new Id[]{Id.createLinkId((String)"x_in_2"), Id.createLinkId((String)"x_out_2")}), (Network)nodeSim.getNetwork());
        NetworkRoute route3 = RouteUtils.createNetworkRoute(Arrays.asList(new Id[]{Id.createLinkId((String)"x_in_3"), Id.createLinkId((String)"x_out_3")}), (Network)nodeSim.getNetwork());
        HashMap<NetworkRoute, Integer> flows = new HashMap<NetworkRoute, Integer>();
        flows.put(route1, 2000);
        flows.put(route2, 2000);
        flows.put(route3, 2000);
        nodeSim.addFlows(flows, true);
        nodeSim.run();
    }
}
