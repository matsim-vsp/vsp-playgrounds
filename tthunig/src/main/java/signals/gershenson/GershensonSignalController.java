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

import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

import com.google.inject.Inject;


/**
 * Implement Gershenson-Controller as outlined in "Self-Organizing Traffic Lights at
 * Multiple-Street Intersections" (Gershenson/Rosenblueth 2011). 
 * NULL-State: All Groups are RED if nobody approaches the SignalSystem. 
 * See config for default values
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
	
	public final static class GershensonFactory implements SignalControllerFactory {
		@Inject private LinkSensorManager sensorManager;
		@Inject private Scenario scenario;
		@Inject private GershensonConfig gershensonConfig;
	
		@Override
		public SignalController createSignalSystemController(SignalSystem signalSystem) {
			SignalController controller = new GershensonSignalController(scenario, sensorManager, gershensonConfig);
			controller.setSignalSystem(signalSystem);
			return controller;
		}
	}
	
	/**
	 * Each SignalGroup has an own datastructure:
	 * outlinks and exceptions in terms that an Link is too short to monitor a certain length
	 */
	
	private class SignalGroupMetadata {

		private Set<Link> outLinks;
		private HashMap<Id<Lane>,Double> inLanesLengthExceptions;
		private HashMap<Id<Link>,Double> inLinkLengthExceptions;
		private HashMap<Link,Link> outLinkExceptionNextLink;
		
		public SignalGroupMetadata(){
			this.outLinks = new HashSet<Link>();
			this.inLanesLengthExceptions  = new HashMap<Id<Lane>,Double>();
			this.inLinkLengthExceptions = new HashMap<Id<Link>,Double>();
			this.outLinkExceptionNextLink = new HashMap<Link,Link>();
		}
		
		public Set<Link> getOutLinks(){
			return this.outLinks;
		}	
		public void addOutLink(Link outLink) {
			this.outLinks.add(outLink);
			checkOutlinkLength(outLink);	
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
		
		public HashMap<Link,Link> getOutLinkExceptionNextLink(){
			return this.outLinkExceptionNextLink;
		}		
		public void setOutLinkExceptionNextLink(Link link, Link next){
			this.outLinkExceptionNextLink.put(link, next);
		}
		
		
//		There are no Turning-Move restrictions:
//			An agent could potentially drive in every direction at this intersection if he comes from the InLink (U-Turn is prohibited)
		public void addOutLinksWithoutBackLinkToMetadata(Link inLink){
			for (Link outLink : inLink.getToNode().getOutLinks().values()){
				if (!outLink.getToNode().equals(inLink.getToNode())){
					addOutLink(outLink);
				}
			}
		}
		
//		There are no Turning-Move restrictions:
//			An agent could potentially drive in every direction at this intersection if he comes from the InLane (U-Turn is prohibited)
		public void addOutLinksWithoutBackLaneToMetadata(Lane inLane, LanesToLinkAssignment lanes4link){
			if (inLane.getToLinkIds()!=null||!inLane.getToLinkIds().isEmpty()){
				for (Id<Link> toLink : inLane.getToLinkIds()){
					Link outLink = scenario.getNetwork().getLinks().get(toLink);
					addOutLink(outLink);
				}
			} else {
				Link inLink = scenario.getNetwork().getLinks().get(lanes4link.getLinkId());
				addOutLinksWithoutBackLinkToMetadata(inLink);
			}		
		}		
		
//		This method checks if an outlink is too short to be monitored.
//				if yes: Check if there is one Link after that Outlink (U-turn is prohibited)
//					if there is only one Link: add it to the outLinkExceptionNextLink-Map
//					put null if there is none or more than one
		private void checkOutlinkLength(Link outLink){
			double length = outLink.getLength();
			if (length<=gershensonConfig.getMinmumDistanceBehindIntersection()){
				Node toNode = outLink.getToNode();
				Node fromNode = outLink.getFromNode();
				Set<Link> followLinks = new HashSet<Link>();
				for (Link link : toNode.getOutLinks().values()){
					if (!link.getToNode().equals(fromNode)) followLinks.add(link);
				}
				log.warn(system.getId().toString()+": The Outlink "+ outLink.getId().toString()+" is shorter than "+gershensonConfig.getMinmumDistanceBehindIntersection() +
						" m (actual: "+length+" m) to monitor at least one car. Try to add next link (not the reversed Link back to the intersection)");				
				if(followLinks.size()==1){
					setOutLinkExceptionNextLink(outLink, followLinks.iterator().next());
					length += followLinks.iterator().next().getLength();
					if (length<=gershensonConfig.getMinmumDistanceBehindIntersection()){
						log.warn(system.getId().toString()+": The Outlink "+ outLink.getId().toString()+" and the next Link are combinded still too short");
					}
				}else{
					log.error(system.getId().toString()+": The Outlink "+ outLink.getId().toString()+"has no or more than one following Link. May be fatal for the signalsystem");
					setOutLinkExceptionNextLink(outLink, null);
				}
			}
		}		
	}

	private static final Logger log = Logger.getLogger(GershensonSignalController.class);
	
	private SignalSystem system;
	private LinkSensorManager sensorManager;
	private Scenario scenario;
	private GershensonConfig gershensonConfig;
	
	private Map<Id<SignalGroup>, SignalGroupMetadata> signalGroupIdMetadataMap;	
	
	private Map<Id<SignalGroup>, Integer> approachingVehiclesGroupMap = new HashMap<Id<SignalGroup>, Integer>();
	private Map<Id<SignalGroup>, Map<Id<Link>, Boolean> > jammedOutLinkMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Boolean>>();	
	private Map<Id<SignalGroup>, Double> counter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Boolean> jammedSignalGroup = new HashMap<Id<SignalGroup>,Boolean>();
	private SignalGroup activeSignalGroup;
	private Map<Id<Signal>, Integer>  maximalNumberOfAgentsInDistanceMap = new HashMap<Id<Signal>, Integer>();
	private double threshold;
	private double timeactivegroup = 0;
	private boolean thereAreVehiclesApproaching;
	
	
	private GershensonSignalController(Scenario scenario, LinkSensorManager sensorManager, GershensonConfig gershensonConfig) {
		this.scenario = scenario;
		this.sensorManager = sensorManager;
		this.gershensonConfig = gershensonConfig;
	}

	
	/**
	 * Initialization for the SignalGroup Metadata:
	 * 	1.  Loop over all Signalgroups of the signal system and add an empty Metadata to the signalGroupIdMetadataMap
	 * 	2.  Loop over all signals in that Signalgroup to add Outlinks to Metadata
	 * 	2.1 Check if there are Turning-Move restrictions
	 * 	2.2 Check if there are Lanes or only Links	
	 */
	private void initSignalGroupMetadata(){
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.signalGroupIdMetadataMap = new HashMap<>();
		SignalSystemData systemData = signalsData.getSignalSystemsData().getSignalSystemData().get(this.system.getId());
		
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			if (!this.signalGroupIdMetadataMap.containsKey(signalGroup.getId())){
				this.signalGroupIdMetadataMap.put(signalGroup.getId(), new SignalGroupMetadata());
				log.info(system.getId().toString()+": SignalGroupIdMetadata for group "+ signalGroup.getId().toString());
			}
			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(signalGroup.getId());
			
//			Add OutLinks:
			for (Signal signal : signalGroup.getSignals().values()){
				Link inLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
				SignalData signalData = systemData.getSignalData().get(signal.getId());
				LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signalData.getLinkId());
//				There are no TurningMoveRestrictions
				if (signalData.getTurningMoveRestrictions() == null || signalData.getTurningMoveRestrictions().isEmpty()){
//					If there are no Lanes for that Link add every Outlink of the intersection except the one which allows the U-Turn
					if (signalData.getLaneIds() == null || signalData.getLaneIds().isEmpty() || lanes4link.getLanes().size()<=1){
						log.info(system.getId().toString()+": No TurninMoveRestrictions and no Lanes - adding every Outlink of intersections except the one in back direction");
						metadata.addOutLinksWithoutBackLinkToMetadata(inLink);
						
					}
//					If there are Lanes:
					else{
						log.info(system.getId().toString()+": No TurninMoveRestrictions and there are Lanes - adding every Outlink of intersections except the one in back direction of each Lane");
						for(Id<Lane> laneId : signalData.getLaneIds()){
							Lane inLane = lanes4link.getLanes().get(laneId);
							metadata.addOutLinksWithoutBackLaneToMetadata(inLane,lanes4link);
						}
					}
//				There are TurningMoveRestrictions
//				Add those as Outlinks
				} else {
					log.info(system.getId().toString()+": There are TurningMoverestriction - add those to outlinks");
					for (Id<Link> linkid : signalData.getTurningMoveRestrictions()){
						Link outLink = scenario.getNetwork().getLinks().get(linkid);
						metadata.addOutLink(outLink);
					}
				}					
			}
		}
	}
	
	/**
	 *  Register and initialize the Sensor Manager of the system and fill the maximalNumberOfAgentsInDistanceMap:
	 *  	1. Loop over all SignalGroups and get the metadata
	 *  	2. Loop over the Signals
	 *  	3. Register two Signal-Sensors for each InLink - check if the Lanes/Links have the required length and warn user if not
	 *  	4. register Sensor for Outlink and check for exceptions
	 *  
	 *  maximalNumberOfAgentsInDistanceMap is needed as matsim stucks agents ontop of each other so the sensor would count to many agents in Distance
	 */	
	private void registerAndInitializeSensorManager() {
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(signalGroup.getId());
			
//			Register InLinks/InLanes for the Sensor-Manager
			for(Signal signal : signalGroup.getSignals().values()){
				
				LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId());

				
//				Put the default value in maximalNumberOfAgentsInDistanceMap first. If the link is to short this will be overwritten
				maximalNumberOfAgentsInDistanceMap.put(signal.getId(), (int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize()));

//				There are only Links (if there is only one Lane on the Link ignore this Lane and work with Link-Sensors instead)
				if((signal.getLaneIds()==null)|| signal.getLaneIds().isEmpty() ||lanes4link.getLanes().size()<=1. ){
					if (!(signal.getLaneIds()==null|| signal.getLaneIds().isEmpty()) && scenario.getNetwork().getLinks().get(signal.getLinkId()).getNumberOfLanes()==1. ){
						log.info(system.getId().toString()+": Register InLane "+ signal.getLaneIds().iterator().next().toString() +" of Signal "+ signal.getId().toString()+". Since there is only one Lane for this signal register the Link");
					} else {
						log.info(system.getId().toString()+": Register Inlink "+ signal.getLinkId().toString() +" of Signal "+ signal.getId().toString());
					}
		
					double lenghtOfLink = scenario.getNetwork().getLinks().get(signal.getLinkId()).getLength();
					
//					The InLink is shorter than the required monitored distance from the config
//						-> Adjust the number of agents in maximalNumberOfAgentsInDistanceMap (minimum is 1 agent)
					if (lenghtOfLink<gershensonConfig.getMonitoredDistance()){
						int adjustedNumberOfAgents = java.lang.Math.max((int)(lenghtOfLink/this.scenario.getConfig().jdeqSim().getCarSize()),1);
						maximalNumberOfAgentsInDistanceMap.put(signal.getId(), adjustedNumberOfAgents);
						
//						For rule 3 a shorter distance is monitored as well
//							if the link is shorter than this distance rule 3 is not going to execute.
//							save this exception
						if (lenghtOfLink<gershensonConfig.getMonitoredPlatoonTail()){
							log.warn(system.getId().toString()+": The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredPlatoonTail()+" m. Register one Sensor at"+ lenghtOfLink+
								" m. The sensor for Rule 3 is set 0´. This means Rule 3 is not going to execute. This might be not fatal but annoying.");
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
//						The difference between the two registered sensor is shorter than Length of PlatoonTails.
//							The system would never switch the light because rule 3 would always be true
						} else if (lenghtOfLink-gershensonConfig.getMonitoredPlatoonTail()<gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
							log.warn(system.getId().toString()+": The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. Moreover the the length of this lane is too short to execute Rule 3 correctly (The monitored distance between the two sensors would be less than two cars-sizes). "
									+ "Register one Sensor at"+ lenghtOfLink+
									" m. The sensor for Rule 3 is set 0´. ");
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
//						Register both sensors but warn the user 
						} else {
							log.warn(system.getId().toString()+": The length of "+signal.getLinkId().toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. This is may not be fatal."
									+ "Two sensors are registered: First at "+lenghtOfLink+" m. Secondly at "+gershensonConfig.getMonitoredPlatoonTail()+ " m.");							
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail());
							this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), lenghtOfLink);
							metadata.setInLinkLengthExceptions(signal.getLinkId(), lenghtOfLink);
						}
