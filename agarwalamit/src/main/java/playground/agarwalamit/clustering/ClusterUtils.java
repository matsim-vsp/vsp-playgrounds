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

package playground.agarwalamit.clustering;

import java.util.Random;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Created by amit on 15.07.17.
 */

public class ClusterUtils {

    public static Point getRandomPoint (final BoundingBox boundingBox) {
        Random random = new Random();
        double x = boundingBox.getxMin() + random.nextDouble() * (boundingBox.getxMax() - boundingBox.getxMin());
        double y = boundingBox.getyMin() + random.nextDouble() * (boundingBox.getyMax() - boundingBox.getyMin());
        return  new Point(x, y);
    }

    public static double euclideanDistance (final Point site1, final Point site2) {
        return CoordUtils.calcEuclideanDistance( new Coord( site1.getX(), site1.getY() ),
                new Coord( site2.getX(), site2.getY()));
    }

}
