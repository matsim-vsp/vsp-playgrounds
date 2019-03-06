package playground.santiago.network;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class NetworkChecker {
	
private static String inputDirectory = "../../shared-svn/santiago/scenario/inputForMATSim/";
private static String originalNetDir = inputDirectory + "network/network_merged_cl.xml.gz";
private static String testNetDir = inputDirectory + "AV_simulation/network_merged_cl.xml.gz";
private String outputFolder = "../../../TESIS/baseCaseAnalysis/general/";

private String cordonShapeFile = inputDirectory + "policies/cordon_outer/modifiedCordon/"
		+ "modifiedCordonEPSG32719.shp";
Collection<SimpleFeature> featuresInCordon;
private static Logger log = Logger.getLogger(NetworkChecker.class);

private Network readNetwork(String netDir) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(scenario.getNetwork()).readFile(netDir);
	Network network = (Network) scenario.getNetwork();
	return network;
}

private Set<Link> analyzeLinks(Network network) {
	
	Set<Link> zeroLengthLinks = new HashSet<Link>();
	for (Link l: network.getLinks().values()) {
		if (l.getLength()==0) {
			zeroLengthLinks.add(l);
			
		}
	}
	
	return zeroLengthLinks;
	
}

private void writeZeroLengthLinksOriginal(Set<Link> zeroLengthLinksOriginal) {
	try {
		
		PrintWriter pw = new PrintWriter (new FileWriter (outputFolder + "zeroLengthLinks.txt" ));
		pw.println("id\tcapacity\tnLanes");
		if(zeroLengthLinksOriginal.size()!=0) {
		for (Link l: zeroLengthLinksOriginal) {
			Id<Link> id = l.getId();
			double capacity = l.getCapacity();
			double nLanes = l.getNumberOfLanes();
			
			System.out.println("The capacity of the " + id + " zero-length link of the network/"
					+ "network_merged_cl.xml.gz is " + String.valueOf(capacity) 
					+ ". The number of lanes of the this link is " + String.valueOf(nLanes));
			
			pw.println(id + "\t" + capacity + "\t" + nLanes);
			}

		}
		
			pw.close();		
		} catch(IOException e){
			log.error(new Exception(e));
		}
}

private int checkLocationZeroLengthLinksOriginal(Set<Link> zeroLengthLinksOriginal) {
	int numberOfLinksInsideCordon = 0;
	ShapeFileReader shapeReader = new ShapeFileReader();
	shapeReader.readFileAndInitialize(cordonShapeFile);
	this.featuresInCordon = shapeReader.getFeatureSet();

	for(Link link : zeroLengthLinksOriginal){
			Coord fromNode = link.getFromNode().getCoord();
			Coord toNode = link.getToNode().getCoord();
			
			if(isFeatureInShape(fromNode)||isFeatureInShape(toNode)){
				System.out.println("Link " + link.getId().toString() + " is inside tolled cordon!");
				numberOfLinksInsideCordon+=1;

		}
	}
	return numberOfLinksInsideCordon;
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

public static void main(String[] args) {
	NetworkChecker nc = new NetworkChecker();
	
	Network originalNet = nc.readNetwork(originalNetDir);
	Network testNet = nc.readNetwork(testNetDir);
	
	Set<Link> zeroLengthLinksOriginal = nc.analyzeLinks(originalNet);
	Set<Link> zeroLengthLinksTest = nc.analyzeLinks(testNet);
		
	System.out.println("The number of zero-length links in the network/network_merged_cl.xml.gz is " 
	+ String.valueOf(zeroLengthLinksOriginal.size()));
	
	System.out.println("The number of zero-length links in the AV_simulation/network_merged_cl.xml.gz is " 
	+ String.valueOf(zeroLengthLinksTest.size()));
	
//	nc.writeZeroLengthLinksOriginal(zeroLengthLinksOriginal);
	
	int numberOfLinksInsideCordon = nc.checkLocationZeroLengthLinksOriginal(zeroLengthLinksOriginal);
	System.out.println("The number of links inside the the cordon is " + String.valueOf(numberOfLinksInsideCordon));
		
	}
}
