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
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.search.RandomParkingSearchLogic;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.google.common.collect.Iterables;

import playground.jbischoff.avparking.AvParkingContext;


public class PrivateAVTaxiDispatcher
    extends AbstractTaxiOptimizer
{
	private final LeastCostPathCalculator router;
	
	public enum AVParkBehavior {findfreeSlot, garage, cruise, randombehavior}
   
	private AVParkBehavior parkBehavior;
	private Random random = MatsimRandom.getRandom();
	private final ParkingSearchManager manager;
	private final ParkingSearchLogic parkingLogic;
	private final List<Link> avParkings;
	/**
	 * @param optimContext
	 * @param params
	 * @param avParkings 
	 * @param unplannedRequests
	 * @param doUnscheduleAwaitingRequests
	 * @param doUpdateTimelines
	 */
	public PrivateAVTaxiDispatcher(TaxiOptimizerContext optimContext, AbstractTaxiOptimizerParams params, ParkingSearchManager parkingManger, AvParkingContext context) {
		super(optimContext, params, new PriorityQueue<TaxiRequest>(100, Requests.T0_COMPARATOR), false, true);
		
		DijkstraFactory f = new DijkstraFactory();
		router = f.createPathCalculator(optimContext.network, getOptimContext().travelDisutility, getOptimContext().travelTime);
		parkBehavior = context.getBehavior();
		manager = parkingManger;
		parkingLogic = new RandomParkingSearchLogic(optimContext.network);
		this.avParkings =	NetworkUtils.getLinks(optimContext.network, context.getAvParkings());
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, 60)) {
			for (Vehicle veh : getOptimContext().fleet.getVehicles().values()) {
				if (veh.getSchedule().getStatus().equals(ScheduleStatus.STARTED)){

				if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())&& manager.getVehicleParkingLocation(Id.createVehicleId(veh.getId())) == null) {

				AVParkBehavior vehParkBehavior = parkBehavior;
				if (vehParkBehavior == AVParkBehavior.randombehavior) {
					int i = random.nextInt(3);
					if (i == 0)
						vehParkBehavior = AVParkBehavior.cruise;
					else if (i == 1)
						vehParkBehavior = AVParkBehavior.findfreeSlot;
					else if (i == 2)
						vehParkBehavior = AVParkBehavior.garage;
				}
				switch (vehParkBehavior) {
				case cruise:
//					Logger.getLogger(getClass()).info(veh.getId()+" --> cruise");
					sendVehicleToCruise(veh);
					break;
				case findfreeSlot:
//					Logger.getLogger(getClass()).info(veh.getId()+" --> parking");
					findFreeSlotAndParkVehicle(veh);
					break;
				case garage:
//					Logger.getLogger(getClass()).info(veh.getId()+" --> garage");
					sendVehicleToGarage(veh);
					break;
				case randombehavior:
					throw new IllegalStateException();
				}

			}

		}
			}
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	/**
	 * @param veh
	 */
	private void findFreeSlotAndParkVehicle(Vehicle veh) {
		    if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())){
		    	Link lastLink = Schedules.getLastLinkInSchedule(veh);
		    	//AV is not parked
		    	
		    		Id<org.matsim.vehicles.Vehicle> vehicleId = Id.createVehicleId(veh.getId());
		    		Id<Link> parkingLinkId = lastLink.getId();
		    		while (!this.manager.reserveSpaceIfVehicleCanParkHere(vehicleId, parkingLinkId)){
		    			parkingLinkId = parkingLogic.getNextLink(parkingLinkId,vehicleId);
		    		}
		    		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink,getOptimContext().network.getLinks().get(parkingLinkId) , getOptimContext().timer.getTimeOfDay(),router , getOptimContext().travelTime);

			    	((PrivateAVScheduler)getOptimContext().scheduler).moveIdleVehicle(veh, path);
		    		manager.parkVehicleHere(vehicleId, parkingLinkId, getOptimContext().timer.getTimeOfDay());
		    		
		    			    
		    	}

		
		
	}

	private void sendVehicleToGarage(Vehicle veh) {
				if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())) {
					Link lastLink = Schedules.getLastLinkInSchedule(veh);
					Link garageLink = findClosestAVParking(lastLink);
					if (!lastLink.getId().equals(garageLink)) {
						VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink,
								garageLink,
								getOptimContext().timer.getTimeOfDay(), router, getOptimContext().travelTime);

						((PrivateAVScheduler) getOptimContext().scheduler).moveIdleVehicle(veh, path);
						manager.parkVehicleHere(Id.createVehicleId(veh.getId()), garageLink.getId(), path.getArrivalTime());
					}
				}

			
		}
	

	/**
	 * @param lastLink
	 * @return
	 */
	private Link findClosestAVParking(Link lastLink) {
		double closestDistance = Double.MAX_VALUE;
		Link closestLink = null;
		for (Link p : avParkings){
			double distance = DistanceUtils.calculateSquaredDistance(lastLink.getCoord(), p.getCoord());
			if (distance<closestDistance){
				closestDistance = distance;
				closestLink = p;
			}
		}
		return closestLink;
	}

	private void sendVehicleToCruise(Vehicle veh) {
			if (isWaitStay((TaxiTask) veh.getSchedule().getCurrentTask())) {
					Link lastLink = Schedules.getLastLinkInSchedule(veh);
					Coord firstCircleCoord = new Coord(lastLink.getCoord().getX()-500-random.nextInt(1500),lastLink.getCoord().getY()-1000+random.nextInt(2000));
					Link firstCircleLink = NetworkUtils.getNearestLink(getOptimContext().network, firstCircleCoord);
					Coord secondCircleCoord = new Coord(lastLink.getCoord().getX()+500+random.nextInt(1500),lastLink.getCoord().getY()-1000+random.nextInt(2000));
					Link secondCircleLink = NetworkUtils.getNearestLink(getOptimContext().network, secondCircleCoord);
					double lastDepartureTime = getOptimContext().timer.getTimeOfDay();
					List<Double> linkTTs = new ArrayList<>();
					List<Link> links = new ArrayList<>();
					for (int i = 0; i < 1000; i++) {
						VrpPathWithTravelData outpath = VrpPaths.calcAndCreatePath(lastLink,
								firstCircleLink, lastDepartureTime,
								router, getOptimContext().travelTime);
						lastDepartureTime = outpath.getArrivalTime();
						for (int z = 0; z < outpath.getLinkCount() - 1; z++) {
							linkTTs.add(outpath.getLinkTravelTime(z));
							links.add(outpath.getLink(z));
						}

						VrpPathWithTravelData nextpath = VrpPaths.calcAndCreatePath(
								firstCircleLink,
								secondCircleLink, lastDepartureTime,
								router, getOptimContext().travelTime);

						for (int z = 0; z < nextpath.getLinkCount() - 1; z++) {
							linkTTs.add(nextpath.getLinkTravelTime(z));
							links.add(nextpath.getLink(z));
						}

						lastDepartureTime = nextpath.getArrivalTime();
						VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(
								secondCircleLink, lastLink,
								lastDepartureTime, router, getOptimContext().travelTime);
						for (int z = 0; z < lastPath.getLinkCount() - 1; z++) {
							linkTTs.add(lastPath.getLinkTravelTime(z));
							links.add(lastPath.getLink(z));
						}

						lastDepartureTime = lastPath.getArrivalTime();

					}
					double[] linkTTA = new double[linkTTs.size()];
					int i = 0;
					for (Double d : linkTTs) {
						linkTTA[i] = d;
						i++;
					}

					VrpPathWithTravelData path = new VrpPathWithTravelDataImpl(getOptimContext().timer.getTimeOfDay(),
							lastDepartureTime - getOptimContext().timer.getTimeOfDay(),
							links.toArray(new Link[links.size()]), linkTTA);
					((PrivateAVScheduler) getOptimContext().scheduler).moveIdleVehicle(veh, path);

				}

			
		
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
            Id<Link> parkLinkId = manager.getVehicleParkingLocation(Id.createVehicleId(personalAV));
            if (parkLinkId!=null){
            	manager.unParkVehicleHere(Id.createVehicleId(personalAV), parkLinkId, path.getDepartureTime());
            }
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
