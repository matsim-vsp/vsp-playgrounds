package playground.santiago.utils;

import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
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


/**
 * Only to write the list of tolled links of the cordon schemes. The tolled highways are not considered
 *
 */
public class GetTolledLinks {
	private static final Logger log = Logger.getLogger(GetTolledLinks.class);
	
	String netFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/network/network_merged_cl.xml.gz";
	
	String cordonShapeFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/policies/cordon_triangle/modifiedCordon/modifiedTriangleEPSG32719.shp";
	String schemeName = "triangleCordon";


	String outFile = "../../../policyCaseAnalysis/0_General/tolledLinksFrom" + schemeName + ".txt";

	Network net;
	Collection<SimpleFeature> featuresInCordon;
	Set<Id<Link>> cordonInLinks;
	Set<Id<Link>> cordonOutLinks;
	
	private void run() {
		

			
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(cordonShapeFile);
		featuresInCordon = shapeReader.getFeatureSet();
		
		net = NetworkUtils.createNetwork();
		new MatsimNetworkReader(net).readFile(netFile);
		
		fillCordonLinkSet();
		removeAndAddSomeLinksFromCordonLinkSet();
		writeCordonLinkSets(this.outFile);
	}

	private void fillCordonLinkSet() {
		cordonInLinks = new HashSet<Id<Link>>();
		cordonOutLinks = new HashSet<Id<Link>>();
		
		for(Link link : net.getLinks().values()){
			if(link.getAllowedModes().contains(TransportMode.pt)){
				continue;
			} else {
				Coord fromNode = link.getFromNode().getCoord();
				Coord toNode = link.getToNode().getCoord();

				if(isFeatureInShape(fromNode)){
					if(!isFeatureInShape(toNode)){
						cordonOutLinks.add(link.getId());
					}
				}
				if(!isFeatureInShape(fromNode)){
					if(isFeatureInShape(toNode)){
						cordonInLinks.add(link.getId());
					}
				}
			}
		}
	}

	private boolean isFeatureInShape(Coord coord) {
		boolean isInShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(SimpleFeature feature : featuresInCordon){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
	
	//Be aware of this special method! It will not work with another net file
	private void removeAndAddSomeLinksFromCordonLinkSet(){
		if(schemeName.substring(0,1).equals("o")){
			//removing...
			cordonOutLinks.remove(Id.createLinkId("18442"));
			cordonInLinks.remove(Id.createLinkId("18441"));
			//adding...
			cordonOutLinks.add(Id.createLinkId("14132"));
			//done.
		} else if(schemeName.substring(0,1).equals("t")) {
		//Everything is ok.

		}
	}
	
	private void writeCordonLinkSets(String outFile){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\ttype\n");
			
			for (Id<Link> id: cordonInLinks){
				writer.write(id+"\t"+"InLink"+"\n");
			}
			
			for (Id<Link> id: cordonOutLinks){
				writer.write(id+"\t"+"OutLink"+"\n");
			}
			
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
		
	}
	


	public static void main(String[] args) {
		GetTolledLinks ccs = new GetTolledLinks();
		ccs.run();
	}
}
