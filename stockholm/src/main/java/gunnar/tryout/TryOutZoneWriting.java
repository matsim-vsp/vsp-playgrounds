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
package gunnar.tryout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import gunnar.ihop2.regent.demandreading.Zone;
import patryk.popgen2.PopulationParser;
import patryk.popgen2.SelectZones;

public class TryOutZoneWriting {

	public TryOutZoneWriting() {
		
		System.out.println("STARTED ...");

		final String networkFile = "./data/network/network_v12_utan_forbifart.xml";
		final String populationFile = "./data/synthetic_population/agentData.csv";
		final String zonesBoundaryShape = "./data/shapes/limit_EPSG3857.shp";

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		final ArrayList<String> coveredZones;
		{
			PopulationParser parser = new PopulationParser();
			parser.read(populationFile);

			final HashMap<String, Zone> zones = parser.getZones();
			final SelectZones selectZones = new SelectZones(zones,
					zonesBoundaryShape);
			coveredZones = selectZones.getZonesInsideBoundary();
		}

		System.out.println("number of zones = " + coveredZones.size()
				* coveredZones.size());

		Matrices matrices = new Matrices();
		final Matrix work = matrices.createMatrix("WORK",
				"random work tour travel times");
		final Matrix other = matrices.createMatrix("OTHER",
				"random other tour travel times");

		final double sampleFraction = 1.0;
		final List<String> sampleZoneIds = new ArrayList<String>();
		for (String zoneId : coveredZones) {
			if (Math.random() < sampleFraction) {
				sampleZoneIds.add(zoneId);
			}
		}

		int fromZoneCnt = 0;
		for (String fromZone : sampleZoneIds) {
			System.out.println((++fromZoneCnt) + " / " + coveredZones.size());
			for (String toZone : sampleZoneIds) {
				work.createAndAddEntry(fromZone, toZone, Math.random());
				other.createAndAddEntry(fromZone, toZone, Math.random());
			}
		}

		final MatricesWriter writer = new MatricesWriter(matrices);
		writer.setIndentationString("    ");
		writer.setPrettyPrint(true);
		writer.write("testmatrix" + sampleFraction + ".xml");

		System.out.println("... DONE");

		
	}
	
	
	public static void main(String[] args) {
		
		final TryOutZoneWriting test = new TryOutZoneWriting();
		
		
	}

}
