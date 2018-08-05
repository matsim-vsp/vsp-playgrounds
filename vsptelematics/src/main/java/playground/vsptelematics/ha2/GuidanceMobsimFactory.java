/* *********************************************************************** *
 * project: org.matsim.*
 * GuidanceMobsimFactory
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
package playground.vsptelematics.ha2;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimBuilder;

import com.google.inject.Provider;

import playground.vsptelematics.common.TelematicsConfigGroup;
import playground.vsptelematics.common.TelematicsConfigGroup.Infotype;


/**
 * @author dgrether
 * 
 */
@Singleton
public class GuidanceMobsimFactory implements Provider<Mobsim>, ShutdownListener {

	private double equipmentFraction;
	private Guidance guidance = null;
	private Infotype type;
	private String outfile;
	private GuidanceRouteTTObserver ttObserver;
	private Scenario scenario;
	private EventsManager eventsManager;

	@Inject
	GuidanceMobsimFactory(GuidanceRouteTTObserver ttObserver, Scenario scenario, EventsManager eventsManager, OutputDirectoryHierarchy outputDirectoryHierarchy) {
		TelematicsConfigGroup tcg = ConfigUtils.addOrGetModule(scenario.getConfig(), TelematicsConfigGroup.GROUPNAME, TelematicsConfigGroup.class);
		this.equipmentFraction = tcg.getEquipmentRate();
		this.type = tcg.getInfotype();
		this.outfile = outputDirectoryHierarchy.getOutputFilename("guidance.txt");
		this.ttObserver = ttObserver;
		this.scenario = scenario;
		this.eventsManager = eventsManager;
	}
	
	private void initGuidance(Network network){
		switch (type){
		case reactive:
			this.guidance = new ReactiveGuidance(network, outfile);
			break;
		case estimated:
			this.guidance = new EstimatedGuidance(network, outfile);
			break;
		default:
			throw new IllegalStateException("Guidance type " + type + " is not known!");
		}
	}

	Mobsim createMobsim(Scenario scenario, EventsManager eventsManager) {
		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		return new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.removeModule(PopulationModule.class) //
				.addModule(new GuidanceQSimModule(guidance, equipmentFraction, ttObserver)) //
				.build(scenario, eventsManager);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.guidance.notifyShutdown();
	}

	@Override
	public Mobsim get() {
		return createMobsim(scenario, eventsManager);
	}
}
