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
package gunnar.ihop2.scaper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScaperToMatsimDictionary {
	public static Map<String, String> scaper2matsim;

	static {
		final Map<String, String> scaper2matsimTmp = new LinkedHashMap<>();

		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_BICYCLE, "bike");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_BUS, "bus");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_TRAM, "tram");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_TRAIN, "train");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_PT, "pt");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_RAIL, "rail");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_WALK, "walk");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_CAR, "car");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_SUBWAY, "subway");

		scaper2matsimTmp.put(ScaperPopulationCreator.HOME_ACTIVITY, "home");
		scaper2matsimTmp
				.put(ScaperPopulationCreator.SHOPPING_ACTIVITY, "other"); // "shopping");
		scaper2matsimTmp.put(ScaperPopulationCreator.LEISURE_ACTIVITY, "other"); // "leisure");
		scaper2matsimTmp.put(ScaperPopulationCreator.OTHER_ACTIVITY, "other");
		scaper2matsimTmp.put(ScaperPopulationCreator.WORK_ACTIVITY, "work");
		scaper2matsim = Collections.unmodifiableMap(scaper2matsimTmp);
	}

	private ScaperToMatsimDictionary() {
	}
}
