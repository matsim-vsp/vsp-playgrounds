/* *********************************************************************** *
 * project: org.matsim.*
 * DgRoederGershensonController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package signals.gershenson;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;
import signals.sensor.LinkSensorManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


/**
 * Implement Gershenson-Controller as outlined in "Self-Organizing Traffic Lights at
 * Multiple-Street Intersections" (Gershenson/Rosenblueth 2011). 
 * NULL-State: All Groups are RED if nobody approaches the SignalSystem. 
 * 
 * 
 * default values:
 * interGreenTime=0
 * (monitoredDistance) d=50m   
 * (monitoredPlatoonTail) r=25m 
 * (minimumGREENtime) t_min = 4s 
 * (lengthOfPlatoonTails) m= 2 veh 
 * (threshold) n= 13.33 veh*s   (sbraun Feb.18)
 * 
 * @author dgrether, droeder, sbraun
 *
 */
public class GershensonSignalController implements SignalController {

	
	public final static String IDENTIFIER = "GershensonSignalController";
	
	
	
	public final static class SignalControlProvider implements Provider<SignalController> {
		private final LinkSensorManager sensorManager;
		private final Scenario scenario;
		private final GershensonConfig gershensonConfig;
		
		public SignalControlProvider(LinkSensorManager sensorManager, Scenario scenario, GershensonConfig gershensonConfig) {
			this.sensorManager = sensorManager;
			this.scenario = scenario;
			this.gershensonConfig = gershensonConfig;
		}

		@Override
		public SignalController get() {
			return new GershensonSignalController(scenario, sensorManager, gershensonConfig);
		}
	}
	
	private class SignalGroupMetadata {

		private Set<Link> inLinks;
		private Set<Link> outLinks;
		private Set<Lane> inLanes;
		private HashMap<Id<Lane>,Id<Link>> inLanesOnLink;
		private HashMap<Id<Lane>,Double> inLanesLengthExceptions;
		private HashMap<Id<Link>,Double> inLinkLengthExceptions;
		
		
		public SignalGroupMetadata(){
			this.inLinks = new HashSet<Link>();
			this.outLinks = new HashSet<Link>();
			this.inLanes = new HashSet<Lane>();
			this.inLanesOnLink = new HashMap<Id<Lane>,Id<Link>>();
			this.inLanesLengthExceptions  = new HashMap<Id<Lane>,Double>();
			this.inLinkLengthExceptions = new HashMap<Id<Link>,Double>();
		}
		
		
		public void addInLink(Link link) {
			this.inLinks.add(link);
		}

		public void addOutLink(Link outLink) {
			this.outLinks.add(outLink);
		}
		
		public Set<Link> getOutLinks(){
			return this.outLinks;
		}
		
		
		public Set<Link> getInLinks(){
			return this.inLinks;
		}
		
		public void addInLanes(Lane lane) {
			this.inLanes.add(lane);
		}

		public Set<Lane> getInLanes(){
			return this.inLanes;
		}
		
		public void addLaneToLinkAssignment(Id<Lane> lane, Id<Link> link){
			this.inLanesOnLink.put(lane, link);
		}
		
		public Id<Link> getLinkOfLane(Id<Lane> lane){
			return this.inLanesOnLink.get(lane);
		}
		
		public HashMap<Id<Lane>,Double> getInLanesLengthExceptions(){
			return this.inLanesLengthExceptions;
		}
		
		public void setInLanesLengthExceptions(Id<Lane> lane,Double length){
			this.inLanesLengthExceptions.put(lane, length);
		}
		
		public HashMap<Id<Link>,Double> getInLinkLengthExceptions(){
			return this.inLinkLengthExceptions;
		}
		
		public void setInLinkLengthExceptions(Id<Link> link,Double length){
			this.inLinkLengthExceptions.put(link, length);
		}
	}

	private static final Logger log = Logger.getLogger(GershensonSignalController.class);
	
	private SignalSystem system;
	private LinkSensorManager sensorManager;
	private Scenario scenario;
	private GershensonConfig gershensonConfig;
	
	private Map<Id<SignalGroup>, SignalGroupMetadata> signalGroupIdMetadataMap;	
	
	
	
	
	private GershensonSignalController(Scenario scenario, LinkSensorManager sensorManager, GershensonConfig gershensonConfig) {
		this.scenario = scenario;
		this.sensorManager = sensorManager;
		this.gershensonConfig = gershensonConfig;
	}

