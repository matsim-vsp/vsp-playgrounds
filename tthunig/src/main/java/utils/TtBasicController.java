/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
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
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.binder.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;


/**
 * @author tthunig
 *
 */
public class TtBasicController {
	
	private static final boolean VIS = false;

	/**
	 * @param args the config file
	 */
	public static void main(String[] args) {
		prepareBasicControler(args[0]).run();
	}

	static Controler prepareBasicControler(String configFileName) {
		Config config = ConfigUtils.loadConfig(configFileName) ;
		
		// adjustments for live visualization
		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfvisConfig.setDrawTime(true);
		otfvisConfig.setAgentSize(80f);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
        
		// add the signals module if signal systems are used
		SignalsModule signalsModule = new SignalsModule();
		// the signals module works for planbased, sylvia and laemmer signal controller
		// by default and is pluggable for your own signal controller like this:
		signalsModule.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
				DownstreamPlanbasedSignalController.DownstreamFactory.class);
		signalsModule.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
		signalsModule.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);
		controler.addOverridingModule(signalsModule);

		// bind gershenson config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});
		
		// add live visualization module
		if (VIS) { 
			controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}
				
		// add additional bindings (analysis tools and classes that are necessary for
		// your own implementations, e.g. your own signal controllers, as e.g. the
		// config for Gershenson)
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				gershensonConfig.setMinimumGREENtime(5);
				// ... set parameters as you like
				bind(GershensonConfig.class).toInstance(gershensonConfig);
				
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
			}
		});
		return controler;
	}

}
