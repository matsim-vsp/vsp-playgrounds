package playground.lu.drtAnalysis;

import java.util.Map;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;

public interface AvailabilityAnalysisHandler {
	Map<DrtZone, Double> getAllDayAvailabilityRate();

	Map<DrtZone, Double> getPeakHourAvailabilityRate();
}
