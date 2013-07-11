/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.data.network.shortestpath;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.michalm.vrp.data.network.MatsimVertex;


public class ShortestPathCalculator
{
    private final LeastCostPathCalculator router;
    private final TravelTime travelTime;
    private final TravelDisutility travelDisutility;


    public ShortestPathCalculator(LeastCostPathCalculator router, TravelTime travelTime,
            TravelDisutility travelDisutility)
    {
        this.router = router;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
    }


    public ShortestPath calculateShortestPath(MatsimVertex fromVertex, MatsimVertex tovVertex,
            int departTime)
    {
        return calculateShortestPath(fromVertex.getLink(), tovVertex.getLink(), departTime);
    }


    public ShortestPath calculateShortestPath(Link fromLink, Link toLink, int departTime)
    {
        if (fromLink != toLink) {
            Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departTime, null, null);

            int count = path.links.size();
            Id[] ids = new Id[count + 1];
            int[] accLinkTravelTimes = new int[count + 1];
            int accTT = 0;

            for (int i = 0; i < count; i++) {
                Link link = path.links.get(i);
                ids[i] = link.getId();
                accTT += travelTime.getLinkTravelTime(link, departTime + accTT, null, null);
                accLinkTravelTimes[i] = accTT;
            }

            ids[count] = toLink.getId();
            accTT += travelTime.getLinkTravelTime(toLink, departTime + accTT, null, null);
            accLinkTravelTimes[count] = accTT;

            double cost = path.travelCost
                    + travelDisutility.getLinkTravelDisutility(toLink, departTime + accTT, null,
                            null);

            return new ShortestPath((int)accTT, cost, ids, accLinkTravelTimes);
        }
        else {
            return ShortestPath.ZERO_PATH_ENTRY;
        }
    }
}