	private void registerAndInitializeSensorManager() {
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(signalGroup.getId());
			for(Signal signal : signalGroup.getSignals().values()){
				
				maximalNumberOfAgentsInDistanceMap.put(signal.getId(), (int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize()));
				//TODO if signal has only one lane register the lane and not the link otherwise this is wrong
				//Like this sensor will mix up signal groups.
				
				
				if((signal.getLaneIds()==null)|| signal.getLaneIds().isEmpty() ||scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()<=1. ){
					//this.sensorManager.registerNumberOfCarsMonitoring(signal.getLinkId());
					if (!(signal.getLaneIds()==null|| signal.getLaneIds().isEmpty()) && scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()==1. ){
						log.info("Register InLane "+ signal.getLaneIds().iterator().next().toString() +" of Signal "+ signal.getId().toString()+". Since there is only one Lane for this signal register the Link");
					} else {
						log.info("Register Inlink "+ signal.getLinkId().toString() +" of Signal "+ signal.getId().toString());
					}

					
	
//					Rule 3: "If a few vehicles (m or fewer, but more than zero) are left to cross a green light at a short distance r , do not switch the light."
//					But if m*vehicle length is more than the distance between the monitored distance this might not work properly
					
					double lenghtOfLink = scenario.getNetwork().getLinks().get(signal.getLinkId()).getLength();
					
					
					
					if (lenghtOfLink<gershensonConfig.getMonitoredDistance()){
						
						int adjustedNumberOfAgents = java.lang.Math.max((int)(lenghtOfLink/this.scenario.getConfig().jdeqSim().getCarSize()),1);
						maximalNumberOfAgentsInDistanceMap.put(signal.getId(), adjustedNumberOfAgents);
						
						if (lenghtOfLink<gershensonConfig.getMonitoredPlatoonTail()){
							log.warn("The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredPlatoonTail()+" m. Register one Sensor at"+ lenghtOfLink+
								" The sensor for Rule 3 is set 0´. This means Rule 3 is not going to execute. This might be not fatal but annoying.");
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
						} else if (lenghtOfLink-gershensonConfig.getMonitoredPlatoonTail()<gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
							log.warn("The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. Moreover the the length of this lane is too short to execute Rule 3 correctly (The monitored distance between the two sensors would be less than two cars-sizes). "
									+ "Register one Sensor at"+ lenghtOfLink+
									" The sensor for Rule 3 is set 0´. ");
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
						} else {
							log.warn("The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. This is may not be fatal."
									+ "Two sensors are registered: First at "+lenghtOfLink+" m. Secondly at "+gershensonConfig.getMonitoredPlatoonTail()+ " m.");							
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail());
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
						}
					} else {
						this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail());
						this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredDistance());
					}
				}else{
					LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId());
					for(Id<Lane> laneId : signal.getLaneIds()){
						log.info("Register Sensor for InLane "+ laneId.toString() +" of Signal "+ signal.getId().toString());
						double lenghtOfLane = lanes4link.getLanes().get(laneId).getStartsAtMeterFromLinkEnd();
						if (lenghtOfLane<gershensonConfig.getMonitoredDistance()){
							
							int adjustedNumberOfAgents = java.lang.Math.max((int)(lenghtOfLane/this.scenario.getConfig().jdeqSim().getCarSize()),1);
							maximalNumberOfAgentsInDistanceMap.put(signal.getId(), adjustedNumberOfAgents);
							//TODO MAP Signal-Exceptions in metadata to adjust for the rules
							if (lenghtOfLane<gershensonConfig.getMonitoredPlatoonTail()){
								log.warn("The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredPlatoonTail()+" m. Register one Sensor at"+ lenghtOfLane+
										" The sensor for Rule 3 is set 0´. This means Rule 3 is not going to execute. This might be not fatal but annoying.");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
							} else if (lenghtOfLane-gershensonConfig.getMonitoredPlatoonTail()<2*this.scenario.getConfig().jdeqSim().getCarSize()){
								log.warn("The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. Moreover the the length of this lane is too short to execute Rule 3 correctly (The monitored distance between the two sensors would be less than two cars-sizes). "
										+ "Register one Sensor at"+ lenghtOfLane+
										" The sensor for Rule 3 is set 0´. ");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
							} else {
								log.warn("The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. This is may not be fatal and is irgonered."
										+ "Two sensors are registered: First at "+lenghtOfLane+" m. Secondly at "+gershensonConfig.getMonitoredPlatoonTail()+ " m.");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredPlatoonTail());
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
							}
						} else {
							this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredPlatoonTail());
							this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredDistance());
							
						}
					}
				}
			}
			for (Link outLink : metadata.getOutLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(outLink.getId());
				log.info("Register Outlink "+ outLink.getId().toString());
			}
		}
	}	
		
