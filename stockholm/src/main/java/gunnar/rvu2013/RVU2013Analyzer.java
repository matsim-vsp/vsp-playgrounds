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
package gunnar.rvu2013;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.math.Histogram;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RVU2013Analyzer extends AbstractTabularFileHandlerWithHeaderLine {

	public static final String startRegionLabel = "D_A_SOMR";
	public static final String endRegionLabel = "D_B_SOMR";
	public static final Set<String> stockholmRegionValues = new LinkedHashSet<>(
			Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));

	public static final String purposeLabel = "H_ARE";
	public static final Set<String> workPurposeValues = new LinkedHashSet<>(Arrays.asList("2", "80"));
	public static final Set<String> otherPurposeValues = new LinkedHashSet<>(Arrays.asList("6", "7", "8", "9", "10",
			"11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
	public static final String work = "work";
	public static final String other = "other";

	public static final String personLabel = "UENR";
	public static final String tripLabel = "H_NUMMER";
	public static final String segmentLabel = "D_NUMMER";

	public static final String startTimeLabel = "D_A_KL";
	public static final String endTimeLabel = "D_B_KL";
	public static final String travelTimeLabel = "D_RESTID";

	public static final String startLocationLabel = "D_A_S"; // "D_A_PKT";
	public static final String endLocationLabel = "D_B_S"; // "D_B_PKT";

	public static final String mainModeLabel = "H_FORD";
	public static final Set<String> driverValues = new LinkedHashSet<>(
			Arrays.asList("501", "502", "503", "504", "505", "506", "507", "508", "509"));

	final static List<String> workSeq = Arrays.asList(work);
	final static List<String> otherSeq = Arrays.asList(other);
	final static List<String> workOtherSeq = Arrays.asList(work, other);
	final static List<List<String>> allSeq = Arrays.asList(workSeq, otherSeq, workOtherSeq);

	private final Map<String, Person> id2person = new LinkedHashMap<>();

	public RVU2013Analyzer() {
	}

	int work2 = 0;
	int work80 = 0;

	@Override
	public void startCurrentDataRow() {

		final String mainMode = this.getStringValue(mainModeLabel);

		final String startRegion = this.getStringValue(startRegionLabel);
		final String endRegion = this.getStringValue(endRegionLabel);
		if (driverValues.contains(mainMode) && stockholmRegionValues.contains(startRegion)
				&& stockholmRegionValues.contains(endRegion)) {

			final String purpose;
			{
				final String purposeValue = this.getStringValue(purposeLabel);

				if ("2".equals(purposeValue)) {
					this.work2++;
				} else if ("80".equals(purposeValue)) {
					this.work80++;
				}

				if (workPurposeValues.contains(purposeValue)) {
					purpose = work;
				} else if (otherPurposeValues.contains(purposeValue)) {
					purpose = other;
				} else {
					purpose = null;
				}
			}

			if (purpose != null) {

				final String personId = this.getStringValue(personLabel);
				Person person = this.id2person.get(personId);
				if (person == null) {
					person = new Person(personId);
					this.id2person.put(personId, person);
				}

				final String startTime = this.getStringValue(startTimeLabel);
				final String endTime = this.getStringValue(endTimeLabel);
				if ((startTime.length() >= 3) && (endTime.length() >= 3)) {

					final String tripId = this.getStringValue(tripLabel);
					final String segmentId = this.getStringValue(segmentLabel);

					final String duration = this.getStringValue(travelTimeLabel);

					final String startLocation = this.getStringValue(startLocationLabel);
					final String endLocation = this.getStringValue(endLocationLabel);

					final TravelSegment segment = new TravelSegment(tripId, segmentId, startTime, endTime, duration,
							purpose, startLocation, endLocation);
					person.add(segment);
				}
			}
		}

	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");

		final RVU2013Analyzer handler = new RVU2013Analyzer();

		final String fileName = "/Users/GunnarF/OneDrive - VTI/My Data/ihop4/rvu2013/MDRE_1113_original.csv";
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.parse(fileName, handler);
		parser.setOmitEmptyColumns(false);

		final TourSequenceTimeStructures timeStructures = new TourSequenceTimeStructures();

		double[] timeBounds_s = new double[23];
		for (int i = 0; i < timeBounds_s.length; i++) {
			timeBounds_s[i] = (1.0 + i) * 3600;
		}
		final Histogram startWork1Tour = new Histogram(timeBounds_s);
		final Histogram startOther1Tour = new Histogram(timeBounds_s);
		final Histogram startWork2Tour = new Histogram(timeBounds_s);
		final Histogram startOther2Tour = new Histogram(timeBounds_s);

		final Histogram durWork1Tour = new Histogram(timeBounds_s);
		final Histogram durOther1Tour = new Histogram(timeBounds_s);
		final Histogram durWork2Tour = new Histogram(timeBounds_s);
		final Histogram durOther2Tour = new Histogram(timeBounds_s);

		int relevantCnt = 0;
		for (Person person : handler.id2person.values()) {

			final List<Tour> tours = person.tours(person.startLocation());
			final boolean relevant;
			if (tours.size() == 1) {

				final String firstPurpose = tours.get(0).uniquePurpose();
				final Tuple<Integer, Integer> startAndDur = tours.get(0).mainActivityStartAndDuration_s();
				if (startAndDur == null) {
					relevant = false;
				} else if (work.equals(firstPurpose)) {
					relevant = true;
					startWork1Tour.add(startAndDur.getFirst());
					durWork1Tour.add(startAndDur.getSecond());
					timeStructures.add(firstPurpose, startAndDur.getFirst(), startAndDur.getSecond());
				} else if (other.equals(firstPurpose)) {
					relevant = true;
					startOther1Tour.add(startAndDur.getFirst());
					durOther1Tour.add(startAndDur.getSecond());
					timeStructures.add(firstPurpose, startAndDur.getFirst(), startAndDur.getSecond());
				} else {
					relevant = true;
				}

			} else if (tours.size() == 2) {

				final Tuple<Integer, Integer> startAndDur1 = tours.get(0).mainActivityStartAndDuration_s();
				final Tuple<Integer, Integer> startAndDur2 = tours.get(1).mainActivityStartAndDuration_s();
				if (!work.equals(tours.get(0).uniquePurpose()) || !other.equals(tours.get(1).uniquePurpose())
						|| startAndDur1 == null || startAndDur2 == null) {
					relevant = false;
				} else {
					relevant = true;
					startWork2Tour.add(startAndDur1.getFirst());
					durWork2Tour.add(startAndDur1.getSecond());
					startOther2Tour.add(startAndDur2.getFirst());
					durOther2Tour.add(startAndDur2.getSecond());
					timeStructures.add(work, startAndDur1.getFirst(), startAndDur1.getSecond(), other,
							startAndDur2.getFirst(), startAndDur2.getSecond());
				}

			} else {
				relevant = false;
			}

			if (relevant) {
				System.out.println(person);
				relevantCnt++;
			}
		}

		System.out.println(relevantCnt);

		System.out.println();

		System.out.println("start work in 1 tour");
		System.out.println(startWork1Tour);
		System.out.println();

		System.out.println("dur work in 1 tour");
		System.out.println(durWork1Tour);
		System.out.println();

		System.out.println("start other in 1 tour");
		System.out.println(startOther1Tour);
		System.out.println();

		System.out.println("dur other in 1 tour");
		System.out.println(durOther1Tour);
		System.out.println();

		System.out.println("start work in 2 tour");
		System.out.println(startWork2Tour);
		System.out.println();

		System.out.println("dur work in 2 tour");
		System.out.println(durWork2Tour);
		System.out.println();

		System.out.println("start other in 2 tour");
		System.out.println(startOther2Tour);
		System.out.println();

		System.out.println("dur other in 2 tour");
		System.out.println(durOther2Tour);
		System.out.println();

		System.out.println(timeStructures);
		
		System.out.println("... DONE");
	}

}
