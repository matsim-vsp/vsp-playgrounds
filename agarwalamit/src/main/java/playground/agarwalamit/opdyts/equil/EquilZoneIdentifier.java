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

package playground.agarwalamit.opdyts.equil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import playground.agarwalamit.clustering.BoundingBox;
import playground.agarwalamit.clustering.Cluster;
import playground.agarwalamit.clustering.ClusterAlgorithm;
import playground.agarwalamit.clustering.ClusterUtils;
import playground.agarwalamit.clustering.Point;
import playground.agarwalamit.opdyts.teleportationModes.Zone;

/**
 * Created by amit on 22.10.17.
 */

public class EquilZoneIdentifier {

    private static final Set<Zone> zones = new LinkedHashSet<>();

    EquilZoneIdentifier(final Population population, final BoundingBox boundingBox, final int numberOfClusters, final ClusterAlgorithm.ClusterType clusterType) {
        List<Point> listOfOrigins = new ArrayList<>();
        for (Person person : population.getPersons().values()) {
            List<PlanElement> planElementList = person.getSelectedPlan().getPlanElements();
            List<playground.agarwalamit.clustering.Point> list = planElementList.stream().filter(pe -> pe instanceof Activity).map(pe ->
                    {
                        Coord cord = ((Activity)pe).getCoord();
                        return ClusterUtils.getPoint(cord);
                    }
            ).collect(Collectors.toList());
            listOfOrigins.addAll(list);
        }

        ClusterAlgorithm clusterAlgorithm = new ClusterAlgorithm(numberOfClusters, boundingBox, clusterType);
        clusterAlgorithm.process(listOfOrigins);
        List<Cluster> clusters = clusterAlgorithm.getClusters();
        for (Cluster cluster : clusters) {
            Zone zone = new Zone(cluster.getId().toString());
            cluster.getPoints().stream().forEach(
                    p -> zone.addCoordsToZone( ClusterUtils.getCoord(p))
            );
            zones.add(zone);
        }
    }

    public Set<Zone> getZones(){
        return this.zones;
    }
}