//					This should be the normal case, everything is registered as expected	
					} else {
						this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail());
						this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), gershensonConfig.getMonitoredDistance());
					}
//				There are lanes. Follow the sam logic as above to check if the sensor will work as wanted
				}else{
					for(Id<Lane> laneId : signal.getLaneIds()){
						log.info(system.getId().toString()+": Register Sensor for InLane "+ laneId.toString() +" of Signal "+ signal.getId().toString());
						double lenghtOfLane = lanes4link.getLanes().get(laneId).getStartsAtMeterFromLinkEnd();
//						For rule 3 a shorter distance is monitored as well
//						if the lane is shorter than this distance rule 3 is not going to execute.
//						save this exception
						if (lenghtOfLane<gershensonConfig.getMonitoredDistance()){
							int adjustedNumberOfAgents = java.lang.Math.max((int)(lenghtOfLane/this.scenario.getConfig().jdeqSim().getCarSize()),1);
							maximalNumberOfAgentsInDistanceMap.put(signal.getId(), adjustedNumberOfAgents);
							if (lenghtOfLane<gershensonConfig.getMonitoredPlatoonTail()){
								log.warn(system.getId().toString()+": The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredPlatoonTail()+" m. Register one Sensor at"+ lenghtOfLane+
										" The sensor for Rule 3 is set 0´. This means Rule 3 is not going to execute. This might be not fatal but annoying.");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
//								The difference between the two registered sensor is shorter than Length of PlatoonTails.
//								The system would never switch the light because rule 3 would always be true								
							} else if (lenghtOfLane-gershensonConfig.getMonitoredPlatoonTail()<gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
								log.warn(system.getId().toString()+": The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. Moreover the the length of this lane is too short to execute Rule 3 correctly (The monitored distance between the two sensors would be less than two cars-sizes). "
										+ "Register one Sensor at"+ lenghtOfLane+
										" The sensor for Rule 3 is set 0´. ");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
//							Register both sensors but warn the user 
							} else {
								log.warn(system.getId().toString()+": The length of "+laneId.toString() +" of Signal "+ signal.getId().toString()+" is smaller than "+gershensonConfig.getMonitoredDistance()+" m. This is may not be fatal and is irgonered."
										+ "Two sensors are registered: First at "+lenghtOfLane+" m. Secondly at "+gershensonConfig.getMonitoredPlatoonTail()+ " m.");
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredPlatoonTail());
								this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, lenghtOfLane);
								metadata.setInLanesLengthExceptions(laneId, lenghtOfLane);
							}
//						This should be the normal case, everything is registered as expected	
						} else {
							this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredPlatoonTail());
							this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, gershensonConfig.getMonitoredDistance());
							
						}
					}
				}
			}
			
