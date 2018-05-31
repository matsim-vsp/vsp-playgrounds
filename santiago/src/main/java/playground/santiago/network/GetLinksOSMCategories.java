package playground.santiago.network;

import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;



/**
 * Useful to return a map of link_ids and their respective categories given the network_merged_cl.xml version.
 * Only links within the "greater Santiago area" are considered.
 */

public class GetLinksOSMCategories {
	
	String shapeFile;
	Network network;
	Map<Id<Link>, String> linkOSMCategories;
	Set<Id<Link>> linksInGreatSantiago;
		
	public GetLinksOSMCategories(String shapeFile, Network network){
		this.shapeFile = shapeFile;
		this.network = network;
		processLinks();
	}
	
	private void processLinks(){
		
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(shapeFile);
		Collection<SimpleFeature> features = shapeReader.getFeatureSet();
			
		linkOSMCategories = new HashMap<>();
		linksInGreatSantiago = new HashSet<>();

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.pt)){
				//OMITTING...
			} else if (!isFeatureInBoundingBox(features,link)) {
				//OMITTING...
			} else {								
				Id<Link> linkId = link.getId();
				linksInGreatSantiago.add(linkId);
				
				double linkCapacity = link.getCapacity();				
				double modifiedLinkSpeed = link.getFreespeed();				
				double linkLanes = link.getNumberOfLanes();				
				double capacityPerLane = linkCapacity/linkLanes;
				
				String category;				
				double originalLinkSpeed;
								
				double thirtyKms = ((double) 30*1000)/3600;
				double fourtyKms = ((double) 40*1000)/3600;
				double fiftyKms  = ((double) 50*1000)/3600;
				double sixtyKms  = ((double) 60*1000)/3600;
				
				//ugly coding 1
				if (modifiedLinkSpeed <= 0.5*thirtyKms) {
					originalLinkSpeed = thirtyKms;
				} else if (modifiedLinkSpeed <= 0.5*fourtyKms) {
					originalLinkSpeed = fourtyKms;
				} else if ((modifiedLinkSpeed <= 0.5*fiftyKms && linkLanes==1)||(modifiedLinkSpeed <= 0.75*fiftyKms && linkLanes==2)||(modifiedLinkSpeed <= fiftyKms && linkLanes>2)) {
					originalLinkSpeed = fiftyKms;
				} else if ((modifiedLinkSpeed <= 0.5*sixtyKms && linkLanes==1)||(modifiedLinkSpeed <= 0.75*sixtyKms && linkLanes==2)||(modifiedLinkSpeed <= sixtyKms && linkLanes>2)) {
					originalLinkSpeed = sixtyKms;
				} else if (modifiedLinkSpeed > sixtyKms) {
					originalLinkSpeed = modifiedLinkSpeed;
				} else {
					System.out.println(linkId.toString() + "-" + String.valueOf(modifiedLinkSpeed) + "-" + String.valueOf(linkLanes) + "-" + String.valueOf(sixtyKms));
					throw new RuntimeException("Link not considered...");				
				}
				
				//ugly coding 2
				if (capacityPerLane == 2000) {
					if (originalLinkSpeed == sixtyKms) {
						category = "trunk";						
					} else {
						category = "motorway";
					}
				} else if (capacityPerLane == 1500) {
					if(originalLinkSpeed == fiftyKms) {
						category = "trunk_link_primary_link";
					} else if (originalLinkSpeed == sixtyKms) {
						category = "primary";
					} else {
						category = "motorway_link";
					}
					
				} else if (capacityPerLane == 1000) {
					category = "secondary";
				} else {
					category = "tertiary";					
				}
				linkOSMCategories.put(linkId,category);
			}
		}
	}
	
	private boolean isFeatureInBoundingBox(Collection<SimpleFeature> features, Link link) {
		
		boolean isInBoundingBox = false;

		Coord fromNode = link.getFromNode().getCoord();
		Coord toNode = link.getToNode().getCoord();
		
		GeometryFactory factory = new GeometryFactory();
		Geometry fromNodeGeo = factory.createPoint(new Coordinate(fromNode.getX(), fromNode.getY()));
		Geometry toNodeGeo = factory.createPoint(new Coordinate(toNode.getX(), toNode.getY()));

		for(SimpleFeature feature : features){
			//Both, fromNode and toNode should be INSIDE the bounding box in order to be considered.
			if(((Geometry) feature.getDefaultGeometry()).contains(fromNodeGeo) && ((Geometry) feature.getDefaultGeometry()).contains(toNodeGeo)){
				isInBoundingBox = true;
				break;
			}
		}

		return isInBoundingBox;
	}
		
	public Map<Id<Link>, String> getLinkOSMCategories(){
		return linkOSMCategories;
	}
	
	public Set<Id<Link>> getLinksInGreatSantiago(){
		return linksInGreatSantiago;
	}
	
//	public static void main (String[]args){
//		String netFile = "../../runs-svn/santiago/baseCase1pct/outputOfStep1/output_network.xml.gz";
//		String shapeFile = "/home/leonardo/Desktop/Thesis_BackUp/distancesByCategory/0_inputs/boundingBox.shp";
//		String outputFile = "../../runs-svn/santiago/baseCase1pct/outputOfStep1/analysis/linksCategories/linksByCategory.txt";
//		
//		Network network = NetworkUtils.createNetwork();
//		new MatsimNetworkReader(network).readFile(netFile);
//		GetLinksOSMCategories analyzer = new GetLinksOSMCategories(shapeFile,network);
//		Map<Id<Link>, String> linkOSMCategories = analyzer.getLinkOSMCategories();
//		
//		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
//			writer.write("linkId\tCategory\n");
//
//			for(Id<Link> linkId : linkOSMCategories.keySet()){				
//				String category = linkOSMCategories.get(linkId);
//						writer.write(linkId+"\t"+category+"\n");
//			}
//
//			writer.close();
//		} catch (Exception e) {
//			throw new RuntimeException("Data is not written. Reason "+e );
//		}
//		
//	}
	
}
