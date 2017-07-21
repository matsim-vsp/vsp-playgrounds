/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.cottbus.football;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.signalsystems.cottbus.CottbusFansControlerListener;
import playground.dgrether.signalsystems.cottbus.CottbusFootballAnalysisControllerListener;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFanCreator;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFootballStrings;
import playground.dgrether.signalsystems.cottbus.footballdemand.SimpleCottbusFanCreator;
import signals.CombinedSignalsModule;
import signals.sylvia.controler.DgSylviaConfig;

/**
 * @author tthunig, copied from dgrether CottbusFootballBatch
 *
 */
public class RunCottbusFootball {
	private static final Logger log = Logger.getLogger(RunCottbusFootball.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config baseConfig;
		if (args != null && args.length != 0){
			baseConfig = ConfigUtils.loadConfig(args[0]);
		} else {
			String configFileName = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/config_cap1.0.xml";
			baseConfig = ConfigUtils.loadConfig(configFileName);
		}
		baseConfig.controler().setLastIteration(0);
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(baseConfig, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
//		signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13.xml"); // TODO comment in for sylvia
		
		Scenario baseScenario = ScenarioUtils.loadScenario(baseConfig);
		// add missing scenario elements
		baseScenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(baseConfig).loadSignalsData());
		//create the output directoy
		String baseOutputDirectory = baseConfig.controler().getOutputDirectory();
		if (! baseOutputDirectory.endsWith("/")){
			baseOutputDirectory = baseOutputDirectory.concat("/");
		}
		baseOutputDirectory+= "footballFixedTime/"; // TODO adapt
		log.info("using base output directory: " + baseOutputDirectory);
//		createOutputDirectory(baseOutputDirectory); // TODO 
		Population fanPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		//initialize variables needed in the loop
		String runId = baseConfig.controler().getRunId();
		Map<Integer, Double> percentageOfFans2AverageTTMap = new HashMap<Integer, Double>();
		//fan creator
		String kreisShapeFile = "../../../shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		CottbusFanCreator fanCreator = new SimpleCottbusFanCreator(kreisShapeFile);
		//start the runs
		int increment = 5;
		for (int numberOfFootballFans = 0; numberOfFootballFans <= 100; numberOfFootballFans = numberOfFootballFans + increment){
			if (numberOfFootballFans != 0) {
				// create additional football fans (from 0 to 2000)
				Population p = fanCreator.createAndAddFans(baseScenario, 20 * increment);
				for (Person pers : p.getPersons().values()){
					fanPop.addPerson(pers);
				}
			}
		
			baseConfig.controler().setOutputDirectory(baseOutputDirectory + numberOfFootballFans + "_football_fans/");
			baseConfig.controler().setRunId(runId + "_" + numberOfFootballFans + "_football_fans");
			Controler controler = new Controler(baseScenario);
			controler.addControlerListener(new CottbusFansControlerListener(fanPop));
			//add average tt handler for football fans
			CottbusFootballAnalysisControllerListener cbfbControllerListener = new CottbusFootballAnalysisControllerListener();
			controler.addControlerListener(cbfbControllerListener);
			//add the signals module
			CombinedSignalsModule signalsModule = new CombinedSignalsModule();
			DgSylviaConfig sylviaConfig = new DgSylviaConfig();
			sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
			sylviaConfig.setSignalGroupMaxGreenScale(2);
//			sylviaConfig.setCheckDownstream(true);
			signalsModule.setSylviaConfig(sylviaConfig);
			controler.addOverridingModule(signalsModule);
			
			controler.run();
			if (cbfbControllerListener.getAverageTraveltime() != null){
				percentageOfFans2AverageTTMap.put(numberOfFootballFans, cbfbControllerListener.getAverageTraveltime());
			}
		}
		
//		try {
//			new SelectedPlans2ESRIShape(baseScenario.getPopulation(), baseScenario.getNetwork(), MGC.getCRS(TransformationFactory.WGS84_UTM33N), "/media/data/work/matsimOutput/run1219/" ).write();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		writeAverageTT(percentageOfFans2AverageTTMap, baseOutputDirectory + "average_traveltimes_last_iteration.csv");
		
	}
		
	private static void writeAverageTT(Map<Integer, Double> map, String filename) throws FileNotFoundException, IOException{
		SortedMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
		sorted.putAll(map);
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("Football fans %" + CottbusFootballStrings.SEPARATOR + "Average travel time");
		writer.newLine();
		for (Entry<Integer, Double> e : sorted.entrySet()){
			writer.write(e.getKey().toString() + CottbusFootballStrings.SEPARATOR + e.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}

	
	private static void createOutputDirectory(String outputDirectory){
		File outdir = new File(outputDirectory);
		if (outdir.exists()){
			throw new IllegalArgumentException("Output directory " + outputDirectory + " already exists!");
		}
		else {
			outdir.mkdir();
		}
	}

	
}