//			Register sensors for the outlinks. The checks for the length is done in the initMetadata-Method
			for (Link outLink : metadata.getOutLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(outLink.getId());
				log.info(system.getId().toString()+": Register Outlink "+ outLink.getId().toString());
//				If the Outlink is too short, try to add the following link
//				If both links together are two short only one sensor is registered 
				if (metadata.getOutLinkExceptionNextLink().containsKey(outLink)){
					if (metadata.getOutLinkExceptionNextLink().get(outLink)!=null){
						Link nextLink = metadata.getOutLinkExceptionNextLink().get(outLink);
						this.sensorManager.registerNumberOfCarsMonitoring(nextLink.getId());
					} 
				}
			}
		}
	}

	/**
	 * If the given SignalGroup is active -> schedule dropping -> all signals are red
	 * If the the given Signalgroup is not active -> check whether there is an other signal group active
	 * 			1. if not: -> everything was red -> schedule Onset for Signalgroup
	 * 			2. if yes: -> another group was active -> schedule drop for the old group and Onset for the new group								
	*/
	private void switchlight(SignalGroup group, double timeSeconds) {
		if (group.getState() == null) {
			counter.put(group.getId(), 0.);
			activeSignalGroup = null;
			this.system.scheduleDropping(timeSeconds, group.getId());
			timeactivegroup = 0.;
		} else {
			if (group.equals(activeSignalGroup) || group.getState().equals(SignalGroupState.GREEN)){
				counter.put(group.getId(), 0.);
				this.system.scheduleDropping(timeSeconds, group.getId());
				activeSignalGroup = null;
				timeactivegroup = 0.;
			} else {
				if (activeSignalGroup==null) {
					this.system.scheduleOnset(timeSeconds + gershensonConfig.getInterGreenTime(), group.getId());
					activeSignalGroup = group;
					timeactivegroup = -gershensonConfig.getInterGreenTime();
					counter.put(group.getId(), 0.);
				} else {
					this.system.scheduleDropping(timeSeconds, activeSignalGroup.getId());
					counter.put(activeSignalGroup.getId(), 0.);
					
					this.system.scheduleOnset(timeSeconds + gershensonConfig.getInterGreenTime(), group.getId());
					activeSignalGroup = group;
					timeactivegroup = -gershensonConfig.getInterGreenTime();
					counter.put(group.getId(), 0.);
				}
			}
		}
	}

	/**
	 * Initialize SignalStates:
	 * 	all dropping (ie. Red) and no active group
	 */
	private void initSignalStates(double now) {
		for(SignalGroup group : this.system.getSignalGroups().values()) {
			system.scheduleDropping(now, group.getId());
		}
		activeSignalGroup = null;
	}
		
	/**
	 * The is a logic to set the threshold according to the SignalSystem. There is a boolean in the config to switch if a default value should be used instead
	 */
	private void setThresholdfromCycleTime(){
		int equiCycleTime = gershensonConfig.getEquiCycleTime();
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
		
		log.info(system.getId().toString()+": The average maximal numbers of agents in distance is "+(int)avgMaximalNumberOfAgentsInDistance+
				". Default was: "+(int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize()));
			
		if (averageInLinksinGroup != 0.){
			averageInLinksinGroup /= numberOfGroups;
		} else averageInLinksinGroup = 1;
		
//		If there is a uniform demand from all directions and only Rule 1 is used to decide which SignalGroups to switch:
//			then this should be equivalent to a Signalsystem with a fixed plan with equiCycleTime 
		this.threshold  = averageInLinksinGroup*avgMaximalNumberOfAgentsInDistance*(equiCycleTime-intergreenTime*numberOfGroups)/numberOfGroups;		
		double oldThreshold = gershensonConfig.getThreshold();	
		
		log.info(system.getId().toString()+": Set Threshold from "+ oldThreshold 
				+ " to "+ this.threshold  +" [veh*s]");
	}

	
	
