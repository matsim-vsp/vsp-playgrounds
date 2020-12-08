package playground.lu.drtAnalysis;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class DrtSatisfactionRateAnalysisUtils {
	public static double getSatisfactionRate(DescriptiveStatistics stats, int timeCriteria) {
		double[] sortedWaitingTime = stats.getSortedValues();
		for (int i = 0; i < sortedWaitingTime.length; i++) {
			if (sortedWaitingTime[i] > timeCriteria) {
				double numOfRequestsInZone = sortedWaitingTime.length;
				double output = i / numOfRequestsInZone;
				return output;
			}
		}
		return 1;
	}
}
