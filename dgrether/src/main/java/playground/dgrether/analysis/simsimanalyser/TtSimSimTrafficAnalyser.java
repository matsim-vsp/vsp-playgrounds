/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.simsimanalyser;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.CountSimComparison;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 */
public class TtSimSimTrafficAnalyser {

	private static final Logger log = Logger.getLogger(TtSimSimTrafficAnalyser.class);
	
	private Network loadNetwork(String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		return scenario.getNetwork();
	}
	
	
	private VolumesAnalyzer loadVolumes(Network network , String eventsFile){
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, network); // one time bin corresponds to one hour
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return va;
	}
	
	public void runAnalysis(String networkFile, String eventsFileCounts, String eventsFileSim, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		VolumesAnalyzer vaCounts = loadVolumes(network, eventsFileCounts);
		VolumesAnalyzer vaSim = loadVolumes(network, eventsFileSim);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		
		SimSimAnalysis countsAnalysis = new SimSimAnalysis();
		Map<Id, List<CountSimComparison>> countSimLinkLeaveCompMap = countsAnalysis.createCountSimComparisonByLinkId(filteredNetwork, vaCounts, vaSim);
		
		new SimSimMorningShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile + ".shp", countSimLinkLeaveCompMap);
	}


	private Network applyNetworkFilter(Network network, CoordinateReferenceSystem networkSrs) {
		log.info("Filtering network...");
		log.info("Nr links in original network: " + network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature("C:/Users/Atany/Desktop/SHK/SVN/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		FeatureNetworkLinkCenterCoordFilter filter = new FeatureNetworkLinkCenterCoordFilter(networkSrs, cottbusFeatureTuple.getSecond(), cottbusFeatureTuple.getFirst());
		netFilter.addLinkFilter(filter);
		Network fn = netFilter.applyFilters();
		log.info("Nr of links in filtered network: " + fn.getLinks().size());
		return fn;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String net = null;
		String eventsFileCountValues = null;
		String eventsFileSimValues = null;
		String outfile = null;
		String srs = null;

		if (args == null || args.length == 0){
			
			String runNr1 = "1910";
			String runNr2 = "1912";
			
			net = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/"+runNr1+".output_network.xml.gz";
			eventsFileCountValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/ITERS/it.2000/"+runNr1+".2000.events.xml.gz";
			eventsFileSimValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr2+"/ITERS/it.2000/"+runNr2+".2000.events.xml.gz";
			outfile = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/shapefiles/"+runNr2+".2000-"+runNr1+".2000_morningPeakAnalysis";

			srs = TransformationFactory.WGS84_UTM33N;			
		}
		else {
			net = args[0];
			eventsFileCountValues = args[1];
			eventsFileSimValues = args[2];
			outfile = args[3];
			srs = args[4];
		}

		TtSimSimTrafficAnalyser analyser = new TtSimSimTrafficAnalyser();
		analyser.runAnalysis(net, eventsFileCountValues, eventsFileSimValues, srs, outfile);
	}

}