//	This tests rule 3: Only a few agents are left to pass -> don't switch
	private Boolean rule3Tester(SignalGroup signalGroup, double time) {
		Boolean VehInR = false;
		for (Signal signal : signalGroup.getSignals().values()) {
			if(VehInR) return VehInR;
			LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId());
 
			if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()||lanes4link.getLanes().size()<=1.){
				if(!VehInR) {
//					There are Link-Sensor-Exceptions ie. a lane was to short for the suggested monitored distances by Gershenson
					if (signalGroupIdMetadataMap.get(signalGroup.getId()).getInLinkLengthExceptions().containsKey(signal.getLinkId())){
						double lengthOfLink = signalGroupIdMetadataMap.get(signalGroup.getId()).getInLinkLengthExceptions().get(signal.getLinkId());
//						If this is not the case the Platoon-Sensor was registered at distance zero so Rule 3 is not feasable here
						if(lengthOfLink>=gershensonConfig.getMonitoredPlatoonTail()+gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
							int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), gershensonConfig.getMonitoredPlatoonTail(), time);
//							If there are only a few cars left to pass don't switch
//								(The difference of the sensor must be zero i.e behind that monitored distance are no other cars anymore)
							if(numberVehiclesInShortDistance <= gershensonConfig.getLengthOfPlatoonTails() && 
									numberVehiclesInShortDistance > 0 &&
									numberVehiclesInShortDistance - sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), lengthOfLink, time) == 0
									) {
								VehInR = true;
								break;
							}
						}
