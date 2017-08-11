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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.google.common.collect.Iterables;


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
		super(optimContext, params, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), false, true);
		
		DijkstraFactory f = new DijkstraFactory();
		router = f.createPathCalculator(optimContext.network, getOptimContext().travelDisutility, getOptimContext().travelTime);
		
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, 60))
		{
			//send them to parking
//			for (Vehicle veh : getOptimContext().fleet.getVehicles().values()){
//				if (veh.getSchedule().getStatus().equals(ScheduleStatus.STARTED)){
//	            if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())){
//	            	Link lastLink = Schedules.getLastLinkInSchedule(veh);
//	            	if (!lastLink.getId().equals(Id.createLinkId(133))){
//	                    VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink,getOptimContext().network.getLinks().get(Id.createLinkId(133)) , getOptimContext().timer.getTimeOfDay(),router , getOptimContext().travelTime);
//
//	            	((PrivateAVScheduler)getOptimContext().scheduler).moveIdleVehicle(veh, path);	
//	            	}
//	            }
//
//			}
//		}
			
			//let them cruise until the end of time
			for (Vehicle veh : getOptimContext().fleet.getVehicles().values()){
			if (veh.getSchedule().getStatus().equals(ScheduleStatus.STARTED)){
            if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())){
            	Link lastLink = Schedules.getLastLinkInSchedule(veh);
            	double lastDepartureTime = getOptimContext().timer.getTimeOfDay();
            	List<Double> linkTTs = new ArrayList<>();
            	List<Link> links = new ArrayList<>();
            	for (int i = 0; i<1000; i++){
                 VrpPathWithTravelData outpath = VrpPaths.calcAndCreatePath(lastLink,getOptimContext().network.getLinks().get(Id.createLinkId(133)) ,lastDepartureTime,router , getOptimContext().travelTime);
                 lastDepartureTime = outpath.getArrivalTime(); 
                 for (int z=0;z<outpath.getLinkCount()-1;z++){
                	 linkTTs.add(outpath.getLinkTravelTime(z));
                	 links.add(outpath.getLink(z));
                 }

                 VrpPathWithTravelData nextpath = VrpPaths.calcAndCreatePath(getOptimContext().network.getLinks().get(Id.createLinkId(133)), getOptimContext().network.getLinks().get(Id.createLinkId(151)), lastDepartureTime,router , getOptimContext().travelTime);
                 
                 for (int z=0;z<nextpath.getLinkCount()-1;z++){
                	 linkTTs.add(nextpath.getLinkTravelTime(z));
                	 links.add(nextpath.getLink(z));
                 }

                 lastDepartureTime = nextpath.getArrivalTime();
                 VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(getOptimContext().network.getLinks().get(Id.createLinkId(151)), lastLink, lastDepartureTime,router , getOptimContext().travelTime);
                 for (int z=0;z<lastPath.getLinkCount()-1;z++){
                	 linkTTs.add(lastPath.getLinkTravelTime(z));
                	 links.add(lastPath.getLink(z));
                 }

                 lastDepartureTime = lastPath.getArrivalTime();

            	}
            	double[] linkTTA = new double[linkTTs.size()];
            	int i = 0;
            	for (Double d : linkTTs){
            		linkTTA[i] = d;
            		i++;
            	}

                VrpPathWithTravelData path = new VrpPathWithTravelDataImpl(getOptimContext().timer.getTimeOfDay(), lastDepartureTime-getOptimContext().timer.getTimeOfDay(), links.toArray(new Link[links.size()]),linkTTA); 
            	((PrivateAVScheduler)getOptimContext().scheduler).moveIdleVehicle(veh, path);	
            	
            }

		}
	}

		}

		super.notifyMobsimBeforeSimStep(e);
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
            if (!isWaitStayOrEmptyDrive((TaxiTask) veh.getSchedule().getCurrentTask())){
            	throw new RuntimeException("Vehicle "+personalAV.toString()+ "is not idle.");

            }
            if ( ((TaxiTask)veh.getSchedule().getCurrentTask()).getTaxiTaskType()==TaxiTaskType.EMPTY_DRIVE){
            	((PrivateAVScheduler)getOptimContext().scheduler).stopCruisingVehicle(veh);;
            }
            
            VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(Schedules.getLastLinkInSchedule(veh), req.getFromLink(), getOptimContext().timer.getTimeOfDay(),router , getOptimContext().travelTime);
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
    protected boolean isWaitStayOrEmptyDrive(TaxiTask task)
    {
    	if ((task.getTaxiTaskType()== TaxiTaskType.STAY)|(task.getTaxiTaskType()== TaxiTaskType.EMPTY_DRIVE)) 
        return true;
    	else return false;
    }


}
