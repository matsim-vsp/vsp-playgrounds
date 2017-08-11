/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.jbischoff.avparking.optimizer;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.zone.*;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;


public class PrivateAVTaxiDispatcher
    extends AbstractTaxiOptimizer
{
	private final LeastCostPathCalculator router;

   
	/**
	 * @param optimContext
	 * @param params
	 * @param unplannedRequests
	 * @param doUnscheduleAwaitingRequests
	 * @param doUpdateTimelines
	 */
	public PrivateAVTaxiDispatcher(TaxiOptimizerContext optimContext, AbstractTaxiOptimizerParams params) {
		super(optimContext, params, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), true, true);
		
		DijkstraFactory f = new DijkstraFactory();
		router = f.createPathCalculator(optimContext.network, getOptimContext().travelDisutility, getOptimContext().travelTime);
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer#scheduleUnplannedRequests()
	 */
	@Override
	protected void scheduleUnplannedRequests() {
        Iterator<TaxiRequest> reqIter = getUnplannedRequests().iterator();
        while (reqIter.hasNext()) {
            TaxiRequest req = reqIter.next();
            Id<Vehicle> personalAV = Id.create(req.getPassenger().getId().toString()+"_av",Vehicle.class);
            Vehicle veh = getOptimContext().fleet.getVehicles().get(personalAV);
            if (veh==null){
            	throw new RuntimeException("Vehicle "+personalAV.toString()+ "does not exist.");
            }
            if (!isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())){
            	throw new RuntimeException("Vehicle "+personalAV.toString()+ "is not idle.");

            }
            TaxiStayTask st = (TaxiStayTask) veh.getSchedule().getCurrentTask();
            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(st.getLink(), req.getFromLink(), getOptimContext().timer.getTimeOfDay(),router , getOptimContext().travelTime);
            getOptimContext().scheduler.scheduleRequest(veh, req, path);
            reqIter.remove();
        
            }
        }
	

    @Override
    protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask)
    {
        return isWaitStay(newCurrentTask);
    }


    protected boolean isWaitStay(TaxiTask task)
    {
        return task.getTaxiTaskType() == TaxiTaskType.STAY;
    }


}
