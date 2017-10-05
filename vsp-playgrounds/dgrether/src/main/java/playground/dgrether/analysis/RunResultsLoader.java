/* *********************************************************************** *
 * project: org.matsim.*
 * RunDirectoryLoader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesReader;

import java.io.File;


/**
 * 
 * @author dgrether
 *
 */
public class RunResultsLoader {
	
	private String directory;
	private String runId;
	private OutputDirectoryHierarchy outputDir;
	private Network network;
	private Population population;
	private Lanes lanes;
	private SignalsData signals;
	
	public RunResultsLoader(String path, String runId) {
		this.directory = path;
		this.runId = runId;
		initialize();
	}
	
	private void initialize(){
		File dir = new File(this.directory);
		if (! (dir.exists() && dir.isDirectory())) {
			throw new IllegalArgumentException("Run directory " + this.directory + " can not be found");
		}
		this.outputDir = new OutputDirectoryHierarchy(
				this.directory,
				this.runId, OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists,
				false);
		String configFilename = outputDir.getOutputFilename(Controler.FILENAME_CONFIG);
	}

	public String getEventsFilename(Integer iteration){
		return this.outputDir.getIterationFilename(iteration, Controler.FILENAME_EVENTS_XML);
	}
	
	public Network getNetwork(){
		if (this.network == null) {
			String nf = this.outputDir.getOutputFilename(Controler.FILENAME_NETWORK);
			this.network = loadNetwork(nf);
		}
		return this.network;
	}

	private Network loadNetwork(String path) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(sc.getNetwork());
		nr.readFile(path);
		return sc.getNetwork();
	}
	
	public Population getPopulation(){
		if (this.population == null) {
			String pf = this.outputDir.getOutputFilename(Controler.FILENAME_POPULATION);
			this.population = this.loadPopulation(pf);
		}
		return this.population;
	}

	private Population loadPopulation(String path) {
		ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(ConfigUtils.createConfig()) ;
		builder.setNetwork(this.network) ;
		Scenario sc = builder.build() ;
		PopulationReader pr= new PopulationReader(sc);
		pr.readFile(path);
		return sc.getPopulation();
	}
	
	//untested
	public Lanes getLanes() {
		if (this.lanes == null){
			String lf = this.outputDir.getOutputFilename(Controler.FILENAME_LANES);
			this.lanes = this.loadLanes(lf);
		}
		return this.lanes;
	}
	
	private Lanes loadLanes(String path) {
		Config c = ConfigUtils.createConfig();
		c.qsim().setUseLanes(true);
		Scenario sc = ScenarioUtils.createScenario(c);
		LanesReader reader = new LanesReader(sc);
		reader.readFile(path);
		return (Lanes) sc.getScenarioElement(Lanes.ELEMENT_NAME);
	}
	
	public SignalsData getSignals() {
		if (this.signals == null) {
			String systemsfile = this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS);
			String groupsfile =  this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS);
			String controlfile = this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL);
			this.signals = loadSignals(systemsfile, groupsfile, controlfile);
		}
		return this.signals;
	}

	private SignalsData loadSignals(String systemspath, String groupspath, String controlpath) {
		Config c = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(c, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setSignalSystemFile(systemspath);
		signalSystemsConfigGroup.setSignalGroupsFile(groupspath);
		signalSystemsConfigGroup.setSignalControlFile(controlpath);
		SignalsDataLoader loader = new SignalsDataLoader(c);
		return loader.loadSignalsData();
	}

	public final String getIterationPath(int iteration) {
		return outputDir.getIterationPath(iteration);
	}

	public final String getIterationFilename(int iteration, String filename) {
		return outputDir.getIterationFilename(iteration, filename);
	}

	public final String getOutputFilename(String filename) {
		return outputDir.getOutputFilename(filename);
	}

	public String getOutputPath() {
		return outputDir.getOutputPath();
	}
	
	
	
}
