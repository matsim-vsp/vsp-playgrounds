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

	public static final String purposeLabel = "D_ARE";
	public static final Set<String> workPurposeValues = new LinkedHashSet<>(Arrays.asList("2"));
	public static final Set<String> otherPurposeValues = new LinkedHashSet<>(Arrays.asList("3", "4", "5", "6", "7", "8",
			"9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
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

		for (Person person : handler.id2person.values()) {
			// if (person.hasOnlySimpleTours() && person.isRoundTrip()) {

			System.out.println(person);

			// }
		}

		System.out.println("... DONE");
	}

}
