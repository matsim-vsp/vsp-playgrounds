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
package signals;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.builder.SignalModelFactoryImpl;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaConfig;
import org.matsim.contrib.signals.controller.sylvia.SylviaPreprocessData;
import org.matsim.contrib.signals.controller.sylvia.SylviaSignalPlan;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.DownstreamSensor;
import org.matsim.contrib.signals.sensor.LinkSensorManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;


/**
 * combined signal model factory that works for all provided signal controller, so far: planbased, sylvia, downstream, laemmer, gershenson
 * 
 * @author tthunig
 *
 */
public class CombinedSignalModelFactory implements SignalModelFactory {

	private static final Logger log = Logger.getLogger(CombinedSignalModelFactory.class);

	private SignalModelFactory delegate;

	private Map<String, Provider<SignalController>> signalControlProvider = new HashMap<>();
	
	@Inject
	CombinedSignalModelFactory(Scenario scenario, SylviaConfig sylviaConfig, 
			LinkSensorManager sensorManager, DownstreamSensor downstreamSensor, GershensonConfig gershensonConfig) {
		delegate = new SignalModelFactoryImpl(scenario, sylviaConfig, sensorManager, downstreamSensor);
		
		// prepare signal controller provider
		signalControlProvider.put(DownstreamPlanbasedSignalController.IDENTIFIER, new DownstreamPlanbasedSignalController.SignalControlProvider(downstreamSensor));
		signalControlProvider.put(FullyAdaptiveLaemmerSignalController.IDENTIFIER, new FullyAdaptiveLaemmerSignalController.SignalControlProvider(sensorManager, scenario, downstreamSensor));
		signalControlProvider.put(GershensonSignalController.IDENTIFIER, new GershensonSignalController.SignalControlProvider(sensorManager, scenario, gershensonConfig));
	}

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return this.delegate.createSignalSystem(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		if (signalControlProvider.containsKey(controllerIdentifier)) {
			log.info("Creating " + controllerIdentifier);
			SignalController signalControl = signalControlProvider.get(controllerIdentifier).get();
			signalControl.setSignalSystem(signalSystem);
			return signalControl;
		}
		return this.delegate.createSignalSystemController(controllerIdentifier, signalSystem);
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		DatabasedSignalPlan plan = (DatabasedSignalPlan) this.delegate.createSignalPlan(planData);
		if (planData.getId().toString().startsWith(SylviaPreprocessData.SYLVIA_PREFIX)) {
			return new SylviaSignalPlan(plan);
		}
		return plan;
	}

}
