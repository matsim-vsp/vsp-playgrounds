/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ikaddoura.analysis.airPollution;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs.LinkWeightMethod;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;

/**
 * @author amit, ihab
 */

public class BerlinSpatialPlots {
	private static final Logger log = Logger.getLogger(BerlinSpatialPlots.class);	

//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_bc-0c/";
//	private final String runId = "bc-0c";
	
	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_savA-2/";
	private final String runId = "savA-2";
	
	private final double countScaleFactor = 10;
	private static double gridSize ;
	private static double smoothingRadius ;
	private final int noOfBins = 1;

	private static final double xMin = 4565039.;
	private static final double xMax = 4632739.; 
	private static final double yMin = 5801108.; 
	private static final double yMax = 5845708.; 
	
	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:31468");

	public static void main(String[] args) {
		
		gridSize = 250;
		smoothingRadius = 500;
		
		BerlinSpatialPlots plots = new BerlinSpatialPlots();
		plots.writeEmissionToCells();
	}

	public void writeEmissionToCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissions = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.SQUARE, gridSize);
		inputs.setSmoothingRadius(smoothingRadius);

		SpatialInterpolation plot = new SpatialInterpolation(inputs, runDir + "/analysis/spatialPlots/"+noOfBins+"timeBins/");
		
		Config config = ConfigUtils.loadConfig(runDir + runId + ".output_config.xml");
		config.plans().setInputFile(null);
		
		final String emissionEventsFile = runId + "." + config.controler().getLastIteration() + ".emission.events.offline.xml.gz";

		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(config.qsim().getEndTime(), runDir + emissionEventsFile, noOfBins);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissions = emsLnkAna.getLink2TotalEmissions();

		Scenario sc = ScenarioUtils.loadScenario(config);

		EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
		writer.openWriter(runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/"+"viaData_NOX_"+GridType.SQUARE+"_"+gridSize+"_"+smoothingRadius+"_line.txt");

		for (double time :linkEmissions.keySet()){
			int counter = 0;
			
			for (Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();
				if (counter % 1000 == 0.) log.info("link #" + counter + " // " + (int) (100 * (counter / (double) sc.getNetwork().getLinks().size())) + "%" );
				if(plot.isInResearchArea(l)){
					double emiss = 0;
					if (linkEmissions.get(time).containsKey(id)) {
						emiss = countScaleFactor * linkEmissions.get(time).get(id).get(WarmPollutant.NOX.toString());
					}
					plot.processLink(l,  emiss);
					
				}
				counter++;
			}
			writer.writeData(time, plot.getCellWeights());
			plot.reset();
		}
		writer.closeWriter();
	}

	private class EmissionTimebinDataWriter{

		BufferedWriter writer;
		public void openWriter (final String outputFile){
			writer = IOUtils.getBufferedWriter(outputFile);
			try {
				writer.write("timebin\t centroidX \t centroidY \t weight \n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void writeData(final double timebin, final Map<Point,Double> cellWeights){
			try {
				for(Point p : cellWeights.keySet()){
					writer.write(timebin+"\t"+p.getCentroid().getX()+"\t"+p.getCentroid().getY()+"\t"+cellWeights.get(p)+"\n");
				}
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void closeWriter (){
			try {
				writer.close();	
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}
	}
}