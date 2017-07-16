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

import org.matsim.core.network.algorithms.CalcBoundingBox;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 15.07.17.
 */


public class BoundingBox {

    private final double xMin;
    private final double xMax;
    private final double yMin;
    private final double yMax;

    public double getxMin() {
        return xMin;
    }

    public double getxMax() {
        return xMax;
    }

    public double getyMin() {
        return yMin;
    }

    public double getyMax() {
        return yMax;
    }

    public BoundingBox(final double xMin, final double yMin, final double xMax, final double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public BoundingBox (final String networkFile) {
        CalcBoundingBox calcBoundingBox = new CalcBoundingBox();
        calcBoundingBox.run(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork());
        this.xMin = calcBoundingBox.getMinX();
        this.xMax = calcBoundingBox.getMaxX();
        this.yMin = calcBoundingBox.getMinY();
        this.yMax = calcBoundingBox.getMaxY();
    }
}