//				No LinkSensor-Exceptions:	
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
						//There are Link-Sensor-Exceptions ie. a lane was to short for the suggested monitored distances by Gershenson
						if(signalGroupIdMetadataMap.get(signalGroup.getId()).getInLanesLengthExceptions().containsKey(laneId)){
							double lengthOfLane = signalGroupIdMetadataMap.get(signalGroup.getId()).getInLanesLengthExceptions().get(laneId);
//							If this is not the case the Platoon-Sensor was registered at distance zero so Rule 3 is not feasable here
							if(lengthOfLane>=gershensonConfig.getMonitoredPlatoonTail()+gershensonConfig.getLengthOfPlatoonTails()*this.scenario.getConfig().jdeqSim().getCarSize()){
								int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId,gershensonConfig.getMonitoredPlatoonTail(), time);
//								If there are only a few cars left to pass don't switch
//								(The difference of the sensor must be zero i.e behind that monitored distance are no other cars anymore)
								if(numberVehiclesInShortDistance <= gershensonConfig.getLengthOfPlatoonTails() && 
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
	
	/**
	 * Method that will be called to monitor the current traffic situation in each simulation timestep
	 */
	private void updateMonitoredTraffic(double now){
		jammedSignalGroup.clear();
		jammedOutLinkMap.clear();
		approachingVehiclesGroupMap.clear();
//----------------------------------------------------------------------------------
		for (SignalGroup group : this.system.getSignalGroups().values()){
//------------------------------- Fills approachingVehiclesGroupMap, monitor approaching cars for a signal group
			if (!approachingVehiclesGroupMap.containsKey(group.getId())) {
				approachingVehiclesGroupMap.put(group.getId(), new Integer(0));
			}
			int carsapproaching = 0;
			for (Signal signal : group.getSignals().values()) {
				LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signal.getLinkId());

				int agents = 0;
				if (signal.getLaneIds()==null||signal.getLaneIds().isEmpty()||lanes4link.getLanes().size()<=1.){
//			There are Link Exceptions
					if(signalGroupIdMetadataMap.get(group.getId()).getInLinkLengthExceptions().containsKey(signal.getLinkId())){
						agents = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), signalGroupIdMetadataMap.get(group.getId()).getInLinkLengthExceptions().get(signal.getLinkId()), now); 
//			There are no Link-Exceptions	
					} else {
						agents = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(),gershensonConfig.getMonitoredDistance(), now); 
					}
						
				} else {
					for (Id<Lane> laneId : signal.getLaneIds()){
//						There are Lane-Exceptions	
						if(signalGroupIdMetadataMap.get(group.getId()).getInLanesLengthExceptions().containsKey(laneId)){
							agents += this.sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId ,signalGroupIdMetadataMap.get(group.getId()).getInLanesLengthExceptions().get(laneId), now);
//						Normal Case - No Length of Lane Exception
						} else {
							agents += this.sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId ,gershensonConfig.getMonitoredDistance(), now);
						}
						
					}
				}
