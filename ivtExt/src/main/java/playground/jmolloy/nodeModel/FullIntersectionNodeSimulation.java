/*
 * Decompiled with CFR 0_122.
 * 
 * Could not load the following classes:
 *  org.matsim.api.core.v01.Id
 *  org.matsim.api.core.v01.network.Network
 *  org.matsim.core.population.routes.NetworkRoute
 *  org.matsim.core.population.routes.RouteUtils
 *  org.matsim.core.utils.collections.Tuple
 */
package playground.jmolloy.nodeModel;

import java.lang.invoke.LambdaMetafactory;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.Tuple;
import playground.jmolloy.nodeModel.AbstractNodeSimulation;

public class FullIntersectionNodeSimulation
extends AbstractNodeSimulation {
    private final int[][] flows;

    public FullIntersectionNodeSimulation(int[][] flows, String network_filename, String outputDirectory, int timesteps) {
        super(network_filename, outputDirectory, timesteps);
        this.flows = flows;
    }

    public static void main(String[] args) {
        String network_filename = "networks/network_tampere.xml";
        String outputDirectory = "output/tampere";
        int timesteps = 5;
        int[][] flows = new int[][]{{1, 6, 50}, {1, 7, 150}, {1, 8, 300}, {2, 5, 100}, {2, 7, 300}, {2, 8, 1600}, {3, 5, 100}, {3, 6, 100}, {3, 8, 600}, {4, 5, 100}, {4, 6, 800}, {4, 7, 800}};
        int[][] gibb_flows = new int[][]{{1, 6, 525}, {1, 7, 150}, {1, 8, 75}, {2, 5, 450}, {2, 7, 300}, {2, 8, 900}, {3, 5, 300}, {3, 6, 300}, {3, 8, 225}, {4, 5, 225}, {4, 6, 1275}, {4, 7, 75}};
        FullIntersectionNodeSimulation nodeSim = new FullIntersectionNodeSimulation(flows, network_filename, outputDirectory, timesteps);
        Map<NetworkRoute, Integer> route_flows = Arrays.stream(flows).map(a -> {
            Id<Link> inLinkId = Id.createLinkId((String)("x_in_" + a[0]));
            Id<Link> outLinkId = Id.createLinkId((String)("x_out_" + a[1]));
            NetworkRoute route = RouteUtils.createNetworkRoute(Arrays.asList(inLinkId, outLinkId), (Network)nodeSim.getNetwork());
            return new Tuple<>(route, a[2]);
        }
        ).collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
        nodeSim.addFlows(route_flows, true);
        nodeSim.run();
    }
}