//		for (SignalGroupMetadata metadata : this.signalGroupIdMetadataMap.values()){
//			// initialize sensors per signal group: one at the out-link, two sensors at the in-link with different distances
//			for (Link outLink : metadata.getOutLinks()){
//				this.sensorManager.registerNumberOfCarsMonitoring(outLink.getId());
//				log.info("Register Outlink "+ outLink.getId().toString());
//			}
//			
//			for (Link inLink : metadata.getInLinks()){
//				this.sensorManager.registerNumberOfCarsMonitoring(inLink.getId());
//				log.info("Register Inlink "+ inLink.getId().toString());
//				this.sensorManager.registerNumberOfCarsInDistanceMonitoring(inLink.getId(), gershensonConfig.getMonitoredPlatoonTail());
//				this.sensorManager.registerNumberOfCarsInDistanceMonitoring(inLink.getId(), gershensonConfig.getMonitoredDistance());
//				
//
//			}
//			
//			
//		}	
//	}

	
//TODO METADATA-Datastructure might be obsolete
	private void initSignalGroupMetadata(){
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.signalGroupIdMetadataMap = new HashMap<>();
		SignalSystemData systemData = signalsData.getSignalSystemsData().getSignalSystemData().get(this.system.getId());
		
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			if (!this.signalGroupIdMetadataMap.containsKey(signalGroup.getId())){
				this.signalGroupIdMetadataMap.put(signalGroup.getId(), new SignalGroupMetadata());
				log.info("SignalGroupIdMetadata for group "+ signalGroup.getId().toString());
			}
			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(signalGroup.getId());
			
			//InLinks and InLanes
			for (Signal signal : signalGroup.getSignals().values()){
				SignalData signalData = systemData.getSignalData().get(signal.getId());
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()|| scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()<=1. ){
					Link inLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
					metadata.addInLink(inLink);
					log.info("ADDING INLINK "+ inLink.toString() + " ; Signalgroup: "+signalGroup.getId().toString()+" with signal: "+signal.getId().toString());
				} else {
					LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signalData.getLinkId());
					for (Id<Lane> laneId : signal.getLaneIds()){
						metadata.addInLanes(lanes4link.getLanes().get(laneId));
						metadata.addLaneToLinkAssignment(laneId, lanes4link.getLinkId());
						
						log.info("ADDING INLANE "+ laneId.toString()+" ON LINK "+ lanes4link.getLinkId()+ " ; Signalgroup: "+signalGroup.getId().toString()+" with signal: "+signal.getId().toString());
					}
				}
			}
			//OutLinks
			for (Signal signal : signalGroup.getSignals().values()){
				Link inLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
				SignalData signalData = systemData.getSignalData().get(signal.getId());
				LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signalData.getLinkId());
				//There are no TurningMoveRestrictions
				if (signalData.getTurningMoveRestrictions() == null || signalData.getTurningMoveRestrictions().isEmpty()){
					//If there are no Lanes for that Link add every Outlink of the intersection except the one which allows the U-Turn
					if (signalData.getLaneIds() == null || signalData.getLaneIds().isEmpty() || scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()<=1){
						log.info("No TurninMoveRestrictions and no Lanes - adding every Outlink of intersections except the one in back direction");
						this.addOutLinksWithoutBackLinkToMetadata(inLink, metadata);
						
					}
					//There are Lanes:
					else{
						log.info("No TurninMoveRestrictions and there are Lanes - adding every Outlink of intersections except the one in back direction of each Lane");
						for(Id<Lane> laneId : signalData.getLaneIds()){
							Lane inLane = lanes4link.getLanes().get(laneId);
							addOutLinksWithoutBackLaneToMetadata(inLane,metadata);
						}
					}
				//There are TurningMoveRestrictions
				//ADD this set to the Methods for 5 and 6
				} else {
					log.info("If there are TurningMoverestriction add those to outlinks");
					for (Id<Link> linkid : signalData.getTurningMoveRestrictions()){
						Link outLink = scenario.getNetwork().getLinks().get(linkid);
						metadata.addOutLink(outLink);
					}
				}					
			}
		}
		//This is a fixed value and and doesn't have to be calculated in every iteration
		maximalNumberOfAgentsInDistance = (int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize());
	}
				
				//-----------------------------------------------------------
				