//				Since Matsim is stucking agents on top of each other, just add the maximal possible numer of agents
				if(maximalNumberOfAgentsInDistanceMap.get(signal.getId()) < agents){
					carsapproaching += maximalNumberOfAgentsInDistanceMap.get(signal.getId());
				} else {
					carsapproaching += agents;
				}
			}
			approachingVehiclesGroupMap.put(group.getId(), carsapproaching);

//-------------------------------Fills counter
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

////-------------------------------Fills jammedOutLinkMap (is using the idea of droeder)
			SignalGroupMetadata metadata = signalGroupIdMetadataMap.get(group.getId());
			if (!jammedOutLinkMap.containsKey(group.getId())) {
				jammedOutLinkMap.put(group.getId(), new HashMap<Id<Link>,Boolean>());
			}

			for (Link link : metadata.getOutLinks()) {
								
				boolean jammed = false;
				if(!metadata.getOutLinkExceptionNextLink().containsKey(link)){
					double storageCap = ((link.getLength()- gershensonConfig.getMinmumDistanceBehindIntersection()) * link.getNumberOfLanes()) / (this.scenario.getConfig().jdeqSim().getCarSize() * this.scenario.getConfig().qsim().getStorageCapFactor());
					
					if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) > (storageCap * gershensonConfig.getStorageCapacityOutlinkJam())){
						jammed = true;
					}	
				} else {
					if (metadata.getOutLinkExceptionNextLink().get(link)!=null){
						Link nextLink = metadata.getOutLinkExceptionNextLink().get(link);
						int carsOnBothLinks = this.sensorManager.getNumberOfCarsOnLink(link.getId()) + this.sensorManager.getNumberOfCarsOnLink(nextLink.getId());
						double length = link.getLength()+ nextLink.getLength();
						double storageCap = ((length- gershensonConfig.getMinmumDistanceBehindIntersection())) / (this.scenario.getConfig().jdeqSim().getCarSize() * this.scenario.getConfig().qsim().getStorageCapFactor());
						
						if (storageCap>0){
							if (carsOnBothLinks > (storageCap * gershensonConfig.getStorageCapacityOutlinkJam())){
								jammed = true;
							}						
						} else {
							if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) >= 1){
								jammed = true;
							}
						}	
					}
				}
				jammedOutLinkMap.get(group.getId()).put(link.getId(), jammed);
			}
