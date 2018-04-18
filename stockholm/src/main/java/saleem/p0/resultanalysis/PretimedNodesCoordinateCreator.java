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
package saleem.p0.resultanalysis;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.p0.stockholm.StockholmP0Helper;
import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;
/**
 * A class to calculate coordinates of signalised junctions using existing utility classes
 * 
 * @author Mohammad Saleem
 */
public class PretimedNodesCoordinateCreator {
	public static void main(String[] args){
		String path = "./ihop2/matsim-input/config - P0.xml";
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
		String nodesfile = "./ihop2/matsim-input/Nodes.csv";
		List<String> timednodes = sth.getPretimedNodes(nodesfile);
		String pretimedxyxcords = "./ihop2/matsim-input/pretimedxyxcords.xy";
		sth.writePretimedNodesCoordinates(nodesfile,pretimedxyxcords);

	}
}
