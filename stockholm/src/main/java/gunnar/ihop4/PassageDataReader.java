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
package gunnar.ihop4;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import cadyts.utilities.misc.Units;
import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PassageDataReader {

	public static Map<String, String> chargingPoint2segment() {
		final Map<String, String> chargingPointId2segId = new LinkedHashMap<>();

		// Tull_ID SegID
		// Danvikstull infart 1 22626
		chargingPointId2segId.put("11", "2262");
		// Danvikstull utfart 2 110210
		chargingPointId2segId.put("12", "110210");
		// Skansbron infart 3 6114
		chargingPointId2segId.put("21", "6114");
		// Skansbron utfart 4 6114
		chargingPointId2segId.put("22", "6114");
		// Skanstullbron infart 5 80449
		chargingPointId2segId.put("31", "80449");
		// Skanstullbron utfart 6 28480
		chargingPointId2segId.put("32", "28480");
		// Johanneshovbron infart 7 9216
		chargingPointId2segId.put("41", "9216");
		// Johanneshovbron utfart 8 18170
		chargingPointId2segId.put("42", "18170");
		// Liljeholmsbron infart 9 25292
		chargingPointId2segId.put("51", "25292");
		// Liljeholmsbron utfart 10 2453
		chargingPointId2segId.put("52", "2453");
		// Stora Essingen infart 11 74188
		chargingPointId2segId.put("61", "74188");
		// Stora Essingen utfart 12 53064
		chargingPointId2segId.put("62", "53064");
		// Stora Essingen infart 13 51930
		chargingPointId2segId.put("63", "51930");
		// Lilla Essingen utfart 14 44566
		chargingPointId2segId.put("72", "44566");
		// Drottningholmsvägen infart från Bromma 15 92866
		chargingPointId2segId.put("81", "92866");
		// Drottningholmsvägen utfart mot Bromma 16 77626
		chargingPointId2segId.put("82", "77626");
		// Drottningholmsvägen infart från EL 17 127169
		chargingPointId2segId.put("83", "127169");
		// Drottningholmsvägen utfart mot EL 18 124799
		chargingPointId2segId.put("84", "124799");
		// Essingeleden södergående från Bromma 19 119586
		chargingPointId2segId.put("85", "119586");
		// Essingleden norrgående mot Bromma 20 120732
		chargingPointId2segId.put("86", "120732");
		// Essingeleden södergående 21 74191
		chargingPointId2segId.put("87", "74191");
		// Essingeleden norrgående 22 52300
		chargingPointId2segId.put("88", "52300");
		// Kristineberg Avfart Essingeleden S 23 95249
		chargingPointId2segId.put("93", "95249");
		// Kristineberg Påfart Essingeleden N 24 52301
		chargingPointId2segId.put("94", "52301");
		// Kristineberg Avfart Essingeleden N 25 128236
		chargingPointId2segId.put("95", "128236");
		// Kristineberg Påfart Essingeleden S 26 128234
		chargingPointId2segId.put("96", "128234");
		// Klarastrandsleden infart 27 122017
		chargingPointId2segId.put("101", "122017");
		// Klarastrandsleden utfart 28 122017
		chargingPointId2segId.put("102", "122017");
		// Ekelundsbron infart 29 121908
		chargingPointId2segId.put("111", "121908");
		// Ekelundsbron utfart 30 121908
		chargingPointId2segId.put("112", "121908");
		// Tomtebodavägen infart NY 31 52416
		chargingPointId2segId.put("121", "52416");
		// Tomtebodavägen utfart NY 32 52416
		chargingPointId2segId.put("122", "52416");
		// Solnavägen infart 33 108353
		chargingPointId2segId.put("131", "108353");
		// Solnavägen utfart 34 113763
		chargingPointId2segId.put("132", "113763");
		// Norrtull Sveavägen utfart 36 101350
		chargingPointId2segId.put("146", "101350");

		// Norrtull tillfällig väg 38 128229
		// FIXME tullId2segId.put("", "128229");

		// Norra stationsgatan utfart 39 34743
		chargingPointId2segId.put("148", "34743");
		// Ekhagen Avfart E18S 40 54215
		chargingPointId2segId.put("211", "54215");
		// Ekhagen Påfart E18N 41 116809
		chargingPointId2segId.put("212", "116809");
		// Ekhagen Avfart E18N 42 74955
		chargingPointId2segId.put("213", "74955");
		// Ekhagen Påfart E18S 43 35466
		chargingPointId2segId.put("214", "35466");
		// Frescati infart 44 56348
		chargingPointId2segId.put("221", "56348");
		// Frescati utfart 45 56348
		chargingPointId2segId.put("222", "56348");
		// Universitetet infart 46 127555
		chargingPointId2segId.put("231", "127555");
		// Universitetet utfart 47 42557
		chargingPointId2segId.put("232", "42557");
		// Roslagstull infart 48 129132
		chargingPointId2segId.put("241", "129132");
		// Roslagstull utfart 49 127146
		chargingPointId2segId.put("242", "127146");

		// Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50 128187
		chargingPointId2segId.put("251", "128187");
		// Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51 128192
		chargingPointId2segId.put("252", "128192");
		// Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52 128204
		chargingPointId2segId.put("253", "128204");
		// Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53 128219
		chargingPointId2segId.put("254", "128219");

		// Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54 128215
		// FIXME tullId2segId.put("", "128215");

		// Ropsten Infart till Norra Hamnvägen 55 23370
		chargingPointId2segId.put("261", "23370");
		// Ropsten Utfart från Norra Hamnvägen 56 43117
		chargingPointId2segId.put("262", "43117");
		// Ropsten Infart mot Hjorthagen 57 125185
		chargingPointId2segId.put("263", "125185");
		// Ropsten Utfart från Hjorthagen 58 58297
		chargingPointId2segId.put("264", "58297");

		return chargingPointId2segId;
	}

	private static class PassageDataHandler extends AbstractTabularFileHandlerWithHeaderLine {

		private static final String PASSAGE_TIME = "passage_time";
		private static final String CHARGING_POINT = "charging_point";
		private static final String VEHICLE_TYPE = "vtr_vehicle_type"; // "PB" is "personbil"
		private static final String PROFESSIONAL_USE = "vtr_professional_use"; // not private
		private static final String PERSON_TYPE = "vtr_person_type"; // "F" is "fysisk person"

		private final Set<String> feasibleVehicleTypes = new LinkedHashSet<>(Arrays.asList("PB"));
		private final Set<String> feasibleProfessionalUses = new LinkedHashSet<>(Arrays.asList(""));
		private final Set<String> feasiblePersonTypes = new LinkedHashSet<>(Arrays.asList("F"));
		private final Map<String, String> chargingPoint2segment = chargingPoint2segment();

		private final DynamicData<Id<Link>> data;
		private final Set<Id<Link>> linkIds;

		private int invalidCnt = 0;
		private int validCnt = 0;

		private final Set<String> allDates = new LinkedHashSet<>();
		private final Set<String> allChargingPoints = new LinkedHashSet<>();
		private final Set<String> allVehicleTypes = new LinkedHashSet<>();
		private final Set<String> allProfessionalUses = new LinkedHashSet<>();
		private final Set<String> allPersonTypes = new LinkedHashSet<>();

		private PassageDataHandler(final DynamicData<Id<Link>> data, final Set<Id<Link>> linkIds) {
			this.data = data;
			this.linkIds = linkIds;
		}

		@Override
		public void startDataRow(String[] args) {

			final List<String> timeData = Arrays.asList(this.getStringValue(PASSAGE_TIME).split("[\\ \\.\\:]"));
			final int hours = Integer.parseInt(timeData.get(1));
			final int minutes = Integer.parseInt(timeData.get(2));
			final int seconds = Integer.parseInt(timeData.get(3));
			final double time_s = 3600.0 * hours + 60.0 * minutes + seconds;
			this.allDates.add(timeData.get(0));
			if ((time_s < 0) || (time_s >= Units.S_PER_D)) {
				this.invalidCnt++;
				return;
			}

			final String segmentStr = this.chargingPoint2segment.get(this.getStringValue(CHARGING_POINT));
			if (segmentStr == null) {
				this.invalidCnt++;
				return;
			}
			this.allChargingPoints.add(segmentStr);
			final Id<Link> segmentId = Id.createLinkId(segmentStr);
			if (!this.linkIds.contains(segmentId)) {
				this.invalidCnt++;
				return;
			}

			try {
				final String vehicleType = this.getStringValue(VEHICLE_TYPE);
				final String professionalUse = this.getStringValue(PROFESSIONAL_USE);
				final String personType = this.getStringValue(PERSON_TYPE);
				if (this.feasibleVehicleTypes.contains(vehicleType)
						&& this.feasibleProfessionalUses.contains(professionalUse)
						&& this.feasiblePersonTypes.contains(personType)) {
					this.allVehicleTypes.add(vehicleType);
					this.allProfessionalUses.add(professionalUse);
					this.allPersonTypes.add(personType);
					this.validCnt++;
				} else {
					this.invalidCnt++;
					return;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				this.invalidCnt++;
				return;
			}

			this.data.add(segmentId, this.data.bin((int) time_s), 1.0);
		}

		@Override
		public void endDocument() {
			System.out.println(this.allDates);
			System.out.println("charging points:   " + this.allChargingPoints);
			System.out.println("vehicle types:     " + this.allVehicleTypes);
			System.out.println("professional uses: " + this.allProfessionalUses);
			System.out.println("person types:      " + this.allPersonTypes);
			System.out.println("valid/invalid:     " + this.validCnt + "/" + this.invalidCnt);
			System.out.println();
		}

	}

	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.createConfig();
		config.network()
				.setInputFile("/Users/GunnarF/OneDrive - VTI/My Data/ihop2/ihop2-data/network-output/network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		System.out.println("nodes: " + network.getNodes().size());
		System.out.println("links: " + network.getLinks().size());
		System.out.println(network.getLinks().keySet());
		System.exit(0);
		
		
		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final DynamicData<Id<Link>> data = new DynamicData<>(timeDiscr);

		final List<String> days = Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18", "2016-10-19",
				"2016-10-20", "2016-10-25", "2016-10-26", "2016-10-27");

		double totalValid = 0;
		double totalInvalid = 0;

		for (String day : days) {
			for (String postfix : new String[] { "-01", "-02" }) {
				final Path path = Paths.get("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata",
						"wsp-passages-vtr-" + day + postfix + ".csv");
				System.out.println(path);

				final TabularFileParser parser = new TabularFileParser();
				parser.setDelimiterTags(new String[] { "," });
				parser.setOmitEmptyColumns(false);

				final PassageDataHandler handler = new PassageDataHandler(data, network.getLinks().keySet());
				parser.parse(path.toString(), handler);

				totalValid += handler.validCnt;
				totalInvalid += handler.invalidCnt;
			}
		}

		final double scale = (1.0 / days.size());

		List<Id<Link>> locations = new ArrayList<>(data.keySet());
		Collections.sort(locations, new Comparator<Id<Link>>() {
			@Override
			public int compare(Id<Link> o1, Id<Link> o2) {
				return new Integer(o1.toString()).compareTo(new Integer(o2.toString()));
			}
		});
		for (Id<Link> loc : locations) {
			System.out.print("@" + loc + "\t");
		}
		System.out.println();
		for (int bin = 0; bin < 24; bin++) {
			for (Id<Link> loc : locations) {
				System.out.print((scale * data.getBinValue(loc, bin)) + "\t");
			}
			System.out.println();
		}

		System.out.println("DONE");
	}
}
