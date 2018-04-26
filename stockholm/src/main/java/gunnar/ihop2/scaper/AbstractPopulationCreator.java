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

import static gunnar.ihop2.regent.demandreading.ShapeUtils.drawPointFromGeometry;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.xml.sax.helpers.DefaultHandler;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AbstractPopulationCreator extends DefaultHandler {

	// -------------------- MEMBERS --------------------

	// TODO encapsulate
	public final Scenario scenario;

	protected final ZonalSystem zonalSystem;

	// -------------------- CONSTRUCTION --------------------

	public AbstractPopulationCreator(final String networkFileName,
			final String zoneShapeFileName, final String zonalCoordinateSystem) {
		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(this.scenario.getNetwork())).readFile(networkFileName);
		this.zonalSystem = new ZonalSystem(zoneShapeFileName,
				zonalCoordinateSystem);
		this.zonalSystem.addNetwork(this.scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);
		Logger.getLogger(this.getClass().getName()).info(
				"number of zones in zonal system is "
						+ this.zonalSystem.getId2zoneView().size());
	}

	// public void setBuildingsFileName(final String buildingShapeFileName) {
	// this.zonalSystem.addBuildings(buildingShapeFileName);
	// }

	// public void removeExpandedLinks(final ObjectAttributes linkAttributes) {
	// final Set<String> tmLinkIds = new LinkedHashSet<String>(
	// ObjectAttributeUtils2.allObjectKeys(linkAttributes));
	// final Set<Id<Link>> removeTheseLinkIds = new LinkedHashSet<Id<Link>>();
	// for (Id<Link> candidateId : this.scenario.getNetwork().getLinks()
	// .keySet()) {
	// if (!tmLinkIds.contains(candidateId.toString())) {
	// removeTheseLinkIds.add(candidateId);
	// }
	// }
	// Logger.getLogger(this.getClass().getName()).info(
	// "Excluding " + removeTheseLinkIds.size()
	// + " expanded links from being activity locations.");
	// for (Id<Link> linkId : removeTheseLinkIds) {
	// this.scenario.getNetwork().removeLink(linkId);
	// }
	// }

	// -------------------- INTERNALS --------------------

	protected Coord drawHomeCoord(final String personId,
			final CoordinateTransformation coordinateTransform,
			final String homeZoneId, final String homeBuildingType) {
		final Zone homeZone = this.zonalSystem.getZone(homeZoneId);
		if (!this.zonalSystem.getNodes(homeZone).isEmpty()) {
			return coordinateTransform.transform(drawPointFromGeometry(homeZone
					.drawHomeGeometry(homeBuildingType)));
		}
		return null;
	}

	protected Coord drawWorkCoordinate(final String personId,
			final CoordinateTransformation coordinateTransform,
			final String workZoneId) {
		final Zone workZone = this.zonalSystem.getZone(workZoneId);
		if (!this.zonalSystem.getNodes(workZone).isEmpty()) {
			return coordinateTransform.transform(drawPointFromGeometry(workZone
					.drawWorkGeometry()));
		}
		return null;
	}

	protected Coord drawOtherCoordinate(final String personId,
			final CoordinateTransformation coordinateTransform,
			final String otherZoneId) {
		final Zone otherZone = this.zonalSystem.getZone(otherZoneId);
		if (!this.zonalSystem.getNodes(otherZone).isEmpty()) {
			return coordinateTransform
					.transform(drawPointFromGeometry(otherZone
							.drawWorkGeometry()));
		}
		return null;
	}

}
