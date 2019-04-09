/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.util.PartialSort;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TaxiZoneManager implements IterationEndsListener {

	@Inject
	ZonalOccupancyAggregator aggregator;
	
	@Inject 
	SharedTaxiFareCalculator fareCalculator;
	
	@Inject
	ZonalBasedRequestValidator validator;
	
	@Inject
	ZonalSystem zonalSystem;
	
	@Inject
	MatsimServices matsimServices;
	
	
	private final DecimalFormat format = new DecimalFormat();
	private boolean headerWritten = false;
	private boolean firstRun = true;
	private double cost_km = 0.3;
	private double fix_cost_vehicle = 150;
	private int aggregateInterval = 10
			;
	Map<String, Double> zoneFaresSum = new HashMap<>();
	Map<String, Double> zoneOccupancySum = new HashMap<>();
	

	/**
	 * 
	 */
	public TaxiZoneManager() {
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(3);
		format.setGroupingUsed(false);	}
	
	

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		Map<String, Double> zoneOccupancy = aggregator.calculateZoneOccupancy();
		Map<String, Double> zoneFares = calculateZoneFares(fareCalculator.getFaresPerLink()); 
		aggregateZones(zoneOccupancy,zoneFares);
		int it = event.getIteration();
		
		writeRevenues(it,zoneFares);
		
		if (it%aggregateInterval == 0&&it>0){
			
		updateZones(it);
		zoneOccupancySum.clear();
		zoneFaresSum.clear();
		}
		
	}



	/**
	 * @param zoneOccupancy
	 * @param zoneFares
	 */
	private void aggregateZones(Map<String, Double> zoneOccupancy, Map<String, Double> zoneFares) {
		for (Entry<String, Double> e : zoneOccupancy.entrySet()){
			double occ = e.getValue();
			if (zoneOccupancySum.containsKey(e.getKey())){
				occ += zoneOccupancySum.get(e.getKey());
			}
			zoneOccupancySum.put(e.getKey(), occ);
			
		}
		
		for (Entry<String, Double> e : zoneFares.entrySet()){
			double occ = e.getValue();
			if (zoneFaresSum.containsKey(e.getKey())){
				occ += zoneFaresSum.get(e.getKey());
			}
			zoneFaresSum.put(e.getKey(), occ);
			
		}
		
	}



	private void updateZones(int it) {
		List<String> zonetable = new ArrayList<>();
		List<String> nonperformers = new ArrayList<>();
		int k = 10;
        PartialSort<String> worstZoneSort = new PartialSort<>(k);
		Map<String,Geometry> currentZones = validator.getZones();

		for (Entry<String, Double> e : zoneOccupancySum.entrySet()){
			Double occupancy = e.getValue();
			if (occupancy == null) throw new RuntimeException();
			if (occupancy.isNaN()){
				occupancy = 0.0;
			}
			Double fare = zoneFaresSum.get(e.getKey());
			if (fare == null) {
				fare = 0.0;}
			
			double performance = fare*occupancy;
			if (fare == 0.0){
				nonperformers.add(e.getKey());
			}
			double indicator;
			
			switch (zonalSystem.getOptimizationCriterion()){
			case Fare:
				indicator = fare;
				break;
			case Performance:
				indicator = performance;
				break;
			case Occupancy:
				indicator = occupancy;
				break;
			default:
				indicator = Double.NaN;
				break;
			}
				
			if (currentZones.containsKey(e.getKey())){
				Logger.getLogger(getClass()).info("adding\t"+e.getKey()+"\t"+indicator );
			worstZoneSort.add(e.getKey(), indicator);
			}
			zonetable.add(e.getKey()+";"+format.format(occupancy/aggregateInterval)+";"+format.format(e.getValue()/aggregateInterval)+";"+format.format(performance/aggregateInterval));
			JbUtils.collection2Text(zonetable,matsimServices.getControlerIO().getIterationFilename(it,"zoneperformance.csv"), "zone;occupancy;fares;performance");

		}	
		
		
		writeShape(matsimServices.getControlerIO().getIterationFilename(it,"zones.shp"), currentZones, zoneOccupancySum, zoneFaresSum);
		if (firstRun){
			for (String z : nonperformers)
			{
				currentZones.remove(z);
			}
			validator.updateZones(currentZones);
			firstRun = false;
		} else
		if (currentZones.size()>2*k){
			Logger.getLogger(getClass()).info("Removing zones in iteration "+it);
		for(String z : worstZoneSort.kSmallestElements()){
			currentZones.remove(z);
			Logger.getLogger(getClass()).info("Removing zone\t"+z);
		}
			validator.updateZones(currentZones);
		}
	}



	/**
	 * @param zoneFares
	 */
	private void writeRevenues(int iteration, Map<String, Double> zoneFares) {
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(matsimServices.getControlerIO().getOutputFilename("drt_revenues.txt"));
		int vkm = (int) (aggregator.getOverallMileage()/1000);
		int pkm = (int) (aggregator.getRevenueMileage()/1000);
		double revenue = 0;
		for (Double f : zoneFares.values()){
			revenue+=f;
		}
		double cost = fix_cost_vehicle * aggregator.getFleetSize() + vkm* cost_km;
		double profit = revenue-cost;
		double revenueperkm = revenue/vkm;
		
			try {
				if (!headerWritten){
				bw.write("iteration);vkm;pkm;revenue;revenuePerVkm;cost;profit");
				headerWritten = true;
				}
				bw.newLine();
				bw.write(iteration+";"+vkm+";"+pkm+";"+format.format(revenue)+";"+format.format(revenueperkm)+";"+cost+";"+profit);
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}



	/**
	 * @param faresPerLink
	 * @return
	 */
	private Map<String, Double> calculateZoneFares(Map<Id<Link>, MutableDouble> faresPerLink) {
		Map<String, Double> zoneFares = new HashMap<>();
		for (Entry<Id<Link>, MutableDouble> e : faresPerLink.entrySet()){
			String zone = aggregator.getZoneForLinkId(e.getKey());
			if (zone!=null){
				if (zoneFares.containsKey(zone)){
					double fare = zoneFares.get(zone)+e.getValue().getValue();
					zoneFares.put(zone, fare);
				} else zoneFares.put(zone, e.getValue().getValue());
			}
		}
		return zoneFares;
	}
	
	
	  private void writeShape(String outfile,Map<String,Geometry> currentZones, Map<String, Double> zoneOccupancy, Map<String, Double> zoneFares )
	    {
		  
		  	String gk4 = "PROJCS[\"DHDN / 3-degree Gauss-Kruger zone 4\", GEOGCS[\"DHDN\", DATUM[\"Deutsches Hauptdreiecksnetz\", SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], TOWGS84[612.4, 77.0, 440.2, -0.054, 0.057, -2.797, 2.55], AUTHORITY[\"EPSG\",\"6314\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4314\"]], PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]], PARAMETER[\"central_meridian\", 12.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 4500000.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"31468\"]]";
		  	//some weird problems with that, when running on cluster
		  	CoordinateReferenceSystem crs;
			try {
				crs = CRS.parseWKT(gk4);
				
				PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class)
						.setCrs(crs).setName("zone").addAttribute("occupancy", Double.class).addAttribute("fare", Double.class).addAttribute("perf.", Double.class).create();
				
				List<SimpleFeature> features = new ArrayList<>();
				
				
				for (Entry<String,Geometry> z :currentZones.entrySet()) {
					Object[] attribs = new Object[4];
					Double occ = zoneOccupancy.get(z.getKey());
					if (occ!=null) occ/=aggregateInterval;
					Double fare = zoneFares.get(z.getKey());
					if(fare!=null) fare/=aggregateInterval;
					attribs[0] = z.getKey();
					attribs[1] = occ  ; 
					attribs[2] = fare ;
					if(occ!=null&&fare!=null){
						attribs[3] =  occ*fare;}
					else {attribs[3] = null;}
					features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
				}
				
				ShapeFileWriter.writeGeometries(features, outfile);
			} catch (FactoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

}
