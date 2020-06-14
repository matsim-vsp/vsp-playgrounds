package playground.gleich.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCutOut {
	
	private final static Logger LOG = Logger.getLogger(NetworkCutOut.class);
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0) { // TODO
			throw new IllegalArgumentException("Arguments array must have a length of 0, ....!"); // TODO
		}
		
		// Local use
		String inputNetworkFileName = "/home/gregor/Projekte/Melbourne/Test_Scenario_Input/Y2015_network_wPnR_base_20170627.xml";
		String outputNetworkFileName = "/home/gregor/Projekte/Melbourne/variableAccessRouter/TestScenario/network.xml.gz";
		double minX = 359643.352775;
		double minY = 5781362.259957;
		double maxX = 370188.044963;
		double maxY = 5787993.587861;
		
		cutOutNetwork(inputNetworkFileName, outputNetworkFileName, minX, minY, maxX, maxY);
	}
		
	public static void cutOutNetwork (String inputNetworkFileName, String outputNetworkFileName, 
			double minX, double minY, double maxX, double maxY) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(network);
		
		matsimNetworkReader.readFile(inputNetworkFileName);
		
		// Extract pt network (NetworkCleaner would otherwise remove it, because it is not connected to the road network)
		// Problems arise if one of the pt modes listed below uses links of the road network instead of running completely independently
		NetworkFilterManager nfm = new NetworkFilterManager(network);
		nfm.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) {
					if ((l.getFromNode().getCoord().getX() > minX && l.getFromNode().getCoord().getY() > minY && 
							l.getFromNode().getCoord().getX() < maxX && l.getFromNode().getCoord().getY() < maxY) ||
							(l.getToNode().getCoord().getX() > minX && l.getToNode().getCoord().getY() > minY && 
							l.getToNode().getCoord().getX() < maxX && l.getToNode().getCoord().getY() < maxY)) {
						return true;
					} else {
						return false;
					}
				}
				else return false;
			}
		});
		Network filteredCarNetwork = nfm.applyFilters();
		
		new NetworkCleaner().run(filteredCarNetwork);
		
		NetworkUtils.writeNetwork(filteredCarNetwork, outputNetworkFileName);
		LOG.info("Done the network file.");
	}

}
