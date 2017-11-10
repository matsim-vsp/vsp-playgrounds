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
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;

/**
 * A traffic stream may have conventional as well as autonomous vehicles. The idea is to let the vehicles connect with each other and travel as a platoon on the link.
 * For this, vehicle must have some identifier to differentiate between conventional and autonomous vehicles. The salient features are:
 * <ul>
 *     <li> Platooning should be allowed only on motorways and major arterials.</li>
 *     <li> *Done* There will be exactly one LEADER of the platoon. </li>
 *     <li> *Done* There can be multiple platoons on a link and thus multiple leaders on the link. </li>
 *     <li> *Partially done* There should be a threshold for number of vehicles in a platoon. </li>
 *     <li> There should be a threshold of time so that, all vehicles within this time will make a platoon. The last vehicle of the time-bin will speed up (accelerate) or other will slowdown to join the platoon.</li>
 *     <li> The algorithm for connected vehicles must be applied to buffer to exclude the agents arriving on the link. </li>
 *     <li> The questions are:
 *         <ul>
 *              <li> can a platoon with 'n' vehicles leave at the same time? What would be the combined flow and storage capacity consumption? </li>
 *              <li> Link leaving time of the platoon will depend on the first/last vehicle? </li>
 *              <li> Should we use a mix of 'withHoles' and 'withoutHoles' traffic dynamics? </li>
 *              <li> Should we cluster vehicles with same OD so that each cluster can make a platoon? </li>
 *         </ul>
 *     </li>
 * </ul>
 * Created by amit on 06.11.17.
 */

public class ConnectedVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {

    private Queue<Platoon> platoons = new LinkedList<>();
    /**
     * Using passing queue, so that a platoon can overtake slower vehicles as well as
     * conventional vehicle can overtake platoon. However, it is somewhat unrealistic that a vehicle
     * overtake whole platoon in one go.
     */
    private Queue<QVehicle> leaderAndRestVehicles = new PassingVehicleQ();

    private int maxSizeOfPlatoon = 5; //TODO : replace magic number by configurable parameter.

    @Override
    public Iterator<QVehicle> iterator() {
        throw new RuntimeException("not implemented yet.");
    }

    @Override
    public int size() {
        return  (int) platoons.parallelStream().mapToInt(e->e.followersQ.size()).sum() + leaderAndRestVehicles.size();
    }

    @Override
    public void addFirst(QVehicle previous) {
        throw new RuntimeException("not implemented yet.");
    }

    @Override
    public boolean offer(QVehicle qVehicle) {
        if (qVehicle.getVehicle().getType().getDescription().equals("autonomous")) {

            Iterator<Platoon> it = platoons.iterator();
            while(it.hasNext()) {
                Platoon platoon = it.next();
                if ( platoon.followersQ.size() < maxSizeOfPlatoon ) {
                    return platoon.followersQ.offer(qVehicle);
                }
            }
            // create a new platoon if no space is available in existing platoons or no platoons
            Platoon platoon = new Platoon(qVehicle);
            platoons.offer(platoon);
            // also add leader to the queue from which vehicles will be removed.
            return leaderAndRestVehicles.add(qVehicle);
        } else {
            return leaderAndRestVehicles.add(qVehicle);
        }
    }

    @Override
    public QVehicle poll() {
        QVehicle vehicle = leaderAndRestVehicles.poll();
        platoons.remove(vehicle); // this will also remove all followers.
        return vehicle;
    }

    @Override
    public QVehicle peek() {
        return leaderAndRestVehicles.peek();
    }
}

