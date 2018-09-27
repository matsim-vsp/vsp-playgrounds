/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.ikaddoura.taxiPricing.prepare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * @author  jbischoff
 *
 */
public class PopulationBasedTaxiVehicleCreator {

	private String networkFile = "/Users/ihab/Documents/workspace/runs-svn/optAV_be5/input/network/berlin-5.0_network.xml.gz";
	private String shapeFile = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/Planungsraum.shp";
	private String vehiclesFilePrefix = "/Users/ihab/Documents/workspace/runs-svn/optAV_be5/input/taxi/v";
	private String populationData = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/bevoelkerung.txt";
	
	private Scenario scenario ;
	Map<String,Geometry> geometry;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();
    private final WeightedRandomSelection<String> wrs;
    CoordinateTransformation ct; 

	public static void main(String[] args) {
		for (int i = 0; i<=50000 ; i=i+5000 ){
			PopulationBasedTaxiVehicleCreator tvc = new PopulationBasedTaxiVehicleCreator();
			System.out.println(i);
			tvc.run(i);
		}
}

	public PopulationBasedTaxiVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		
		// change nodes in a way that pt links are not taken into consideration
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getId().toString().startsWith("pt_")) {
				link.getFromNode().setCoord(new Coord(4449458.462246102,4708888.241959611));
				link.getToNode().setCoord(new Coord(4449458.462246102,4708888.241959611));
			}
		}
		
		this.geometry = readShapeFileAndExtractGeometry(shapeFile, "SCHLUESSEL");	
		this.wrs = new WeightedRandomSelection<>();
		this.ct = TransformationFactory.getCoordinateTransformation("EPSG:25833", TransformationFactory.DHDN_GK4);
        readPopulationData();
	}
	
	private void readPopulationData() {
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"}); //berlin
        config.setFileName(populationData);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {

				wrs.add(row[0], Double.parseDouble(row[2]));
			}
		});
        
		
	}

	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
			Link link ;
		Point p = getRandomPointInFeature(random, geometry.get(wrs.select()));
		link = NetworkUtils.getNearestLinkExactly(((Network) scenario.getNetwork()),ct.transform( MGC.point2Coord(p)));
		
        Vehicle v = new VehicleImpl(Id.create("rt"+i, Vehicle.class), link, 5, Math.round(1), Math.round(30*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml.gz");
	}
	
	private static Point getRandomPointInFeature(Random rnd, Geometry g)
    {
        Point p = null;
        double x, y;
        do {
            x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
            y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
            p = MGC.xy2Point(x, y);
        }
        while (!g.contains(p));
        return p;
    }
	
	private static Map<String,Geometry> readShapeFileAndExtractGeometry(String filename, String key){
		
		Map<String,Geometry> geometry = new TreeMap<>();	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					String lor = ft.getAttribute(key).toString();
					geometry.put(lor, geo);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			 
		}	
		return geometry;
	}
}
