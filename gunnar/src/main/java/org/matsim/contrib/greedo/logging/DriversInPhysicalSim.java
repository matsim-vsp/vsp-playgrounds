package org.matsim.contrib.greedo.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPhysicalSim implements Statistic<LogDataWrapper> {

	@Override
	public String label() {
		return DriversInPhysicalSim.class.getSimpleName();
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getDriversInPhysicalSim());
	}

}