//				Link inLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
//				metadata.addInLink(inLink);
//				//outlinks
//				SignalData signalData = systemData.getSignalData().get(signal.getId());
//				if (signalData.getTurningMoveRestrictions() == null || signalData.getTurningMoveRestrictions().isEmpty()){
//					if (signalData.getLaneIds() == null || signalData.getLaneIds().isEmpty()){
//						this.addOutLinksWithoutBackLinkToMetadata(inLink, metadata);
//					}
//					else { // there are lanes
//						LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signalData.getLinkId());
//						for (Id<Lane> laneId : signalData.getLaneIds()){
//							Lane lane = lanes4link.getLanes().get(laneId);
//							if (lane.getToLinkIds() == null || lane.getToLinkIds().isEmpty()){
//								this.addOutLinksWithoutBackLinkToMetadata(inLink, metadata);
//							}
//							else{
//								for (Id<Link> toLinkId : lane.getToLinkIds()){
//									Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
//									if (!toLink.getFromNode().equals(inLink.getToNode())){
//										metadata.addOutLink(toLink);
//									}
//								}
//							}
//						}
//					}
//				}
//				else {  // turning move restrictions exist
//					for (Id<Link> linkid : signalData.getTurningMoveRestrictions()){
//						Link outLink = scenario.getNetwork().getLinks().get(linkid);
//						metadata.addOutLink(outLink);
//					}
//				}
//			}
//		}
//		
//		
//		
//		//This is a fixed value and and doesn't have to be calculated in every iteration
//		maximalNumberOfAgentsInDistance = (int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize());
//		
//		setThresholdfromCycleTime();
//	}
	
	
	private void addOutLinksWithoutBackLinkToMetadata(Link inLink, SignalGroupMetadata metadata){
		for (Link outLink : inLink.getToNode().getOutLinks().values()){
			if (!outLink.getToNode().equals(inLink.getToNode())){
				metadata.addOutLink(outLink);
			}
		}
	}
	private void addOutLinksWithoutBackLaneToMetadata(Lane inLane, SignalGroupMetadata metadata){
		if (inLane.getToLinkIds()!=null||!inLane.getToLinkIds().isEmpty()){
			for (Id<Link> toLink : inLane.getToLinkIds()){
				metadata.addOutLink(scenario.getNetwork().getLinks().get(toLink));
			}
		} else {
			Link inLink = scenario.getNetwork().getLinks().get(metadata.getLinkOfLane(inLane.getId()));
			addOutLinksWithoutBackLinkToMetadata(inLink,metadata);
		}		
	}
	
	
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		initSignalGroupMetadata();
		initSignalStates(simStartTimeSeconds);
		registerAndInitializeSensorManager();
		//TODO Adjust the Threshold according to the proposed formula.
		setThresholdfromCycleTime();
	}
		
	/**
	 * The signal group for that the next onset will be triggered
	 */
	
	
	private void switchlight(SignalGroup group, double timeSeconds) {
		if (group.getState() == null) {
			timecounter.put(group.getId(), 0.);
			counter.put(group.getId(), 0.);
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
			this.system.scheduleDropping(timeSeconds, group.getId());
		} else {
			
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
		
			if (group.getState().equals(SignalGroupState.RED)){
				this.system.scheduleOnset(timeSeconds + gershensonConfig.getInterGreenTime(), group.getId());
				timecounter.put(group.getId(), - gershensonConfig.getInterGreenTime());
				activeSignalGroup = group;
				counter.put(group.getId(), 0.);
			} else {
				this.system.scheduleDropping(timeSeconds, group.getId());
				timecounter.put(group.getId(), 0.);
				counter.put(group.getId(), 0.);
				
				//TODO does this even makes sense; might be obsolet
				for(Id<SignalGroup> groupid : this.signalGroupIdMetadataMap.keySet()){
					if (!groupid.equals(group.getId())&&(!this.system.getSignalGroups().get(groupid).getState().equals(SignalGroupState.RED)||!this.system.getSignalGroups().get(groupid).equals(activeSignalGroup))){
						this.system.scheduleDropping(timeSeconds, groupid);
					}
				}
				
				
				//Ensures that if a Group is switched from GREEN to RED that it is not the active Group anymore
				if(group.equals(activeSignalGroup)) activeSignalGroup=null;
			}
		}	
	}
	

	
	//Method to monitor approaching cars for a signal group
	private void carsOnInLinks(SignalGroup group, double now) {
		if (!approachingVehiclesMap.containsKey(group.getId())) {
			approachingVehiclesMap.put(group.getId(), new HashMap<Signal,Integer>());
		}
		for (Signal signal : group.getSignals().values()) {
			if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()||scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()<=1.){
//			if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()){
				//There are Link Exceptions
				if(signalGroupIdMetadataMap.get(group.getId()).getInLinkLengthExceptions().containsKey(signal.getLinkId())){
					Integer cars = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), signalGroupIdMetadataMap.get(group.getId()).getInLinkLengthExceptions().get(signal.getLinkId()), now); 
					approachingVehiclesMap.get(group.getId()).put(signal, cars);
					//There are no Link-Exceptions	
				} else {
					Integer cars = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(),gershensonConfig.getMonitoredDistance(), now); 
					approachingVehiclesMap.get(group.getId()).put(signal, cars);
				}
					
			} else {
				Integer cars = new Integer(0);
				for (Id<Lane> laneId : signal.getLaneIds()){
					//There are Lane-Exceptions	

					
					if(signalGroupIdMetadataMap.get(group.getId()).getInLanesLengthExceptions().containsKey(laneId)){
						cars += this.sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId ,signalGroupIdMetadataMap.get(group.getId()).getInLanesLengthExceptions().get(laneId), now);
					//Normal Case - No Length of Lane Exception
					} else {
						cars += this.sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId ,gershensonConfig.getMonitoredDistance(), now);
					}
				}
				
				
//				/if(group.getId().toString().equals("SignalGroup2")) log.error(cars);
				approachingVehiclesMap.get(group.getId()).put(signal, cars);
			}
		}	
