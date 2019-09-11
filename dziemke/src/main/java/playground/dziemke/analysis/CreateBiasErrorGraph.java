package playground.dziemke.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.entity.StandardEntityCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;

/**
 * @author dziemke
 */
public class CreateBiasErrorGraph {
	private final static Logger LOG = Logger.getLogger(CreateBiasErrorGraph.class);
	
	public static void main(String[] args) throws IOException {
		// Parameters
		String runId = "be_204";
		int iterationNumber = 300;
		int width=440;
		int height=330;
		String filename = "biasErrorGraph.png";
		
		// Input and output
		String inputFile = "../../runs-svn/berlin_scenario_2016/" + runId + "/ITERS/it." + iterationNumber + "/" + runId + "." + iterationNumber + ".countscompare.txt";
		String outputFile = "../../runs-svn/berlin_scenario_2016/" + runId + "/ITERS/it." + iterationNumber + "/" + runId + "." + iterationNumber + "." + filename;
		
		// Other objects
		List<CountSimComparison> countSimComparisonList = new ArrayList<CountSimComparison>();
		
		// Collect the data for the graph
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(inputFile);
			String currentLine = bufferedReader.readLine();
			
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
								
				Id<Link> linkId = Id.create(entries[0], Link.class);
				int hour = Integer.parseInt(entries[1]);
				double matsimVolume = Double.parseDouble(entries[2]);
				double countVolume = Double.parseDouble(entries[3]);
								
				CountSimComparison countSimComparison = new CountSimComparisonImpl(linkId, hour, countVolume, matsimVolume);
				countSimComparisonList.add(countSimComparison);
			}
		} catch (IOException e) {
			LOG.error(new Exception(e));
		}
		LOG.info("Done collecting data for files.");
		
		// following taken from "package org.matsim.counts.algorithms.CountSimComparisonKMLWriter"
		// BiasErrorGraph ep = new BiasErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
		
		// Create the graph
		BiasErrorGraph ep = new BiasErrorGraph(countSimComparisonList, iterationNumber, outputFile, "error graph");
		ep.createChart(0);
		
		// The following is (partially) taken from "org.matsim.counts.algorithms.graphs.helper.OutputDelegate.java"
		ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
		File graphicsFile = new File(outputFile);
		ChartUtils.saveChartAsPNG(graphicsFile, ep.getChart(), width, height, info);
		LOG.info("Done creating graphics file.");
	}
}