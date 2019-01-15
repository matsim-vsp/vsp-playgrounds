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
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;

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

    private final double countScaleFactor;
    private final double gridSize;
    private final double smoothingRadius;
	private final int noOfBins = 1;

	private static final double xMin = 4565039.;
	private static final double xMax = 4632739.; 
	private static final double yMin = 5801108.; 
	private static final double yMax = 5845708.; 
	
	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:31468");

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
        //plots.writeEmissionToCells(inputArguments.config, inputArguments.events, inputArguments.outputFile);
    }

    private void writeEmissionsToCSV(Path configPath, Path eventsPath, Path outputPath) {

        //Config config = ConfigUtils.loadConfig(runDir + runId + ".output_config.xml");
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
                .withGridType(EmissionGridAnalyzer.GridType.Hexagonal)
                .build();

        //TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(runDir + runId + "." + config.controler().getLastIteration() + ".emission.events.offline.xml.gz");
        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(eventsPath.toString());

        log.info("Writing to csv...");
        writeGridToCSV(timeBins, Pollutant.NOX, outputPath);
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, Pollutant pollutant, Path outputPath) {

        //String filename = runDir + "/air-pollution-analysis/spatialPlots/" + noOfBins + "timeBins/" + "viaData_NOX_" + EmissionGridAnalyzer.GridType.Square + "_" + gridSize + "_" + smoothingRadius + "_line.csv";

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

    public void writeEmissionToCells(Path configPath, Path eventsPath, Path outputPath) {
        Map<Double, Map<Id<Link>, SortedMap<String, Double>>> linkEmissions;

        // setting of input data
        SpatialDataInputs inputs = new SpatialDataInputs(SpatialDataInputs.LinkWeightMethod.line);
        inputs.setBoundingBox(xMin, xMax, yMin, yMax);
        inputs.setTargetCRS(targetCRS);
        inputs.setGridInfo(GeneralGrid.GridType.SQUARE, gridSize);
        inputs.setSmoothingRadius(smoothingRadius);

        SpatialInterpolation plot = new SpatialInterpolation(inputs, runDir + "/air-pollution-analysis/spatialPlots/" + noOfBins + "timeBins/");

        Config config = ConfigUtils.loadConfig(configPath.toString());
        config.plans().setInputFile(null);

        final String emissionEventsFile = runId + "." + config.controler().getLastIteration() + ".emission.events.offline.xml.gz";

        EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(config.qsim().getEndTime(), eventsPath.toString(), noOfBins);
        emsLnkAna.preProcessData();
        emsLnkAna.postProcessData();
        linkEmissions = emsLnkAna.getLink2TotalEmissions();

        Scenario sc = ScenarioUtils.loadScenario(config);

        EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
        writer.openWriter(outputPath.toString());

        for (double time : linkEmissions.keySet()) {
            int counter = 0;

            for (Link l : sc.getNetwork().getLinks().values()) {
                Id<Link> id = l.getId();
                if (counter % 1000 == 0.)
                    log.info("link #" + counter + " // " + (int) (100 * (counter / (double) sc.getNetwork().getLinks().size())) + "%");
                if (plot.isInResearchArea(l)) {
                    double emiss = 0;
                    if (linkEmissions.get(time).containsKey(id)) {
                        emiss = countScaleFactor * linkEmissions.get(time).get(id).get(WarmPollutant.NOX.getText());
                    }
                    plot.processLink(l, emiss);

                }
                counter++;
            }
            writer.writeData(time, plot.getCellWeights());
            plot.reset();
        }
        writer.closeWriter();
    }

    private class EmissionTimebinDataWriter {

        BufferedWriter writer;

        void openWriter(final String outputFile) {
            writer = IOUtils.getBufferedWriter(outputFile);
            try {
                writer.write("timebin\t centroidX \t centroidY \t weight \n");
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason " + e);
            }
        }

        void writeData(final double timebin, final Map<Point, Double> cellWeights) {
            try {
                for (Point p : cellWeights.keySet()) {
                    writer.write(timebin + "\t" + p.getCentroid().getX() + "\t" + p.getCentroid().getY() + "\t" + cellWeights.get(p) + "\n");
                }
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason " + e);
            }
        }

        void closeWriter() {
            try {
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason " + e);
            }
        }
    }
}