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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author amit, ihab
 */

public class BerlinSpatialPlots {
	private static final Logger log = Logger.getLogger(BerlinSpatialPlots.class);

    //private final String runDir = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/";
    //private final String runId = "berlin-v5.2-1pct";
	
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

    private final double countScaleFactor;
    private final double gridSize;
    private final double smoothingRadius;

	private static final double xMin = 4565039.;
	private static final double xMax = 4632739.; 
	private static final double yMin = 5801108.;
    private static final double yMax = 5845708.;

    private BerlinSpatialPlots(final double gridSize, final double smoothingRadius, final double countScaleFactor) {
        this.gridSize = gridSize;
        this.smoothingRadius = smoothingRadius;
        this.countScaleFactor = countScaleFactor;
    }

	public static void main(String[] args) {

        InputArguments inputArguments = new InputArguments();
        JCommander.newBuilder().addObject(inputArguments).build().parse(args);

        BerlinSpatialPlots plots = new BerlinSpatialPlots(inputArguments.gridSize, inputArguments.smoothingRadius, inputArguments.countScaleFactor);
        plots.writeEmissionsToCSV(inputArguments.config, inputArguments.events, inputArguments.outputFile);
    }

    private void writeEmissionsToCSV(Path configPath, Path eventsPath, Path outputPath) {

        Config config = ConfigUtils.loadConfig(configPath.toString());
		config.plans().setInputFile(null);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        double binSize = config.qsim().getEndTime() - config.qsim().getStartTime(); // this results in only one time bin
        Network network = scenario.getNetwork();

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(gridSize)
                .withTimeBinSize(binSize)
                .withNetwork(network)
                .withBounds(createBoundingBox())
                .withSmoothingRadius(smoothingRadius)
                .withCountScaleFactor(countScaleFactor)
                .withGridType(EmissionGridAnalyzer.GridType.Square)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(eventsPath.toString());

        log.info("Writing to csv...");
        writeGridToCSV(timeBins, Pollutant.NOX, outputPath);
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, Pollutant pollutant, Path outputPath) {

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputPath.toString()), CSVFormat.TDF)) {
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

    private Geometry createBoundingBox() {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });
    }

    private static class InputArguments {

        @Parameter(names = {"-gridSize", "-gs"})
        private double gridSize = 250;

        @Parameter(names = {"-smoothingRadius", "-sr"})
        private double smoothingRadius = 500;

        @Parameter(names = {"-countScaleFactor", "-csf"})
        private double countScaleFactor = 10;

        @Parameter(names = {"-events"}, required = true)
        private Path events;

        @Parameter(names = {"-config"}, required = true)
        private Path config;

        @Parameter(names = {"-output"}, required = true)
        private Path outputFile;
    }
}