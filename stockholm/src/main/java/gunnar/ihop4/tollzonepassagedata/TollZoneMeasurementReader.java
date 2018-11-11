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
package gunnar.ihop4.tollzonepassagedata;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.CountMeasurementSpecification;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.CountMeasurements;
import org.matsim.core.config.Config;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TollZoneMeasurementReader {

	static void replaceByInterpolation(final DynamicData<String> data, final int interpolateTime_s) {
		int bin = data.bin(interpolateTime_s);
		for (String key : data.keySet()) {
			final double prevValue = data.getBinValue(key, bin - 1);
			final double nextValue = data.getBinValue(key, bin + 1);
			data.put(key, bin, 0.5 * (prevValue + nextValue));
		}
	}

	static Set<String> keysWithDataOutsideOfTollTime(final DynamicData<String> data, final int tollStart_s,
			final int tollEnd_s) {
		final Set<String> result = new LinkedHashSet<>();
		for (int bin = 0; bin < data.bin(tollStart_s); bin++) {
			for (String key : data.keySet()) {
				if (data.getBinValue(key, bin) > 0) {
					result.add(key);
				}
			}
		}
		for (int bin = data.bin(tollEnd_s) + 1; bin < data.getBinCnt(); bin++) {
			for (String key : data.keySet()) {
				if (data.getBinValue(key, bin) > 0) {
					result.add(key);
				}
			}
		}
		return result;
	}

	static CountMeasurements loadLinkStr2countMeas(final Config config) {

		final int maxVehicleLength_m = 20;

		final TimeDiscretization allDayTimeDiscr = new TimeDiscretization(0, 1800, 48);
		final TimeDiscretization tollTimeOnlyTimeDiscr = new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24);
		final int guaranteedBeforeToll_s = 6 * 3600;
		final int guaranteedAfterToll_s = 19 * 3600;

		final List<String> days = Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18", "2016-10-19",
				"2016-10-20", "2016-10-25", "2016-10-26", "2016-10-27");

		final List<String> files = new ArrayList<>(days.size() * 2);
		for (String day : days) {
			for (String postfix : new String[] { "-01", "-02" }) {
				final Path path = Paths.get("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata",
						"wsp-passages-vtr-" + day + postfix + ".csv");
				files.add(path.toString());
			}
		}

		final SizeAnalyzer sizeAnalyzer = new SizeAnalyzer(maxVehicleLength_m);
		for (String file : files) {
			System.out.println(file);
			sizeAnalyzer.parse(file);
		}
		System.out.println(sizeAnalyzer.toString());

		final PassageDataAnalyzer dataAnalyzer = new PassageDataAnalyzer(allDayTimeDiscr,
				sizeAnalyzer.getRelevantProbaPerMeterLengthClass());
		for (String file : files) {
			System.out.println(file);
			dataAnalyzer.parse(file);
		}
		System.out.println(dataAnalyzer.toString());

		replaceByInterpolation(dataAnalyzer.getData(), 6 * 3600 + 15 * 60);
		replaceByInterpolation(dataAnalyzer.getData(), 18 * 3600 + 45 * 60);
		System.out.println(dataAnalyzer.toString());

		for (String key : keysWithDataOutsideOfTollTime(dataAnalyzer.getData(), 6 * 3600, 19 * 3600)) {
			System.out.print(key + "\t");
		}
		System.out.println();

		// CREATION OF ACTUAL SENSOR DATA

		final CountMeasurements measurements = new CountMeasurements(config);

		final DynamicData<String> allData = dataAnalyzer.getData();

		for (String linkStr : allData.keySet()) {

			// TODO CONTINUE HERE

			final double[] singleSensorData = new double[allDayTimeDiscr.getBinCnt()];
			for (int bin = 0; bin < allDayTimeDiscr.getBinCnt(); bin++) {
				singleSensorData[bin] = allData.getBinValue(linkStr, bin);
			}

			CountMeasurementSpecification spec = new CountMeasurementSpecification(Id.createLinkId(linkStr), null,
					null);

			measurements.addMeasurement(spec, singleSensorData);

			// if (linksWithAllDayData.contains(linkStr)) {
			// linkStr2countMeas.put(linkStr, new CountMeasurement(singleSensorData,
			// allDayTimeDiscr));
			// } else {
			// final DiscretizationChanger discretizationChanger = new
			// DiscretizationChanger(allDayTimeDiscr,
			// singleSensorData, DiscretizationChanger.DataType.TOTALS);
			// discretizationChanger.run(tollTimeOnlyTimeDiscr);
			// linkStr2countMeas.put(linkStr,
			// new CountMeasurement(discretizationChanger.getToTotalsCopy(),
			// tollTimeOnlyTimeDiscr));
			// }
		}
		
		return measurements;
	}

	public static void main(String[] args) {

		loadLinkStr2countMeas(null);

	}

}
