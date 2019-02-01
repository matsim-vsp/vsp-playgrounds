package org.matsim.contrib.greedo.logging;

import floetteroed.utilities.statisticslogging.Statistic;

public class LambdaRealized implements Statistic<LogDataWrapper> {

	@Override
	public String label() {
		return LambdaRealized.class.getSimpleName();
	}

	@Override
	public String value(LogDataWrapper arg0) {
		return Statistic.toString(arg0.getLambdaRealized());
	}

}