//-------------------------------Fills jammedSignalGroup
			boolean jammedGroup = false;
			if (jammedOutLinkMap.get(group.getId()) != null){
				if(jammedOutLinkMap.get(group.getId()).containsValue(true)) jammedGroup = true;
			}
			jammedSignalGroup.put(group.getId(), jammedGroup);		
		}
//---------------------------Set Boolean thereAreVehiclesApproaching
		Boolean approach = false;
		for (Id<SignalGroup> signalGroupId : approachingVehiclesGroupMap.keySet()) {
			if(!approach && approachingVehiclesGroupMap.get(signalGroupId).compareTo(0)!=0) {
				approach = true;
			}
		}
		thereAreVehiclesApproaching = approach;
	}
	
	/**
	 * Initialisation method will call all other init-Methods
	 * if a an adjusted threshold is wanted use the propose formular
	 */
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		initSignalGroupMetadata();
		initSignalStates(simStartTimeSeconds);
		registerAndInitializeSensorManager();
		
		if (gershensonConfig.getSignalSystemDependendThreshold()) setThresholdfromCycleTime();
	}
	
	
	/**
	 * Run in every update step through Gershensons rule set
	 */
	@Override
	public void updateState(double timeSeconds) {
		
//		Only run through the ruleset if there is no active Signalgroup (i.e. all are RED) or if the activeSignal-Group is Green
//		 -> If the active Signalgroup is Red it should be due to the intergreen time				
		if( activeSignalGroup ==null|| activeSignalGroup.getState().equals(SignalGroupState.GREEN)) {
//			add one second to the timecounter of the active group
			if(activeSignalGroup!=null) timeactivegroup++; 
//			 Get the current traffic situation which is needed before going through the rule set	
			updateMonitoredTraffic(timeSeconds);

// End of Preparation--------------
	
//		Rule 6
//		if the whole intersection is jammed OR
//		no Cars approaching the SignalSystem.
//			-> turn all signals to RED	
			
			if (!jammedSignalGroup.containsValue(false)  || (!thereAreVehiclesApproaching)) {
					if (activeSignalGroup != null) switchlight(activeSignalGroup,timeSeconds);
			} else {
//		Rule 5
//		At least one Outlink is jammed - from all groups, which are not jammed turn the one with the highest counter to GREEN
//		the others to RED
//		The switchlightCounter ensures that the activeSignalGroup is set to null if necessary
				if (jammedSignalGroup.containsValue(true) && activeSignalGroup !=null) {
					SignalGroup changerGroup = null;
					for (SignalGroup group : this.system.getSignalGroups().values()) {
						if (!jammedSignalGroup.get(group.getId())){
							if (changerGroup==null){
								changerGroup = group;
							} else {
								if (counter.get(group.getId())> counter.get(changerGroup.getId())){
									changerGroup = group;
								}
							}
						}
					}
					if (!changerGroup.equals(activeSignalGroup)) switchlight(changerGroup,timeSeconds);
				} else {
//		Complement to Rule 6
//		All Signals are RED, but there is no jam on Outlinks anymore. Restore a single Green for the Group with the highest count
					if (activeSignalGroup==null) {
						if (jammedSignalGroup.containsValue(false)){
							SignalGroup changerGroup = null;
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (!jammedSignalGroup.get(group.getId())){
									if (changerGroup==null){
										changerGroup = group;
									} else {
										if (counter.get(group.getId())> counter.get(changerGroup.getId())){
											changerGroup = group;
										}
									}
								}
							}
							switchlight(changerGroup,timeSeconds);
						}				
					} else {
//		Rule 4
//		One Group has GREEN but no cars approaching and another group has RED and Cars approaching (switch them)
						if(approachingVehiclesGroupMap.containsValue(0) && thereAreVehiclesApproaching && activeSignalGroup!=null && approachingVehiclesGroupMap.get(activeSignalGroup.getId())==0) {		
							if(approachingVehiclesGroupMap.get(activeSignalGroup.getId())==0){
								SignalGroup changerGroup = null;
								for (SignalGroup group : this.system.getSignalGroups().values()) {
									if (changerGroup==null && !activeSignalGroup.equals(changerGroup)){
										changerGroup = group;
									} else {
										if (counter.get(group.getId())> counter.get(changerGroup.getId()) && !group.equals(activeSignalGroup) ){
											changerGroup = group;
										}
									}																	
								}
								if (!(changerGroup==null)) switchlight(changerGroup,timeSeconds);	
							}	
						} else {
//		Rule 3
//		A signal should switch from GREEN to RED (according to Rule 1 and 2), but there are just a few cars left to pass: Don't switch
							boolean hasfewerThanMVehiclesInR= false;
							if (rule3Tester(activeSignalGroup, timeSeconds) ){
								hasfewerThanMVehiclesInR = true;
							}
//		Rule 2
//		A signal should switch from GREEN to RED (according to Rule 1), but internal time of the Group hasn't reached
//		the minimal green time yet - Dont't switch
							if (!hasfewerThanMVehiclesInR) {
								boolean minimumGreenTimereached = false;	
								if (timeactivegroup>= gershensonConfig.getMinimumGREENtime()) minimumGreenTimereached = true;

//		Rule 1	- if the the threshold is reached switch that signalgroup to GREEN							
								if (minimumGreenTimereached) {
									SignalGroup changerGroup = null;
									for (SignalGroup group : this.system.getSignalGroups().values()) {										
										if (!group.equals(activeSignalGroup) && counter.get(group.getId())>this.threshold) {
											if (changerGroup==null){
												changerGroup = group;
											} else {
												if (counter.get(group.getId())> counter.get(changerGroup.getId())){
													changerGroup = group;
												}
											}
										}
									}
									if (changerGroup!=null){
										switchlight(changerGroup,timeSeconds);
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
//		nothing to do here as we don't deal with plans
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem ;		
	}	
}