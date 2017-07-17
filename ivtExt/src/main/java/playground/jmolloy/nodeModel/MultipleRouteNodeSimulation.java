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

import java.lang.invoke.LambdaMetafactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import playground.jmolloy.nodeModel.AbstractNodeSimulation;

public class MultipleRouteNodeSimulation
extends AbstractNodeSimulation {
    public MultipleRouteNodeSimulation(String network_filename, String outputDirectory, int timesteps) {
        super(network_filename, outputDirectory, timesteps);
    }

    public static void main(String[] args) {
        String network_filename = "networks/network_single_OD_multiple_routes.xml";
        String outputDirectory = "output/multiRoute";
        int timesteps = 4;
        MultipleRouteNodeSimulation nodeSim = new MultipleRouteNodeSimulation(network_filename, outputDirectory, timesteps);
        List<String> routeStrings = Arrays.asList("x_r,1,2,5,8,x_s", "x_r,1,3,4,5,8,x_s", "x_r,1,2,6,7,8,x_s", "x_r,1,3,4,6,7,8,x_s");
        List routes = routeStrings.stream().map(s -> Arrays.stream(s.split(",")).map((Function<String, Id>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, createLinkId(java.lang.String ), (Ljava/lang/String;)Lorg/matsim/api/core/v01/Id;)()).collect(Collectors.toList())).map(ss -> RouteUtils.createNetworkRoute((List)ss, (Network)nodeSim.getNetwork())).collect(Collectors.toList());
        int[] routeFlows = new int[]{1941, 1608, 2423, 2028};
        HashMap<NetworkRoute, Integer> flows = new HashMap<NetworkRoute, Integer>();
        for (int i = 0; i < routeFlows.length; ++i) {
            flows.put((NetworkRoute)routes.get(i), routeFlows[i]);
        }
        nodeSim.addFlows(flows, true);
        nodeSim.run();
    }
}
