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

package playground.agarwalamit.connectedVehicles;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * A platoon consists of a leader and zero or more followers.
 *
 * Created by amit on 06.11.17.
 */

public class Platoon  {

    QVehicle leader ;

    Queue<QVehicle> followersQ = new FollowersQueue();

    /**
     * No platoon without a <code>leader</code>.
     * @param leader
     */
    Platoon (QVehicle leader) {
        this.leader = leader;
    }


    class FollowersQueue extends AbstractQueue<QVehicle> {

        Queue<QVehicle> followers = new LinkedList<>();

        @Override
        public Iterator<QVehicle> iterator() {
            return followers.iterator();
        }

        @Override
        public int size() {
            return followers.size();
        }

        @Override
        public boolean offer(QVehicle qVehicle) {
            // TODO here modify the flow and storage cap of leader
//            leader.getVehicle().getType().setFlowEfficiencyFactor();
//             qVehicle.getFlowCapacityConsumptionInEquivalents()
            return followers.offer(qVehicle);
        }

        @Override
        public QVehicle poll() {
            throw new RuntimeException("not implemented yet.");
        }

        @Override
        public QVehicle peek() {
           throw new RuntimeException("not implemented yet.");
        }
    }

}
