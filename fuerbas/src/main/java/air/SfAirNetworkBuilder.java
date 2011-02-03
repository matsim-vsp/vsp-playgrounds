package air;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class SfAirNetworkBuilder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String output = "/home/soeren/workspace/testnetzwerk";
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");

		NetworkImpl network = NetworkImpl.createNetwork();
		network.setCapacityPeriod(60.0);		//60 seconds capacity period --> 1 take-off per minute
		
		BufferedReader brAirports = new BufferedReader(new FileReader(new File("/home/soeren/workspace/OsmTest.txt")));
		BufferedReader brRoutes = new BufferedReader(new FileReader(new File("/home/soeren/workspace/cityPairs.txt")));
		
		CoordinateTransformation coordtransform =
			TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
		
		while (brAirports.ready()) {
			String oneLine = brAirports.readLine();
			String[] lineEntries = new String[2];
			lineEntries = oneLine.split("\t");
			String airportCode = lineEntries[0];
			String[] coordinates = new String[2];
			coordinates = lineEntries[1].split("y");
			String xValue = coordinates[0].substring(3);
			String yValue = coordinates[1].substring(1);
			xValue = xValue.replaceAll("\\]", "");
			xValue = xValue.replaceAll("\\[", "");
			yValue = yValue.replaceAll("\\[", "");
			yValue = yValue.replaceAll("\\]", "");
			
			Coord coord = new CoordImpl(Double.parseDouble(xValue), Double.parseDouble(yValue));
			Coord airportCoord = coordtransform.transform(coord);
			
			new SfMATSimAirport(new IdImpl(airportCode), airportCoord).createRunways(network);			
		}
		
		
		while (brRoutes.ready()) {
			String oneLine = brRoutes.readLine();
			String[] lineEntries = oneLine.split("\t");
			double length = Double.parseDouble(lineEntries[1]);
			String origin = lineEntries[0].substring(0, 3);
			String destination = lineEntries[0].substring(3, 6);
			
			
			//LÖSCHEN
//			if ((origin.equalsIgnoreCase("TXL") && destination.equalsIgnoreCase("ZRH")) || (origin.equalsIgnoreCase("ZRH") && destination.equalsIgnoreCase("TXL")) ) {
			
			Id originRunway = new IdImpl(origin+"runwayOutbound");
			Id destinationRunway = new IdImpl(destination+"runwayInbound");
			Link originToDestination = network.getFactory().createLink(new IdImpl(origin+destination), originRunway, destinationRunway);
			originToDestination.setAllowedModes(allowedModes);
			originToDestination.setCapacity(1.0);
			originToDestination.setFreespeed(750.0/3.6);
			originToDestination.setLength(length*1000.0);
			network.addLink(originToDestination);
			
//			} // HIER LÖSCHEN
			
		}
		

		
		new NetworkWriter(network).write(output + ".xml");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml");
		
		brAirports.close();
		brRoutes.close();
		
		
	}

}
