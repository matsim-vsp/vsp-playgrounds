package org.matsim.contrib.greedo.logging;

public class LambdaRealized extends PopulationAverageStatistic {

	@Override
	public String value(LogDataWrapper arg0) {
		return this.averageOrEmpty(arg0.getNumberOfReplanners().doubleValue(), arg0.getPopulationSize());
	}

}
