/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.audiAV.electric;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vsp.edvrp.etaxi.run.RunETaxiBenchmark;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;
import org.matsim.vsp.ev.dvrp.DvrpAuxConsumptionFactory;

public class RunEAVBenchmark {
	private static final double TEMPERATURE = 20;// 20 oC

	public static void run(String configFile, int runs) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new EvConfigGroup());
		createControler(config, runs).run();
	}

	public static Controler createControler(Config config, int runs) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Controler controler = RunETaxiBenchmark.createControler(config, runs);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(AuxEnergyConsumption.Factory.class).toInstance(
						new DvrpAuxConsumptionFactory(() -> TEMPERATURE, RunEAVBenchmark::isServingCustomer));
			}
		});

		return controler;
	}

	private static boolean isServingCustomer(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			switch (((TaxiTask)schedule.getCurrentTask()).getTaxiTaskType()) {
				case PICKUP:
				case OCCUPIED_DRIVE:
				case DROPOFF:
					return true;

				default:
			}
		}
		return false;
	}

	public static void main(String[] args) {
		String cfg = "../../../runs-svn/avsim_time_variant_network/" + args[0];
		run(cfg, 1);
	}
}
