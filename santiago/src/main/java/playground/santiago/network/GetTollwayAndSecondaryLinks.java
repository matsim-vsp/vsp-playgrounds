package playground.santiago.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;


/**
 * Useful to return a set of link objects given a shape file with link-geometries. Id's are from the network_merged_cl.xml version. If network is changed, this class will be useless.
 *
 */

public class GetTollwayAndSecondaryLinks {
	
	public static Set<Id<Link>> getTollwayLinks(String shapeFile){
	
			
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(shapeFile);
		Collection<SimpleFeature> features = shapeReader.getFeatureSet();
		
		Set linkIds = new HashSet<Id<Link>>();
		
		for(SimpleFeature feature : features){
			linkIds.add(Id.createLinkId((String) feature.getAttribute(1)));
		}
			
		return linkIds;
	}


}
