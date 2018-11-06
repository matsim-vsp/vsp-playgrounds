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

	/*-
	TOLL STATIONS:
	
	22626
	Danvikstull infart 1
	22626_AB  -->  22626_W
	
	110210
	Danvikstull utfart 2
	110210_AB  -->  110210_E
	
	6114
	Skansbron infart 3
	Skansbron utfart 4
	6114_AB  -->  6114_S
	6114_BA  -->  6114_N
	
	80449
	Skanstullbron infart 5
	80449_AB  -->  80449_N
	
	28480
	Skanstullbron utfart 6
	28480_AB  -->  28480_S
	
	9216
	Johanneshovbron infart 7
	9216_AB  -->  9216_N
	
	18170
	Johanneshovbron utfart 8
	18170_AB  -->  18170_SE
	
	25292
	Liljeholmsbron infart 9
	25292_AB  -->  25292_NE
	
	2453
	Liljeholmsbron utfart 10
	2453_AB  -->  2453_SW
	
	74188
	Stora Essingen infart 11
	74188_AB  -->  74188_NW
	
	53064
	Stora Essingen utfart 12
	53064_AB  -->  53064_SE
	
	51930
	Stora Essingen infart 13
	51930_AB  -->  51930_W
	
	44566
	Lilla Essingen utfart 14
	44566_AB  -->  44566_NE
	
	92866
	Drottningholmsvägen infart från Bromma 15
	92866_AB  -->  92866_E
	
	77626
	Drottningholmsvägen utfart mot Bromma 16
	77626_AB  -->  77626_W
	
	127169
	Drottningholmsvägen infart från EL 17
	
	124799
	Drottningholmsvägen utfart mot EL 18
	124799_AB  -->  124791_W
	
	119586
	Essingeleden södergående från Bromma 19
	119586_AB  -->  119586_S
	
	120732
	Essingleden norrgående mot Bromma 20
	120732_AB  -->  120732_W
	
	74191
	Essingeleden södergående 21
	74191_AB  -->  74191_S
	
	52300
	Essingeleden norrgående 22
	52300_AB  -->  52300_N
	
	95249
	Kristineberg Avfart Essingeleden S 23
	95249_AB  -->  REMOVED: 95249_NE
	
	52301
	Kristineberg Påfart Essingeleden N 24
	52301_AB  -->  52301_N
	
	128236
	Kristineberg Avfart Essingeleden N 25
	
	128234
	Kristineberg Påfart Essingeleden S 26
	
	122017
	Klarastrandsleden infart 27
	Klarastrandsleden utfart 28
	122017_AB  -->  122017_NW
	122017_BA  -->  122017_SE
	
	121908
	Ekelundsbron infart 29
	Ekelundsbron utfart 30
	121908_AB  -->  121908_N
	121908_BA  -->  121908_S
	
	52416
	Tomtebodavägen infart NY 31
	Tomtebodavägen utfart NY 32
	52416_AB  -->  REMOVED: 52416_SW
	52416_BA  -->  52416_NE
	
	108353
	Solnavägen infart 33
	108353_AB  -->  108353_SE
	
	113763
	Solnavägen utfart 34
	113763_AB  -->  113763_NW
	
	101350
	Norrtull Sveavägen utfart 36
	101350_AB  -->  101350_NW
	
	128229
	Norrtull tillfällig väg 38
	
	34743
	Norra stationsgatan utfart 39
	34743_AB  -->  34743_NE
	
	54215
	Ekhagen Avfart E18S 40
	54215_AB  -->  54215_S
	
	116809
	Ekhagen Påfart E18N 41
	116809_AB  -->  116809_NW
	
	74955
	Ekhagen Avfart E18N 42
	74955_AB  -->  74955_N
	
	35466
	Ekhagen Påfart E18S 43
	35466_AB  -->  35466_SE
	
	56348
	Frescati infart 44
	Frescati utfart 45
	56348_AB  -->  REMOVED: 56348_W
	56348_BA  -->  REMOVED: 56348_E
	
	127555
	Universitetet infart 46
	
	42557
	Universitetet utfart 47
	42557_AB  -->  42557_N
	
	129132
	Roslagstull infart 48
	
	127146
	Roslagstull utfart 49
	
	128187
	Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50
	
	128192
	Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51
	
	128204
	Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52
	
	128219
	Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53
	
	128215
	Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54
	
	23370
	Ropsten Infart till Norra Hamnvägen 55
	23370_AB  -->  23370_NE
	
	43117
	Ropsten Utfart från Norra Hamnvägen 56
	43117_AB  -->  43117_N
	
	125185
	Ropsten Infart mot Hjorthagen 57
	125185_AB  -->  125167_N
	
	58297
	Ropsten Utfart från Hjorthagen 58
	58297_AB  -->  58297_SE

	*/

	public static Map<String, String> chargingPoint2link() {
		final Map<String, String> chargingPoint2link = new LinkedHashMap<>();

		// Tull_ID SegID

		// Danvikstull infart 1 22626
		chargingPoint2link.put("11", "22626_W");

		// Danvikstull utfart 2 110210
		chargingPoint2link.put("12", "110210_E");

		// Skansbron infart 3 6114
		chargingPoint2link.put("21", "6114_N");

		// Skansbron utfart 4 6114
		chargingPoint2link.put("22", "6114_S");

		// Skanstullbron infart 5 80449
		chargingPoint2link.put("31", "80449_N");

		// Skanstullbron utfart 6 28480
		chargingPoint2link.put("32", "28480_S");

		// Johanneshovbron infart 7 9216
		chargingPoint2link.put("41", "9216_N");

		// Johanneshovbron utfart 8 18170
		chargingPoint2link.put("42", "18170_SE");

		// Liljeholmsbron infart 9 25292
		chargingPoint2link.put("51", "25292_NE");

		// Liljeholmsbron utfart 10 2453
		chargingPoint2link.put("52", "2453_SW");

		// Stora Essingen infart 11 74188
		chargingPoint2link.put("61", "74188_NW");

		// Stora Essingen utfart 12 53064
		chargingPoint2link.put("62", "53064_SE");

		// Stora Essingen infart 13 51930
		chargingPoint2link.put("63", "51930_W");

		// Lilla Essingen utfart 14 44566
		chargingPoint2link.put("72", "44566_NE");

		// Drottningholmsvägen infart från Bromma 15 92866
		chargingPoint2link.put("81", "92866_E");

		// Drottningholmsvägen utfart mot Bromma 16 77626
		chargingPoint2link.put("82", "77626_W");

		// Drottningholmsvägen infart från EL 17 127169
		// [not encoded]

		// Drottningholmsvägen utfart mot EL 18 124799
		chargingPoint2link.put("84", "124791_W");

		// Essingeleden södergående från Bromma 19 119586
		chargingPoint2link.put("85", "119586_S");

		// Essingleden norrgående mot Bromma 20 120732
		chargingPoint2link.put("86", "120732_W");

		// Essingeleden södergående 21 74191
		chargingPoint2link.put("87", "74191_S");

		// Essingeleden norrgående 22 52300
		chargingPoint2link.put("88", "52300_N");

		// Kristineberg Avfart Essingeleden S 23 95249
		// [removed when cleaning network]

		// Kristineberg Påfart Essingeleden N 24 52301
		chargingPoint2link.put("94", "52301_N");

		// Kristineberg Avfart Essingeleden N 25 128236
		// [not encoded]

		// Kristineberg Påfart Essingeleden S 26 128234
		// [not encoded]

		// Klarastrandsleden infart 27 122017
		chargingPoint2link.put("101", "122017_SE");

		// Klarastrandsleden utfart 28 122017
		chargingPoint2link.put("102", "122017_NW");

		// Ekelundsbron infart 29 121908
		chargingPoint2link.put("111", "121908_S");

		// Ekelundsbron utfart 30 121908
		chargingPoint2link.put("112", "121908_N");

		// Tomtebodavägen infart NY 31 52416
		// [problematic, unclear which one cleaned away]
		
		// Tomtebodavägen utfart NY 32 52416
		// [problematic, unclear which one cleaned away]

		// Solnavägen infart 33 108353
		chargingPoint2link.put("131", "108353_SE");

		// Solnavägen utfart 34 113763
		chargingPoint2link.put("132", "113763_NW");

		// Norrtull Sveavägen utfart 36 101350
		chargingPoint2link.put("146", "101350_NW");

		// Norrtull tillfällig väg 38 128229
		// [not encoded]

		// Norra stationsgatan utfart 39 34743
		chargingPoint2link.put("148", "34743_NE");

		// Ekhagen Avfart E18S 40 54215
		chargingPoint2link.put("211", "54215_S");

		// Ekhagen Påfart E18N 41 116809
		chargingPoint2link.put("212", "116809_NW");

		// Ekhagen Avfart E18N 42 74955
		chargingPoint2link.put("213", "74955_N");

		// Ekhagen Påfart E18S 43 35466
		chargingPoint2link.put("214", "35466_SE");

		// Frescati infart 44 56348
		// [removed when cleaning network]

		// Frescati utfart 45 56348
		// [removed when cleaning network]

		// Universitetet infart 46 127555
		// [not encoded]

		// Universitetet utfart 47 42557
		chargingPoint2link.put("232", "42557_N");

		// Roslagstull infart 48 129132
		// [not encoded]

		// Roslagstull utfart 49 127146
		// [not encoded]

		// Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50 128187
		// [not encoded]

		// Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51 128192
		// [not encoded]

		// Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52 128204
		// [not encoded]

		// Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53 128219
		// [not encoded]

		// Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54 128215
		// [not encoded]

		// Ropsten Infart till Norra Hamnvägen 55 23370
		chargingPoint2link.put("261", "23370_NE");

		// Ropsten Utfart från Norra Hamnvägen 56 43117
		chargingPoint2link.put("262", "43117_N");

		// Ropsten Infart mot Hjorthagen 57 125185
		chargingPoint2link.put("263", "125167_N");

		// Ropsten Utfart från Hjorthagen 58 58297
		chargingPoint2link.put("264", "58297_SE");

		return chargingPoint2link;
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
		private final Map<String, String> chargingPoint2link = chargingPoint2link();

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

			final String segmentStr = this.chargingPoint2link.get(this.getStringValue(CHARGING_POINT));
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
		for (Id<Link> loc : locations) {
			System.out.print(loc + "\t");
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
