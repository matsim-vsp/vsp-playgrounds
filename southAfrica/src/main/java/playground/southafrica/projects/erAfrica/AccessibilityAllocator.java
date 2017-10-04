/* *********************************************************************** *
 * project: org.matsim.*
 * AccessibilityAllocator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.projects.erAfrica;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.filter.expression.ThisPropertyAccessorFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.utilities.Header;

/**
 * Class to allocate the econometric accessibility, calculated by Dominik
 * Ziemke in the accessibility contrib, to individual households. The coordinate
 * reference system is currently hard-coded.
 * 
 * @author jwjoubert
 *
 */
public class AccessibilityAllocator {
	private static AccessMode mode;
	final private static Logger LOG = Logger.getLogger(AccessibilityAllocator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AccessibilityAllocator.class.toString(), args);
		
		String householdsFile = args[0];
		String householdAttributesFiles = args[1];
		String econometricAccessibility = args[2];
		String householdAccessibility = args[3];
		mode = AccessMode.valueOf(args[4]); 
		QuadTree<Double> qt = parseEconometricAccessibility(econometricAccessibility);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(householdsFile);
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.readFile(householdAttributesFiles);
		
		processHouseholds(sc, qt, householdAccessibility);
		
		Header.printFooter();
	}
	
	private static void processHouseholds(Scenario sc, QuadTree<Double> qt, String output){
		LOG.info("Processing households...");
		
		/* The coordinate transformation is currently hard-coded. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO19,
				TransformationFactory.WGS84);
		
		Counter counter = new Counter("   households # ");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("hhId,hhInc,x,y,lon,lat,accessibility");
			bw.newLine();
			
			for(Household hh : sc.getHouseholds().getHouseholds().values()){
				Object o = sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "homeCoord");
				Coord homeCoord = null;
				if(o instanceof Coord){
					homeCoord = (Coord) o;
				} else{
					LOG.error("Not a Coord!!");
				}
				Coord cWgs = ct.transform(homeCoord);
				
				String s = String.format("%s,%.0f,%.0f,%.0f,%.6f,%.6f,%.4f\n", 
						hh.getId().toString(),
						hh.getIncome().getIncome(),
						homeCoord.getX(), homeCoord.getY(),
						cWgs.getX(), cWgs.getY(),
						qt.getClosest(homeCoord.getX(), homeCoord.getY()));
				bw.write(s);
				
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Crash writing!!");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Crash closing!!");
			}
		}
		
		LOG.info("Done processing households.");
	}
	
	
	private static QuadTree<Double> parseEconometricAccessibility(String filename){
		LOG.info("Parsing econometric accessibilities...");
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		/*FIXME Currently hard-coded: Dominik produces Cape Town accessibilities
		 * in some UTM CRS. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				"EPSG:22235", TransformationFactory.HARTEBEESTHOEK94_LO19);
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header */
			
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[0]);
				double y = Double.parseDouble(sa[1]);
				Coord cUTM = CoordUtils.createCoord(x, y);
				Coord cH = ct.transform(cUTM);
				
				xMin = Math.min(xMin, cH.getX());
				xMax = Math.max(xMax, cH.getX());
				yMin = Math.min(yMin, cH.getY());
				yMax = Math.max(yMax, cH.getY());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Crash reading!!");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Crash closing!!");
			}
		}
		QuadTree<Double> qt = new QuadTree<Double>(xMin, yMin, xMax, yMax);
		
		br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header */
			
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[0]);
				double y = Double.parseDouble(sa[1]);
				Coord cUTM = CoordUtils.createCoord(x, y);
				Coord cH = ct.transform(cUTM);
				String accessibilityString = sa[mode.getColumn()];
				double accessibility = Double.parseDouble(accessibilityString);
				
				qt.put(cH.getX(), cH.getY(), accessibility);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Crash reading!!");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Crash closing!!");
			}
		}
		
		LOG.info("Done parsing econometric accessibilities... (" + qt.size() + " entries)");
		return qt;
	}
	

	private enum AccessMode {
		freespeed, car, walk, bike;
		
		private int getColumn(){
			switch (this) {
			case freespeed:
				return 2;
			case car:
				return 3;
			case walk:
				return 4;
			case bike:
				return 5;
			default:
				throw new RuntimeException("Unknown mode: " + this.toString());
			}
		}
	}
}
