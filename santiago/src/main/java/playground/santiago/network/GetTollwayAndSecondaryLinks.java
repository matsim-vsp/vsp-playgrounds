package playground.santiago.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Useful to return a set of link objects given a shape file with link-geometries. Id's are from the network_merged_cl.xml version. 
 * If network is changed, this class will be <strong>useless</strong>.
 *
 */

public class GetTollwayAndSecondaryLinks {
	
	String shapeFile;
	Network network;
	Map<Id<Link>, String> tollwayLinks;
	Set<Id<Link>> secondaryLinks;
		
	public GetTollwayAndSecondaryLinks(String shapeFile, Network network){
		this.shapeFile = shapeFile;
		this.network = network;
		processLinks();
	}
	
	private void processLinks(){

		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(shapeFile);
		Collection<SimpleFeature> features = shapeReader.getFeatureSet();		
		tollwayLinks = new HashMap<>();

		for(SimpleFeature feature : features){
			tollwayLinks.put(Id.createLinkId((String) feature.getAttribute(1)), ((String) feature.getAttribute(9)));
		}

		secondaryLinks = new HashSet<>();

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.pt)){
				//OMITTING...
			} else {				
				Id<Link> linkId = link.getId();				
				if (tollwayLinks.containsKey(linkId)){
					//OMITTING...
				} else {
					secondaryLinks.add(linkId);
				}
			}
		}
	}
		
	public Map<Id<Link>, String> getTollwayLinks(){
		return tollwayLinks;
	}
	
	public Set<Id<Link>> getSecondaryLinks(){
		return secondaryLinks;
	}
	
	
}
