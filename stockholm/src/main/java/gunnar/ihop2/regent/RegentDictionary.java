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
package gunnar.ihop2.regent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import gunnar.ihop2.regent.demandreading.RegentPopulationReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RegentDictionary {

	public static final Map<String, String> regent2matsim;
	public static final Map<String, String> matsim2regent;

	static {
		final Map<String, String> regent2matsimTmp = new LinkedHashMap<>();
		final Map<String, String> matsim2regentTmp = new LinkedHashMap<>();

		regent2matsimTmp.put(RegentPopulationReader.CAR_ATTRIBUTEVALUE, "car");
		matsim2regentTmp.put("car", RegentPopulationReader.CAR_ATTRIBUTEVALUE);

		regent2matsimTmp.put(RegentPopulationReader.PT_ATTRIBUTEVALUE, "pt");
		matsim2regentTmp.put("pt", RegentPopulationReader.PT_ATTRIBUTEVALUE);

		regent2matsim = Collections.unmodifiableMap(regent2matsimTmp);
		matsim2regent = Collections.unmodifiableMap(matsim2regentTmp);
	}

	private RegentDictionary() {
	}

}
