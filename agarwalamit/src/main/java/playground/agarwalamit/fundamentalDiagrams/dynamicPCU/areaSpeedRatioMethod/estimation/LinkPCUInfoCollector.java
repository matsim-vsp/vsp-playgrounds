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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.estimation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 29.06.17.
 *
 * update pcu at the end of track, eventually, this will also have effect on flow capacity
 *
 */


public class LinkPCUInfoCollector {

    private final Id<Link> linkId;
    private final double timeBin;

    private double pcu;

    LinkPCUInfoCollector (final Id<Link> linkId, final double timeBin) {
        this.linkId = linkId;
        this.timeBin = timeBin;
    }

    public double getPCU () {
        return this.pcu;
    }


}
