/* *********************************************************************** *
 * project: org.matsim.*
 * DreieckNModes													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.io.IOException;
import java.util.Arrays;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDAgentsGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDAgentsGeneratorControlerListner;
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDDistributionAgentsGeneratorImpl;
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDAgentsGeneratorImpl;

/**
 * @author amit after ssix
 */

public class FDModule extends AbstractModule {

	public static final Logger LOG = Logger.getLogger(FDModule.class);

	public static final double MAX_ACT_END_TIME = 1800.;

	private String runDir ;
	static boolean isUsingLiveOTFVis = false;
	private final Scenario scenario;
	private static FDNetworkGenerator fdNetworkGenerator;

	private String[] travelModes;
	private FDConfigGroup FDConfigGroup;

	public FDModule(final Scenario scenario){
		FDConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);
		fdNetworkGenerator = new FDNetworkGenerator(FDConfigGroup);
		this.scenario = scenario;
		fdNetworkGenerator.createNetwork(this.scenario.getNetwork());

		checkForConsistencyAndInitialize();
		setUpConfig();

		new ConfigWriter(scenario.getConfig()).write(this.runDir+"/output_config.xml");
		new NetworkWriter(scenario.getNetwork()).write(this.runDir+"/output_network.xml");
		new VehicleWriterV1(scenario.getVehicles()).writeFile(this.runDir+"/output_vehicles.xml");
	}

	private void checkForConsistencyAndInitialize(){
		this.runDir = scenario.getConfig().controler().getOutputDirectory();
		if(runDir==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");

		createLogFile();

		if(FDConfigGroup.getReduceDataPointsByFactor() != 1) {
			LOG.info("===============");
			LOG.warn("Number of modes for each mode type in FD will be reduced by a factor of "+ FDConfigGroup.getReduceDataPointsByFactor()+". This will not change the traffic dynamics.");
			if (scenario.getConfig().qsim().getTrafficDynamics()== QSimConfigGroup.TrafficDynamics.queue) LOG.warn("Make sure this is what you want because it will be more likely to have less or no points in congested regime in absence of queue model with holes.");
			LOG.info("===============");
		}

		travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);

		if (scenario.getVehicles().getVehicleTypes().isEmpty()) {
			if (travelModes.length==1 && travelModes [0].equals("car")) {
				LOG.warn("No vehicle information is provided for "+this.travelModes[0]+". Using default vehicle (i.e. car) with maximum speed same as" +
						"allowed speed on the link.");

				VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
            	car.setPcuEquivalents(1.0);
            	car.setMaximumVelocity( FDConfigGroup.getTrackLinkSpeed() );
            	scenario.getVehicles().addVehicleType(car);
			} else {
				throw new RuntimeException("Vehicle type information for modes "+ Arrays.toString(travelModes)+" is not provided. Aborting...");
			}
		}

		if (scenario.getConfig().controler().getOverwriteFileSetting().equals(OverwriteFileSetting.deleteDirectoryIfExists)) {
			LOG.warn("Overwrite file setting is set to "+scenario.getConfig().controler().getOverwriteFileSetting() 
					+ ", which will also remove the fundamental diagram data file. Setting it back to "+OverwriteFileSetting.overwriteExistingFiles);
			scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		}
	}

	private void setUpConfig() {
		// required when using controler
		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(home);

		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(work);

		scenario.getConfig().controler().setCreateGraphs(false);
		scenario.getConfig().controler().setDumpDataAtEnd(false);

		scenario.getConfig().qsim().setEndTime(100.0*3600.); // qsim should not go beyond 100 hrs it stability is not achieved.

		// following is necessary, in order to achieve the data points at high density
		if(this.travelModes.length==1 && this.travelModes[0].equals("car")) scenario.getConfig().qsim().setStuckTime(60.);
		else  if (this.travelModes.length==1 && this.travelModes[0].equals("truck")) scenario.getConfig().qsim().setStuckTime(180.);

		//TODO probably, following is not required anymore.
		if ( scenario.getConfig().network().isTimeVariantNetwork() ) {
			Network netImpl = scenario.getNetwork();
			netImpl.getFactory().setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		}

		StrategyConfigGroup.StrategySettings ss = new StrategyConfigGroup.StrategySettings();
		ss.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
		ss.setWeight(1.0);
		scenario.getConfig().strategy().addStrategySettings(ss);
		scenario.getConfig().strategy().setFractionOfIterationsToDisableInnovation(1.0);
	}

	@Override
	public void install() {
		bind(FDNetworkGenerator.class).toInstance(fdNetworkGenerator); // required for FDTrackMobsimAgent

		this.bindMobsim().toProvider(FDQSimProvider.class);

		bind(GlobalFlowDynamicsUpdator.class).asEagerSingleton(); //provide same instance everywhere
		addEventHandlerBinding().to(GlobalFlowDynamicsUpdator.class);

		if (FDConfigGroup.isRunningDistribution()) {
			bind(FDAgentsGenerator.class).to(FDDistributionAgentsGeneratorImpl.class);
		} else {
			bind(FDAgentsGenerator.class).to(FDAgentsGeneratorImpl.class);
		}

		bind(FDAgentsGeneratorControlerListner.class).asEagerSingleton(); //probably, not really necessary, since there is no shared information whereever it is required.
		addControlerListenerBinding().to(FDAgentsGeneratorControlerListner.class);
		bind(TerminationCriterion.class).to(FDAgentsGeneratorControlerListner.class);

		bind(FDDataWriter.class).asEagerSingleton();// necessary to access constructor arguments
		addControlerListenerBinding().to(FDDataWriter.class);

		bind(FDStabilityTester.class).asEagerSingleton();
		bind(FDDataContainer.class).asEagerSingleton();
	}

	private void createLogFile(){
		PatternLayout layout = new PatternLayout();
		String conversionPattern = " %d %4p %c{1} %L %m%n";
		layout.setConversionPattern(conversionPattern);
		FileAppender appender;
		String filename = runDir + "/fdlogfile.log";
		try {
			appender = new FileAppender(layout, filename,false);
		} catch (IOException e1) {
			throw new RuntimeException("File "+filename+" not found.");
		}
		LOG.addAppender(appender);
	}

}
