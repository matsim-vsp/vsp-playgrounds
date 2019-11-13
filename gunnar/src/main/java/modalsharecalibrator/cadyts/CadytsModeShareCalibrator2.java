/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package modalsharecalibrator.cadyts;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import floetteroed.cadyts.calibrators.Calibrator;
import floetteroed.cadyts.demand.PlanBuilder;
import floetteroed.cadyts.measurements.SingleLinkMeasurement;
import floetteroed.cadyts.measurements.SingleLinkMeasurement.TYPE;
import floetteroed.cadyts.supply.SimResults;
import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@SuppressWarnings("serial")
public class CadytsModeShareCalibrator2 extends Calibrator<String> {

	// -------------------- CONSTANTS --------------------

	private static final int DAYLENGTH_S = (int) Units.S_PER_D;
	private static final SingleLinkMeasurement.TYPE MODEMEASUREMENT_TYPE = SingleLinkMeasurement.TYPE.COUNT_VEH;

	private final String flowAnalysisPrefix;
	private final double estimatedTotalTripCnt;
	private final double reproductionWeight;
	private final Map<String, Double> mode2totalSimCnt = new LinkedHashMap<>();
	private final Map<String, Double> mode2totalRealCnt = new LinkedHashMap<>(); // for logging

	// -------------------- CONSTRUCTION --------------------

	public CadytsModeShareCalibrator2(final String logPath, final double estimatedTotalTripCnt,
			final double reproductionWeight) {
		super(Paths.get(logPath, "cadyts.log").toString(), null, DAYLENGTH_S);
		super.setStatisticsFile(Paths.get(logPath, DEFAULT_STATISTICS_FILE).toString());
		this.flowAnalysisPrefix = Paths.get(logPath, "calibrated-flows").toString();
		super.setMinStddev(1e-9, MODEMEASUREMENT_TYPE);
		super.setCountFirstLink(true);
		super.setCountLastLink(true);
		this.estimatedTotalTripCnt = estimatedTotalTripCnt;
		this.reproductionWeight = reproductionWeight;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addModeMeasurement(final String mode, final double share) {
		final double cnt = share * this.estimatedTotalTripCnt;
		super.addMeasurement(mode, 0, DAYLENGTH_S, cnt, (Math.sqrt(cnt) / this.reproductionWeight),
				MODEMEASUREMENT_TYPE);
		this.mode2totalRealCnt.put(mode, cnt); // for logging
	}

	public void addToDemand(final Map<String, Integer> mode2personCnt) {
		final PlanBuilder<String> planBuilder = this.newPlanBuilder();
		for (Map.Entry<String, Integer> entry : mode2personCnt.entrySet()) {
			for (int i = 0; i < entry.getValue(); i++) {
				planBuilder.addEntry(entry.getKey(), 0);
			}
			this.mode2totalSimCnt.put(entry.getKey(),
					this.mode2totalSimCnt.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
		}
		super.addToDemand(planBuilder.getResult());
	}

	public void afterNetworkLoading(final int it) {

		final double fact = this.estimatedTotalTripCnt
				/ this.mode2totalSimCnt.values().stream().mapToDouble(v -> v).sum();
		final SimResults<String> simResults = new SimResults<String>() {
			@Override
			public double getSimValue(final String mode, final int startTime_s, final int endTime_s, final TYPE type) {
				if ((startTime_s == 0) && (endTime_s == DAYLENGTH_S) && (MODEMEASUREMENT_TYPE.equals(type))) {
					return fact * mode2totalSimCnt.getOrDefault(mode, 0.0);
				} else {
					throw new RuntimeException("mode = " + mode + ", startTime_s = " + startTime_s + ", endTime_s = "
							+ endTime_s + ", type = " + type);
				}
			}
		};
		super.setFlowAnalysisFile(this.flowAnalysisPrefix + "." + it + ".log");
		super.afterNetworkLoading(simResults);

		Logger.getLogger(this.getClass()).info("---------- MODAL SHARES (PCT) ----------");
		Logger.getLogger(this.getClass()).info("mode\tsimulated\treal");
		for (String mode : this.mode2totalSimCnt.keySet()) {
			Logger.getLogger(this.getClass())
					.info(mode + "\t"
							+ (simResults.getSimValue(mode, 0, DAYLENGTH_S, MODEMEASUREMENT_TYPE)
									/ this.estimatedTotalTripCnt * 100.0)
							+ "\t" + (this.mode2totalRealCnt.get(mode) / this.estimatedTotalTripCnt * 100.0));
		}
		Logger.getLogger(this.getClass()).info("----------------------------------------");

		this.mode2totalSimCnt.clear();
	}
}
