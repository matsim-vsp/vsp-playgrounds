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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Matrix;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

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
	private double cost_km = 0.3;
	private double fix_cost_vehicle = 150;

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
		List<String> zonetable = new ArrayList<>();
		int k = 10;
        PartialSort<String> worstZoneSort = new PartialSort<>(k);
        
		for (Entry<String, Double> e : zoneOccupancy.entrySet()){
			Double occupancy = e.getValue();
			if (occupancy == null) throw new RuntimeException();
			if (occupancy == Double.NaN){
				occupancy = 0.0;
			}
			Double fare = zoneFares.get(e.getKey());
			if (fare == null) {
				fare = 0.0;}
			
			double performance = fare*occupancy;
			
			double indicator;
			
			switch (zonalSystem.getOptimizationCriterion()){
			case Fare:
				indicator = fare;
			case Performance:
				indicator = performance;
			case Occupancy:
				indicator = occupancy;
			default:
				indicator = Double.NaN;
			}
				
			
			worstZoneSort.add(e.getKey(), indicator);
			
			zonetable.add(e.getKey()+";"+format.format(occupancy)+";"+format.format(e.getValue())+";"+format.format(performance));
			
		}	
		
		JbUtils.collection2Text(zonetable,matsimServices.getControlerIO().getIterationFilename(event.getIteration(),"zoneperformance.csv"), "zone;occupancy;fares;performance");
		
		Map<String,Geometry> currentZones = validator.getZones();
		writeShape(matsimServices.getControlerIO().getIterationFilename(event.getIteration(),"zones.shp"), currentZones, zoneOccupancy, zoneFares);
		writeRevenues(event.getIteration(),zoneFares);
		if (currentZones.size()>2*k){
		for(String z : worstZoneSort.retriveKSmallestElements()){
			currentZones.remove(z);
			validator.updateZones(currentZones);
		}}
		
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
		
			try {
				if (!headerWritten){
				bw.write("iteration);vkm;pkm;revenue;cost;profit");
				headerWritten = true;
				}
				bw.newLine();
				bw.write(iteration+";"+vkm+";"+pkm+";"+revenue+";"+cost+";"+profit);
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
		  
		  	CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.DHDN_GK4);

			PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class)
					.setCrs(crs).setName("zone").addAttribute("occupancy", Double.class).addAttribute("fare", Double.class).addAttribute("perf.", Double.class).create();

			List<SimpleFeature> features = new ArrayList<>();
			
			
			for (Entry<String,Geometry> z :currentZones.entrySet()) {
                Object[] attribs = new Object[4];
                Double occ = zoneOccupancy.get(z.getKey());
                Double fare = zoneFares.get(z.getKey());
                attribs[0] = z.getKey();
                attribs[1] = occ; 
                attribs[2] = fare;
                if(occ!=null&&fare!=null){
                attribs[3] =  occ*fare;}
                else {attribs[3] = null;}
				features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
			}

			ShapeFileWriter.writeGeometries(features, outfile);
	    }

}
