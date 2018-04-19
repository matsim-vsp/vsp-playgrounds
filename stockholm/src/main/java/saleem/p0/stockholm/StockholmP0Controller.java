/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package saleem.p0.stockholm;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;
/**
 * A class to run P0 for Stockholm.
 * 
 * @author Mohammad Saleem
 *
 */

public class StockholmP0Controller {

	public static void main(String[] args) {
		
		String path = "./ihop2/matsim-input/config.xml";

		Config config = ConfigUtils.loadConfig(path);
		config.network().setTimeVariantNetwork(true);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
	    double samplesize = config.qsim().getStorageCapFactor();
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
		Network network = (Network)scenario.getNetwork();
		StockholmP0Helper sth = new StockholmP0Helper(network);
		String nodesfile = "./ihop2/matsim-input/NodesSingleJunction.csv";

		List<String> timednodes = sth.getPretimedNodes(nodesfile);
		
		Map<String, List<Link>> incominglinks = sth.getInLinksForJunctions(timednodes, network);
		Map<String, List<Link>> outgoinglinks = sth.getOutLinksForJunctions(timednodes, network);

		//		String pretimedxyxcords = "./ihop2/matsim-input/pretimedxyxcords.xy";
//		sth.writePretimedNodesCoordinates(nodesfile,pretimedxyxcords);
		
		NetworkFactory nf = network.getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		
//		controler.getConfig().qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
//		controler.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		controler.getConfig().qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves); // this means, using with holes AND constraining inflow from maxFlowFromFdiag.
//		
		
		controler.addControlerListener(new StockholmP0ControlListener(scenario, (Network) scenario.getNetwork(), incominglinks, outgoinglinks));
//		controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.run();
		
	}
}
