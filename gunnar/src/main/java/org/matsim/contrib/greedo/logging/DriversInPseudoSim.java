package org.matsim.contrib.greedo.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPseudoSim implements Statistic<LogDataWrapper> {

	@Override
	public String label() {
		return DriversInPseudoSim.class.getSimpleName();
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getDriversInPseudoSim());
	}

}
