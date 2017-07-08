/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts.patna.allModes;

import java.util.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.opdyts.teleportationModes.Zone;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 15.06.17.
 */

public final class PatnaZoneToLinkIdentifier {

    private static final Logger LOGGER = Logger.getLogger(PatnaZoneToLinkIdentifier.class);

    //BEGIN_EXAMPLE
    public static void main(String[] args) {
        String zoneFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_allModes/Wards.shp";
        String networkFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_allModes/network.xml.gz";
        new PatnaZoneToLinkIdentifier(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork(), zoneFile);
    }
    //END_EXAMPLE

    private static final Set<Zone> zones = new LinkedHashSet<>();
    private final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG, TransformationFactory.WGS84);

    PatnaZoneToLinkIdentifier (final Population population, final String zoneFile) {
        ShapeFileReader reader = new ShapeFileReader();
        Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);

        Iterator<SimpleFeature> iterator = features.iterator();
        while (iterator.hasNext()){
            SimpleFeature feature = iterator.next();
            int id = (Integer) feature.getAttribute("ID1");
            Zone zone = new Zone(String.valueOf(id));

            for (Person p : population.getPersons().values()) {
                List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
                for (PlanElement pe : pes ) {
                    if (pe instanceof Activity) {
                        Coord coord = coordinateTransformation.transform( ((Activity)pe).getCoord() );
                        Point point = MGC.xy2Point(coord.getX(), coord.getY());
                        if ( ((Geometry) feature.getDefaultGeometry()).contains(point)) {
                            zone.addCoordsToZone(coord);
                        }
                    }
                }
            }

            if (zone.getCoordsInsideZone().isEmpty()) {
                LOGGER.warn("No coordinates found in the zone "+ zone.getZoneId());
            } else {
                LOGGER.info(zone.getCoordsInsideZone().size() + " coords are inside the zone "+ zone.getZoneId());
                zones.add(zone);
            }
        }
    }

    PatnaZoneToLinkIdentifier (final Network network, final String zoneFile) {
        ShapeFileReader reader = new ShapeFileReader();
        Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);

        Iterator<SimpleFeature> iterator = features.iterator();
        while (iterator.hasNext()){
            SimpleFeature feature = iterator.next();
            int id = (Integer) feature.getAttribute("ID1");
            Zone zone = new Zone(String.valueOf(id));

            for (Link l : network.getLinks().values()) {
                Coord coord = coordinateTransformation.transform(l.getCoord());
                Point point = MGC.xy2Point(coord.getX(), coord.getY());
                if ( ((Geometry) feature.getDefaultGeometry()).contains(point)) {
                    zone.addLinksToZone(l.getId());
                }
            }

            if (zone.getLinksInsideZone().isEmpty()) {
                LOGGER.warn("No link found in the zone "+ zone.getZoneId());
            } else {
                LOGGER.info(zone.getLinksInsideZone().size() + " links are inside the zone "+ zone.getZoneId());
                zones.add(zone);
            }
        }
    }

    public Set<Zone> getZones(){
        return this.zones;
    }
}
