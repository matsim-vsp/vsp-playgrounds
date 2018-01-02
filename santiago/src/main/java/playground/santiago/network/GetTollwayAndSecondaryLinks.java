package playground.santiago.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Useful to return a set of link objects given a shape file with link-geometries. Id's are from the network_merged_cl.xml version. 
 * If network is changed, this class will be <strong>useless</strong>.
 *
 */

public class GetTollwayAndSecondaryLinks {
	
	String shapeFile;
	String networkFile;
	Set tollwayLinks;
	Set secondaryLinks;
		
	public GetTollwayAndSecondaryLinks(String shapeFile, String networkFile){
		this.shapeFile = shapeFile;
		this.networkFile = networkFile;
		processLinks();
	}
	
	private void processLinks(){

		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(shapeFile);
		Collection<SimpleFeature> features = shapeReader.getFeatureSet();		
		tollwayLinks = new HashSet<Id<Link>>();

		for(SimpleFeature feature : features){
			tollwayLinks.add(Id.createLinkId((String) feature.getAttribute(1)));
		}

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = (Network) scenario.getNetwork();
		secondaryLinks = new HashSet<Id<Link>>();

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.pt)){
				//OMITTING...
			} else {				
				Id<Link> linkId = link.getId();				
				if (tollwayLinks.contains(linkId)){
					//OMITTING...
				} else {
					secondaryLinks.add(Id.createLinkId(linkId));
				}
			}
		}
	}
		
	public Set<Id<Link>> getTollwayLinks(){
		return tollwayLinks;
	}
	
	public Set<Id<Link>> getSecondaryLinks(){
		return secondaryLinks;
	}
	
	
}