//		if (group.getId().toString().equals("SignalGroup2")){	
//			log.error(group.getId().toString()+" has: ");
//				for (Signal signalwqw : approachingVehiclesMap.get(group.getId()).keySet()){
//					log.error("             at signal"+signalwqw.getId().toString()+ " are "+  approachingVehiclesMap.get(group.getId()).get(signalwqw));
//				}
//		}
	}
	
	//method to monitor cars behind intersection
	private void jamOnOutLink(SignalGroup group) {
		if (!jammedOutLinkMap.containsKey(group.getId())) {
			jammedOutLinkMap.put(group.getId(), new HashMap<Id<Link>,Boolean>());
		}

		for (Link link : signalGroupIdMetadataMap.get(group.getId()).getOutLinks()) {
			double storageCap = ((link.getLength()- gershensonConfig.getMinmumDistanceBehindIntersection()) * link.getNumberOfLanes()) / (this.scenario.getConfig().jdeqSim().getCarSize() * this.scenario.getConfig().qsim().getStorageCapFactor());
			boolean jammed = false;
			if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) > (storageCap * gershensonConfig.getStorageCapacityOutlinkJam())){
				jammed = true;
			}
			jammedOutLinkMap.get(group.getId()).put(link.getId(), jammed);
			
		}
		
	}
	
	private void initSignalStates(double now) {
		for(SignalGroup group : this.system.getSignalGroups().values()) {
			activeSignalGroup = null;
			system.scheduleDropping(now, group.getId());
		}		
	}
	
	
	//If a minimum Cycle-Time for Rule 1 should be used, call this method.
	private void setThresholdfromCycleTime(){
		int minCycleTime = gershensonConfig.getMinmumCycleTime();
		double intergreenTime = gershensonConfig.getInterGreenTime();
		int numberOfGroups = signalGroupIdMetadataMap.size();
		
		
		//This ensures that the threshold adjusts if there are more than one in InLink per Group that the Counter is not reached to quickly
		double averageInLinksinGroup = 0.;
		double avgMaximalNumberOfAgentsInDistance=0.;
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			// count number of lanes of this signal group
			for (Signal signal : signalGroup.getSignals().values()) {
				avgMaximalNumberOfAgentsInDistance += (double) maximalNumberOfAgentsInDistanceMap.get(signal.getId());
				if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()) {
					averageInLinksinGroup += 1;
				} else {
					averageInLinksinGroup += signal.getLaneIds().size();
				}
			}
		}
		avgMaximalNumberOfAgentsInDistance/=maximalNumberOfAgentsInDistanceMap.size();
		
		log.info("The average maximal numbers of agents in distance is "+avgMaximalNumberOfAgentsInDistance+". Default was: "+(int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize()));
		
		
		if (averageInLinksinGroup != 0.){
			averageInLinksinGroup /= numberOfGroups;
		} else averageInLinksinGroup = 1;
		
		//TODO Explanation
		this.threshold  = averageInLinksinGroup*avgMaximalNumberOfAgentsInDistance*(minCycleTime-intergreenTime*numberOfGroups)/numberOfGroups;		
		double oldThreshold = gershensonConfig.getThreshold();	
		
		log.info("Set Threshold from "+ oldThreshold + " to "+ this.threshold  +" [veh*s]");
	}
	
	
	
	private boolean jammedafterIntersection(SignalGroup group) {
		boolean jammed = false;
		if (jammedOutLinkMap.get(group.getId()) != null){
			if(jammedOutLinkMap.get(group.getId()).containsValue(true)) jammed = true;
		}
		return jammed;
	}
	
	//fills approachingVehiclesGroupMap, adding up every link for each group
	private void carsApproachInGroup() {
		approachingVehiclesGroupMap.clear();
		for (SignalGroup group : this.system.getSignalGroups().values()) {
			int vehicles = 0;
			for(Signal signal : approachingVehiclesMap.get(group.getId()).keySet()){
				
				//Fix Agents distance here for the case that there is no length of lane exceptions //TODO
				if(maximalNumberOfAgentsInDistanceMap.get(signal.getId()) < approachingVehiclesMap.get(group.getId()).get(signal)){
					vehicles += maximalNumberOfAgentsInDistanceMap.get(signal.getId());
				} else {
					vehicles += approachingVehiclesMap.get(group.getId()).get(signal);
				}
			}
		approachingVehiclesGroupMap.put(group.getId(), vehicles);
		}
	}
		
	
	
	private void updatecounter(){	
		for(SignalGroup group : this.system.getSignalGroups().values()) {
			if (approachingVehiclesGroupMap.containsKey(group.getId())){
				if (!counter.containsKey(group.getId())){
					counter.put(group.getId(), 0.);
				}
					
				if (group.getState() != null ||activeSignalGroup!=null){
					if (group.getState().equals((SignalGroupState.RED))){
						counter.put(group.getId(), counter.get(group.getId()) + new Double(approachingVehiclesGroupMap.get(group.getId())));
					} 
				}
			}
		}
	}
	

	private Boolean rule3Tester(SignalGroup signalGroup, double time) {
		Boolean VehInR = false;
		for (Signal signal : signalGroup.getSignals().values()) {
			if(VehInR) return VehInR;
			if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()||scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()<=1.){
//			if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()){
				if(!VehInR) {
					//There are Link-Sensor-Excepetions ie. a lane was to short for the suggested montitored distances by Gershenson
					if (signalGroupIdMetadataMap.get(signalGroup.getId()).getInLinkLengthExceptions().containsKey(signal.getLinkId())){
						double lengthOfLink = signalGroupIdMetadataMap.get(signalGroup.getId()).getInLinkLengthExceptions().get(signal.getLinkId());
						if(lengthOfLink>=gershensonConfig.getMonitoredPlatoonTail()+gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
							int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail(), time);
							if(numberVehiclesInShortDistance < gershensonConfig.getLengthOfPlatoonTails() && 
									numberVehiclesInShortDistance > 0 &&
									numberVehiclesInShortDistance - sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), lengthOfLink, time) == 0
									) {
								VehInR = true;
								break;
							}
						}
				//No LinkSensor-Exceptions:	
					} else {
						int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail(), time);
						if(numberVehiclesInShortDistance < gershensonConfig.getLengthOfPlatoonTails() && 
								numberVehiclesInShortDistance > 0 &&
								numberVehiclesInShortDistance - sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), gershensonConfig.getMonitoredDistance(), time) == 0
								) {
							VehInR = true;
							break;
						}
					}
				}
			} else {
				if(!VehInR) {
					for(Id<Lane> laneId : signal.getLaneIds()){
						//There are Link-Sensor-Excepetions ie. a lane was to short for the suggested montitored distances by Gershenson
						if(signalGroupIdMetadataMap.get(signalGroup.getId()).getInLanesLengthExceptions().containsKey(laneId)){
							double lengthOfLane = signalGroupIdMetadataMap.get(signalGroup.getId()).getInLanesLengthExceptions().get(laneId);
							if(lengthOfLane>=gershensonConfig.getMonitoredPlatoonTail()+2*2*this.scenario.getConfig().jdeqSim().getCarSize()){
								int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId,gershensonConfig.getMonitoredPlatoonTail(), time);
								if(numberVehiclesInShortDistance < gershensonConfig.getLengthOfPlatoonTails() && 
										numberVehiclesInShortDistance > 0 &&
										numberVehiclesInShortDistance - sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId, lengthOfLane , time) == 0
								){
									VehInR = true;
									break;
								}
							}
							
						//No LinkSensor-Exceptions:	
						}else{
							int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId,gershensonConfig.getMonitoredPlatoonTail(), time);
							if(numberVehiclesInShortDistance < gershensonConfig.getLengthOfPlatoonTails() && 
									numberVehiclesInShortDistance > 0 &&
									numberVehiclesInShortDistance - sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId,  gershensonConfig.getMonitoredDistance(), time) == 0
							){
								VehInR = true;
								break;
							}
						}
					}
				}
			}
		}
		return VehInR;
	}
		

		
	

	private Double highestcount(){
		Double highest = 0.;
		for (Id<SignalGroup>signalgroup : counter.keySet()) {
			if(!jammedafterIntersection(this.system.getSignalGroups().get(signalgroup))) {
				if (highest==0.) highest = counter.get(signalgroup);
				if(highest<counter.get(signalgroup)) highest = counter.get(signalgroup);
			}
		}
		return highest;
	}
	
	private Boolean vehiclesApproachingSomewhere() {
		Boolean approach = false;
		for (Id<SignalGroup> signal : approachingVehiclesGroupMap.keySet()) {
			if(!approach && approachingVehiclesGroupMap.get(signal).compareTo(0)!=0) {
				approach = true;
			}
		}
		return approach;
	}
	
	
	
	
	//-----------------------------

	//-------------------------
	
	private Map<Id<SignalGroup>, Map<Signal, Integer> > approachingVehiclesMap = new HashMap<Id<SignalGroup>, Map<Signal, Integer>>();
	private Map<Id<SignalGroup>, Integer> approachingVehiclesGroupMap = new HashMap<Id<SignalGroup>, Integer>();
	private Map<Id<SignalGroup>, Map<Id<Link>, Boolean> > jammedOutLinkMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Boolean>>();	
	private Map<Id<SignalGroup>, Double>  timecounter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Double> counter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Boolean> jammedSignalGroup = new HashMap<Id<SignalGroup>,Boolean>();
	private SignalGroup activeSignalGroup = null;
	private int maximalNumberOfAgentsInDistance;
	private Map<Id<Signal>, Integer>  maximalNumberOfAgentsInDistanceMap = new HashMap<Id<Signal>, Integer>();
	private double threshold;
	

	
	
	@Override
	public void updateState(double timeSeconds) {
	// Get data which is needed before going through the rule set	
		
		//This is just to demonstrate that the algorithm works
//		if (timeSeconds%1==0) {
//			if (activeSignalGroup!= null){
//				log.info("At "+timeSeconds +" "+ activeSignalGroup.getId().toString() + " is active and is" + activeSignalGroup.getState());
//			} else {
//				log.info("At "+timeSeconds + " active Signalgroup is not initilised");
//			}
//			log.info("At "+timeSeconds +" : "+ counter.toString() + " ");
//			log.info(" ");
//		}
		
		//add one second to the timecounter of eachgroup
		for (SignalGroup group : this.system.getSignalGroups().values()){
			timecounter.merge(group.getId(), 0., (a,b) -> a+1.);
	
		}
		
		for(SignalGroup group : this.system.getSignalGroups().values()){
			boolean greenbefore = false;
			if (group.getState()!=null){
				if (greenbefore && group.getState().equals(SignalGroupState.GREEN)){
					log.error("BOTH GROUPS ARE GREEN");
					break;
				}
				if (group.getState().equals(SignalGroupState.GREEN)) greenbefore = true;
			}
		}
		
		//Only run through the ruleset if there is no active Signalgroup (i.e. all are RED) or if the activeSignal-Group is Green
		// -> If the active Signalgroup is Red it should be due to the intergreen time				
		if( activeSignalGroup ==null|| activeSignalGroup.getState().equals(SignalGroupState.GREEN)) {
			
			
			jammedSignalGroup.clear();
			jammedOutLinkMap.clear();
			approachingVehiclesMap.clear();
			
			
			
			for (SignalGroup group : this.system.getSignalGroups().values()){

		//fills approachingVehiclesMap
				carsOnInLinks(group,timeSeconds);
		//fills jammedOutLinkMap
				jamOnOutLink(group);
				
				jammedSignalGroup.put(group.getId(), jammedafterIntersection(group));
			}
			
		//fills approachingVehiclesGroupMap and counter (product of internal times with vehicles)
			carsApproachInGroup();
			updatecounter();
		//Find the group with the highest count (product of internal time with vehicles in D s.t. no OutLink-Jam)
			Double signalGrouphighestCount = highestcount();
			boolean thereAreVehiclesApproaching = vehiclesApproachingSomewhere();
			
	//-----------------End of Preparation--------------
	
		//Rule 6
		//if the whole intersection is jammed OR
		//no Cars approaching the SignalSystem.
		//	-> turn all signals to RED	
			
			if (!jammedSignalGroup.containsValue(false)  || (!thereAreVehiclesApproaching)) {
//				log.info("Envoke Rule 6 at "+timeSeconds + !vehiclesApproachingSomewhere());
//				log.info("Rule 6");
//				log.info("all jammed: "+ !jammedSignalGroup.containsValue(false));
//				log.info("no cars approaching: "+!thereAreVehiclesApproaching);
				
				
					for (SignalGroup group : this.system.getSignalGroups().values()){ 			
						if (activeSignalGroup != null && activeSignalGroup.equals(group)) switchlight(group,timeSeconds);
					}
			
//					for(SignalGroup group : this.system.getSignalGroups().values()){
//						boolean greenbefore = false;
//						if (group.getState()!=null){
//							if (greenbefore && group.getState().equals(SignalGroupState.GREEN)){
//								log.error("BOTH GROUPS ARE GREEN -RULE6" );
//								break;
//							}
//							if (group.getState().equals(SignalGroupState.GREEN)) greenbefore = true;
//						}
//					}
				
				//if(!vehiclesApproachingSomewhere()) activeSignalGroup=null;
			} else {
		//Rule 5
		//At least one Outlink is jammed - from all groups, which are not jammed turn the one with the highest counter to GREEN
		//the others to RED
		//The switchlightCounter ensures that the activeSignalGroup is set to null if necessary
				if (jammedSignalGroup.containsValue(true) && activeSignalGroup !=null) {
					//log.info("Envoke Rule 5 at "+timeSeconds + jammedSignalGroup.toString());
					log.info("One is jammed:" +jammedSignalGroup.containsValue(true)+ "and there is an active group: "+activeSignalGroup !=null);
					int switchlightcounter = 0;
					for (SignalGroup group : this.system.getSignalGroups().values()) {
						//The active signalgroup is jammed behind the intersection, switch it to Red
						
						
						if (jammedSignalGroup.get(group.getId()) && activeSignalGroup.equals(group))
								/*signalGroupstatesMap.get(group.getId()).equals(SignalGroupState.GREEN))*/ {
							switchlight(group,timeSeconds);
							switchlightcounter++;
//							log.info("Fehler 1 - Rule 5");
//							log.info("active ");
						} else {
		// Set a not active Signalgroup to Green if its is not jammed and it has the highest number of cars approaching
		// if at there are some
							System.err.println("group: " + group + ", "
									+ "activeSignal: " + activeSignalGroup);
							if ( !jammedSignalGroup.get(group.getId()) && 
									counter.get(group.getId())==signalGrouphighestCount && 
									counter.get(group.getId())!=0.0 &&
									group.getState().equals(SignalGroupState.RED) 			
								|| (!activeSignalGroup.equals(group) && 
										group.
										getState().
										equals(SignalGroupState.GREEN))){
								
								switchlight(group,timeSeconds);
								switchlightcounter++;
//								log.info("Fehler 2 - Rule 5 " + switchlightcounter);
							}
						}	
					}
			//If only one switch happened, all groups should be red and active group should be null
					//if (switchlightcounter==1) activeSignalGroup=null;
				
					
				} else {
		//Complement to Rule 6
		//All Signals are RED, but there is no jam on Outlinks anymore. Restore a single Green for the Group with the highest count
					if (activeSignalGroup==null) {
//						log.info("no active Signalgroup: "+ activeSignalGroup==null);
						if (jammedSignalGroup.containsValue(false)){
//							log.info("Rule 6 complement. One group is jammed on Outlink: "+jammedSignalGroup.containsValue(false));
							for (SignalGroup group : this.system.getSignalGroups().values()) {
//								log.info("Counter of Group " +group.getId().toString()+ " is "+counter.get(group.getId())+ " group is " + group.getState().toString());
								if (signalGrouphighestCount!=0.0 && counter.get(group.getId()) == signalGrouphighestCount) {
									switchlight(group,timeSeconds);
//									log.info("Complement Rule 6 switched light of group " +group.getId().toString());
									break;
								}
							}
						}
							
						
					} else {
		//Rule 4
		//One Group has GREEN but no cars approaching and another group has RED and Cars approaching (switch them)
						if(approachingVehiclesGroupMap.containsValue(0) && thereAreVehiclesApproaching) {
//							log.info("There is a group where no one is approaching " +approachingVehiclesGroupMap.containsValue(0));
//							log.info("There are vehicles approacing " +thereAreVehiclesApproaching);
							int highest = 0;
							SignalGroup firstlight=null; 
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (activeSignalGroup != null){								
									if (group.getState().equals(SignalGroupState.GREEN) &&
											(approachingVehiclesGroupMap.get(group.getId())==0 )){
											
										//Active group should be switched to red
										switchlight(group,timeSeconds);
//										log.info("Rule 4 - Group "+group.getId().toString()+" is green: "+group.getState().equals(SignalGroupState.GREEN));
//										log.info("but there are Cars arriving:" +approachingVehiclesGroupMap.get(group.getId()) + "->switch to RED");
									} else {
										if(
												//approachingVehiclesGroupMap.get(activeSignalGroup.getId())==0  &&
												counter.get(group.getId()) == signalGrouphighestCount &&
												group.getState().equals(SignalGroupState.RED) &&
												counter.get(group.getId()) > 0.){
											switchlight(group,timeSeconds);
										}
									}
								} else {
									if (approachingVehiclesGroupMap.get(group.getId())>=highest) firstlight = group;
								}
							}
							//if (activeSignalGroup == null) switchlight(firstlight,timeSeconds);
							
						} else {
		//Rule 3
		//A signal should switch from GREEN to RED (according to Rule 1 and 2), but there are just a few cars left to pass: Don't switch
							boolean hasfewerThanMVehiclesInR= false;
							if (rule3Tester(activeSignalGroup, timeSeconds) ){
								hasfewerThanMVehiclesInR = true;
							}
							
							if (!hasfewerThanMVehiclesInR) {
		//Rule 2
		//A signal should switch from GREEN to RED (according to Rule 1), but internal time of the Group hasn't reached
		//the minimal green time yet - Dont't switch
								int counterRule2 = 0;
								for (SignalGroup group : this.system.getSignalGroups().values()) {
									if (group.getState().equals(SignalGroupState.GREEN) && timecounter.get(group.getId())< gershensonConfig.getMinimumGREENtime()) counterRule2++;
								}
								if (counterRule2==0) {
		//Rule 1
									LinkedList<SignalGroup> potentialRule1 = new LinkedList<SignalGroup>();
									for (SignalGroup group : this.system.getSignalGroups().values()) {
										if (!group.equals(activeSignalGroup) && counter.get(group.getId())>this.threshold) {
											potentialRule1.add(group);
											//log.info("Rule 1: Signalgroup "+ group.getId().toString() + " reached the threshold");
										}
									}
									if(!potentialRule1.isEmpty()) {
										SignalGroup groupR1 = null;
										for(SignalGroup group : potentialRule1) {
											if (groupR1==null) groupR1=group;
											if(counter.get(groupR1.getId())<counter.get(group.getId())) groupR1=group;
										}
										//some og it obsolet
										for (SignalGroup group : this.system.getSignalGroups().values()) {	
											if (group.equals(activeSignalGroup)) switchlight(group,timeSeconds);
											if (group.getState().equals(SignalGroupState.GREEN)) switchlight(group,timeSeconds);
											if (group.equals(groupR1)){
												//log.info(groupR1.getId().toString()+" reached the threshold- counter: "+counter.get(groupR1.getId())+ " threshold was: "+gershensonConfig.getThreshold());
												switchlight(group,timeSeconds);
											}
											
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

			
			
	@Override
	public void addPlan(SignalPlan plan) {
		//nothing to do here as we don't deal with plans
	}

	@Override
	public void reset(Integer iterationNumber) {
		// TODO call initialize method or similar
		// simulationInitialized-Method is called anyway so there is no need to do anything here
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem ;		
	}	

	

}
