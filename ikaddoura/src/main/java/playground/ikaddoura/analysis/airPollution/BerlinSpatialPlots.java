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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author amit, ihab
 */

public class BerlinSpatialPlots {
	private static final Logger log = Logger.getLogger(BerlinSpatialPlots.class);	
	
	private final String runDir = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/";
	private final String runId = "berlin-v5.2-1pct";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_bc-0/";
//	private final String runId = "bc-0";

//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_bc-0/";
//	private final String runId = "bc-0";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/";
//	private final String runId = "berlin-v5.2-1pct";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_savA-0d/";
//	private final String runId = "savA-0d";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_savA-2d/";
//	private final String runId = "savA-2d";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_savA-3d/";
//	private final String runId = "savA-3d";
	
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
		
		gridSize = 1000;
		smoothingRadius = 500;
		
		BerlinSpatialPlots plots = new BerlinSpatialPlots();
        plots.writeEmissionsToCSV();
    }

    private void writeEmissionsToCSV() {

		Config config = ConfigUtils.loadConfig(runDir + runId + ".output_config.xml");
		config.plans().setInputFile(null);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        double binSize = config.qsim().getEndTime() - config.qsim().getStartTime(); // this results in only one time bin
        Network network = scenario.getNetwork();

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(gridSize)
                .withTimeBinSize(binSize)
                .withNetwork(network)
                .withSmoothingRadius(smoothingRadius)
                .withCountScaleFactor(countScaleFactor)
                .withGridType(EmissionGridAnalyzer.GridType.Square)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(runDir + runId + "." + config.controler().getLastIteration() + ".emission.events.offline.xml.gz");

        log.info("Writing to csv...");
        writeGridToCSV(timeBins, Pollutant.NOX);
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, Pollutant pollutant) {

        String filename = runDir + "/air-pollution-analysis/spatialPlots/" + noOfBins + "timeBins/" + "viaData_NOX_" + EmissionGridAnalyzer.GridType.Square + "_" + gridSize + "_" + smoothingRadius + "_line.csv";
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filename), CSVFormat.DEFAULT)) {
            printer.printRecord("timeBinStartTime", "centroidX", "centroidY", "weight");

            for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : bins.getTimeBins()) {
                final double timeBinStartTime = bin.getStartTime();
                for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {
                    double weight = cell.getValue().containsKey(pollutant) ? cell.getValue().get(pollutant) : 0;
                    printer.printRecord(timeBinStartTime, cell.getCoordinate().x, cell.getCoordinate().y, weight);
				}
			}
        } catch (IOException e) {
            e.printStackTrace();
		}
	}
}