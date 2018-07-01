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
 * default values:
 * interGreenTime=0
 * (monitoredDistance) d=50m   
 * (monitoredPlatoonTail) r=25m 
 * (minimumGREENtime) t_min = 4s 
 * (lengthOfPlatoonTails) m= 2 veh 
 * (threshold) n= 13.33 veh*s   (sbraun Feb.18)
 * 
 * @author dgrether, sbraun, droeder
 *
 */
public class DgRoederGershensonSignalController implements SignalController {

	
	public final static String IDENTIFIER = "gretherRoederGershensonSignalControl";
	
	
	
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
			return new DgRoederGershensonSignalController(scenario, sensorManager, gershensonConfig);
		}
	}
	
	private class SignalGroupMetadata {

		private Set<Link> inLinks = new HashSet<Link>();
		private Set<Link> outLinks = new HashSet<Link>();
		
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
		
	}

	private static final Logger log = Logger.getLogger(DgRoederGershensonSignalController.class);
	
	private SignalSystem system;

	
	//TODO fuer Setter in Config-Datei
	//protected int tGreenMin =  0; // time in seconds
	//protected int minCarsTime = 0; //
	
	//protected double storageCapacityOutlinkJam = 0.8;

	//protected double interGreenTime = 0.; //TODO default should be at least 2
	//protected double threshold = 13.33; //TODO should be higher
	
	//protected int lengthOfPlatoonTails = 2;
	//protected int minimumGREENtime = 4;  //TODO should be higher maybe 20
	//protected double monitoredPlatoonTail = 25.;
	//protected double monitoredDistance = 50.;
	//protected double minmumDistanceBehindIntersection =10.;
	
	//protected boolean outLinkJam;
	//protected boolean maxRedTimeActive = false;
	//protected double compGreenTime;
	//protected double approachingRed;
	//protected double approachingGreenLink;
	//protected double approachingGreenLane;
	//protected double carsOnRefLinkTime;
	//protected boolean compGroupsGreen;
	//protected SignalGroupState oldState;
	
	//protected double signalgrouptimecounter = 0.0;
	private LinkSensorManager sensorManager;
	private Scenario scenario;
	private GershensonConfig gershensonConfig;
	
	private Map<Id<SignalGroup>, SignalGroupMetadata> signalGroupIdMetadataMap;	
	
	private DgRoederGershensonSignalController(Scenario scenario, LinkSensorManager sensorManager, GershensonConfig gershensonConfig) {
		this.scenario = scenario;
		this.sensorManager = sensorManager;
		this.gershensonConfig = gershensonConfig;
	}

	private void registerAndInitializeSensorManager() {
		for (SignalGroupMetadata metadata : this.signalGroupIdMetadataMap.values()){
			// initialize three sensors per signal group: one at the out-link, two sensors at the in-link with different distances
			for (Link outLink : metadata.getOutLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(outLink.getId());
				log.info("Register Outlink "+ outLink.getId().toString());
			}
			
			for (Link inLink : metadata.getInLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(inLink.getId());
				log.info("Register Inlink "+ inLink.getId().toString());
				this.sensorManager.registerNumberOfCarsInDistanceMonitoring(inLink.getId(), gershensonConfig.getMonitoredPlatoonTail());
				this.sensorManager.registerNumberOfCarsInDistanceMonitoring(inLink.getId(), gershensonConfig.getMonitoredDistance());
			}
		}	
	}

	private void initSignalGroupMetadata(){
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.signalGroupIdMetadataMap = new HashMap<>();
		SignalSystemData systemData = signalsData.getSignalSystemsData().getSignalSystemData().get(this.system.getId());
		
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			if (!this.signalGroupIdMetadataMap.containsKey(signalGroup.getId())){
				this.signalGroupIdMetadataMap.put(signalGroup.getId(), new SignalGroupMetadata());
			}
			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(signalGroup.getId());
			for (Signal signal : signalGroup.getSignals().values()){
				//inlinks
				Link inLink = scenario.getNetwork().getLinks().get(signal.getLinkId());
				metadata.addInLink(inLink);
				//outlinks
				SignalData signalData = systemData.getSignalData().get(signal.getId());
				if (signalData.getTurningMoveRestrictions() == null || signalData.getTurningMoveRestrictions().isEmpty()){
					if (signalData.getLaneIds() == null || signalData.getLaneIds().isEmpty()){
						this.addOutLinksWithoutBackLinkToMetadata(inLink, metadata);
					}
					else { // there are lanes
						LanesToLinkAssignment lanes4link = scenario.getLanes().getLanesToLinkAssignments().get(signalData.getLinkId());
						for (Id<Lane> laneId : signalData.getLaneIds()){
							Lane lane = lanes4link.getLanes().get(laneId);
							if (lane.getToLinkIds() == null || lane.getToLinkIds().isEmpty()){
								this.addOutLinksWithoutBackLinkToMetadata(inLink, metadata);
							}
							else{
								for (Id<Link> toLinkId : lane.getToLinkIds()){
									Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
									if (!toLink.getFromNode().equals(inLink.getToNode())){
										metadata.addOutLink(toLink);
									}
								}
							}
						}
					}
				}
				else {  // turning move restrictions exist
					for (Id<Link> linkid : signalData.getTurningMoveRestrictions()){
						Link outLink = scenario.getNetwork().getLinks().get(linkid);
						metadata.addOutLink(outLink);
					}
				}
			}
		}
	}
	private void addOutLinksWithoutBackLinkToMetadata(Link inLink, SignalGroupMetadata metadata){
		for (Link outLink : inLink.getToNode().getOutLinks().values()){
			if (!outLink.getToNode().equals(inLink.getToNode())){
				metadata.addOutLink(outLink);
			}
		}
	}
	
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		initSignalGroupMetadata();
		initSignalStates(simStartTimeSeconds);
		registerAndInitializeSensorManager();
	}
		
	/**
	 * The signal group for that the next onset will be triggered
	 */
	
	
	private void switchlight(SignalGroup group, double timeSeconds) {
		if (group.getState() == null) {
			timecounter.put(group.getId(), 0.);
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
			this.system.scheduleDropping(timeSeconds, group.getId());
		} else {
			
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
		
			if (group.getState().equals(SignalGroupState.RED)){
				this.system.scheduleOnset(timeSeconds + gershensonConfig.getInterGreenTime(), group.getId());
				timecounter.put(group.getId(), - gershensonConfig.getInterGreenTime());
				activeSignalGroup = group;
			} else {
				this.system.scheduleDropping(timeSeconds, group.getId());
				timecounter.put(group.getId(), 0.);
			}
		}	
	}
	

	
	//Method to monitor approaching cars for a signal group
	private void carsOnInLinks(SignalGroup group, double now) {
		if (!approachingVehiclesMap.containsKey(group.getId())) {
			approachingVehiclesMap.put(group.getId(), new HashMap<Id<Link>,Integer>());
		}
		for (Link link : signalGroupIdMetadataMap.get(group.getId()).getInLinks()) {
			//Integer cars = this.sensorManager.getNumberOfCarsOnLink(link.getId());
			Integer cars = this.sensorManager.getNumberOfCarsInDistance(link.getId(), gershensonConfig.getMonitoredDistance(), now); 
			approachingVehiclesMap.get(group.getId()).put(link.getId(), cars);
		} 
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
			for(Id<Link> link : approachingVehiclesMap.get(group.getId()).keySet()){
				vehicles += approachingVehiclesMap.get(group.getId()).get(link);
			}
		approachingVehiclesGroupMap.put(group.getId(), vehicles);
		}
	}
	
	private void updatecounter(){
		counter.clear();		
		for(SignalGroup group : this.system.getSignalGroups().values()) {
			if (timecounter.containsKey(group.getId()) && approachingVehiclesGroupMap.containsKey(group.getId())){
					
					counter.put(group.getId(), timecounter.get(group.getId())*approachingVehiclesGroupMap.get(group.getId()));
				
			}
		}

	}
	
	private Boolean hasMoreThanMVehiclesInR(Id<SignalGroup> signal, double time) {
		Boolean VehInR = false;
		for (Link link : signalGroupIdMetadataMap.get(signal).getInLinks()) {
			if(!VehInR) {
				if(sensorManager.getNumberOfCarsInDistance(link.getId(), gershensonConfig.getMonitoredPlatoonTail(), time) < gershensonConfig.getLengthOfPlatoonTails() && 
						sensorManager.getNumberOfCarsInDistance(link.getId(), gershensonConfig.getMonitoredPlatoonTail(), time) > 0) {
					VehInR = true;
					//log.info("There were Vehicles in R in " + signal + " at time " +time + "s on link " + link.getId() );
				}
			}
		}
		return VehInR;
	}

	private Id<SignalGroup> highestcount(){
		Id<SignalGroup> highest = null;
		for (Id<SignalGroup>signalgroup : counter.keySet()) {
			if(!jammedafterIntersection(this.system.getSignalGroups().get(signalgroup))) {
				if (highest==null) highest = signalgroup;
				if(counter.get(highest)<counter.get(signalgroup)) highest = signalgroup;
			}
		}
		//if (highest == null) log.error("There are no Signalgroups in the, which are not jammed. Rule 6 should have triggerd before");
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
	
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > approachingVehiclesMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Integer>>();
	private Map<Id<SignalGroup>, Integer> approachingVehiclesGroupMap = new HashMap<Id<SignalGroup>, Integer>();
	private Map<Id<SignalGroup>, Map<Id<Link>, Boolean> > jammedOutLinkMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Boolean>>();	
	private Map<Id<SignalGroup>, Double>  timecounter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Double> counter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Boolean> jammedSignalGroup = new HashMap<Id<SignalGroup>,Boolean>();
	private SignalGroup activeSignalGroup = null;
	
	

	
	@Override
	public void updateState(double timeSeconds) {
	// Get data which is needed before going through the rule set	
		
		
//		//This is just to demonstrate that the algorithm works
//		if (timeSeconds%1==0) {
//			log.info("States at "+timeSeconds +" "+ signalGroupstatesMap.toString() + approachingVehiclesGroupMap.toString());
//		}
		
		for (SignalGroup group : this.system.getSignalGroups().values()){
			timecounter.merge(group.getId(), 0., (a,b) -> a+1.);
		}
		
		if( activeSignalGroup ==null|| activeSignalGroup.getState().equals(SignalGroupState.GREEN)) {
			
			
			jammedSignalGroup.clear();
			jammedOutLinkMap.clear();
			approachingVehiclesMap.clear();
			
			
			
			for (SignalGroup group : this.system.getSignalGroups().values()){
				//timecounter.merge(group.getId(), 0., (a,b) -> a+1.);
				//fills approachingVehiclesMap
				carsOnInLinks(group,timeSeconds);
				//fills jammedOutLinkMap
				jamOnOutLink(group);
				
				jammedSignalGroup.put(group.getId(), jammedafterIntersection(group));
			}
			
			//log.error("At "+ timeSeconds + " are the following groups jammed: "+ jammedSignalGroup.toString());
			//fills approachingVehiclesGroupMap and counter (product of internal times with vehicles)
			carsApproachInGroup();
			updatecounter();
			//Find the group with the highest count (product of internal time with vehicles in D s.t. no OutLink-Jam)
			Id<SignalGroup> signalGrouphighestCount = highestcount();
	
	//-----------------End of Preparation--------------
	
		//Rule 6
		//if the whole intersection is jammed OR
		//no Cars approaching the SignalSystem.
		//	-> turn all signals to RED	
			if (!jammedSignalGroup.containsValue(false)  || !vehiclesApproachingSomewhere()) {
				//log.info("Envoke Rule 6 at "+timeSeconds + !vehiclesApproachingSomewhere());
				for (SignalGroup group : this.system.getSignalGroups().values()){ 
					if (!SignalGroupState.RED.equals(group.getState())) switchlight(group,timeSeconds);
				}
			} else {
		//Rule 5
		//At least one Outlink is jammed - from all groups, which are not jammed turn the one with the highest counter to GREEN
		//the others to RED
				if (jammedSignalGroup.containsValue(true) && activeSignalGroup !=null) {
					//log.error("Envoke Rule 5 at "+timeSeconds + jammedSignalGroup.toString());
					for (SignalGroup group : this.system.getSignalGroups().values()) {
						if (jammedSignalGroup.get(group.getId()) && activeSignalGroup.equals(group))
								/*signalGroupstatesMap.get(group.getId()).equals(SignalGroupState.GREEN))*/ {
							switchlight(group,timeSeconds);
						} else {
							if ((counter.get(group.getId())==counter.get(signalGrouphighestCount)) ||
									(counter.get(group.getId())!=counter.get(signalGrouphighestCount)  && activeSignalGroup.equals(group)))
									/*signalGroupstatesMap.get(group.getId()).equals(SignalGroupState.GREEN)))*/ {
								
								switchlight(group,timeSeconds);
							}
						}	
					}
				} else {
		//Complement to Rule 6
		//All Signals are RED, but there is no jam on Outlinks anymore. Restore a single Green for the Group with the highest count
					if (activeSignalGroup==null) {
						for (SignalGroup group : this.system.getSignalGroups().values()) {
							if (counter.get(signalGrouphighestCount)!=0.0 && group.getId().equals(signalGrouphighestCount)) {
								switchlight(group,timeSeconds);
							}
						}
					} else {
		//Rule 4
		//One Group has GREEN but no cars approaching and another group has RED and Cars approaching (switch them)
						if(approachingVehiclesGroupMap.containsValue(0) && vehiclesApproachingSomewhere()) {
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (group.getState().equals(SignalGroupState.GREEN) &&
										(approachingVehiclesGroupMap.get(group.getId())==0 || !highestcount().equals(group.getId()))){
									switchlight(group,timeSeconds);
								}
								if(highestcount().equals(group.getId()) && group.getState().equals(SignalGroupState.RED)) {
									switchlight(group,timeSeconds);
								}
								
							}
						} else {
		//Rule 3
		//A signal should switch from GREEN to RED (according to Rule 1 and 2), but there are just a few cars left to pass: Don't switch
							int counterRule3 = 0;
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (hasMoreThanMVehiclesInR(group.getId(), timeSeconds)) counterRule3++;
							}
							if (counterRule3==0) {
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
										if (group.getState().equals(SignalGroupState.RED) && counter.get(group.getId())>gershensonConfig.getThreshold()) {
											potentialRule1.add(group);
										}
									}
									if(!potentialRule1.isEmpty()) {
										SignalGroup groupR1 = null;
										for(SignalGroup group : potentialRule1) {
											if (groupR1==null) groupR1=group;
											if(counter.get(groupR1.getId())<counter.get(group.getId())) groupR1=group;
										}
										
										//log.info("Envoke Rule 1. Rule 2,3,4 are not violated. Switch " + groupR1.getId());
										for (SignalGroup group : this.system.getSignalGroups().values()) {	
											if (group.getState().equals(SignalGroupState.GREEN)) switchlight(group,timeSeconds);
											if (group.equals(activeSignalGroup)) switchlight(group,timeSeconds);
											if (group.equals(groupR1)) switchlight(group,timeSeconds);
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
//	// old code from daniel and dominik
//	@Override
//	public void updateState(double timeSeconds) {
//		log.error("Signal system: " + this.system.getId() + " at time " + timeSeconds);
//		//if the groups are switching do nothing
//		if (this.isSwitching()){
//			log.error("  Group: " + this.greenGroup.getId() + " is switching with state: " + this.greenGroup.getState());
//			return;
//		}
//		log.error("  is not switching...");
//		SignalGroup newGreenGroup = null;
//		//rule 7 
//		SignalGroup maxRedViolatingGroup = this.checkMaxRedTime(timeSeconds);
//		if (maxRedViolatingGroup != null){
//			newGreenGroup = maxRedViolatingGroup;
//			log.error("new green group is : " + newGreenGroup.getId());
//		}
//		if (newGreenGroup != null){
//			this.switchGroup2Red(timeSeconds, this.greenGroup);
//			this.switchGroup2Green(timeSeconds, newGreenGroup);
//		}
//		//rule 6
//		Set<SignalGroup> notOutLinkJamGroups = new HashSet<SignalGroup>();
//		for (SignalGroup group : this.system.getSignalGroups().values()){
//			if (!this.hasOutLinkJam(group, this.signalGroupIdMetadataMap.get(group.getId()))){
//				notOutLinkJamGroups.add(group);
//			}
//		}
//		if (this.greenGroup != null){
//			if (!notOutLinkJamGroups.contains(this.greenGroup)){
//				notOutLinkJamGroups.remove(this.greenGroup);
//				this.switchGroup2Red(timeSeconds, this.greenGroup);
//				this.greenGroup = null;
//			}
//		}
//		if (this.greenGroup == null && !notOutLinkJamGroups.isEmpty()) { //we have an all red but one has no jam
//			this.switchGroup2Green(timeSeconds, notOutLinkJamGroups.iterator().next());
//		}
//	}
//		//rule 4
//		
//		for (SignalGroup group : this.system.getSignalGroups().values()){
//			SignalGroupMetadata metadata = this.signalGroupIdMetadataMap.get(group.getId());
//			//rule 6
//			if (group.getState().equals(SignalGroupState.GREEN) && this.hasOutLinkJam(group, metadata)){ 
//				//TODO schedule dropping
//				continue;
//			}
//			
//			else {
//				//if all red switch to green
//			
//				//rule 4
//				if (!approachingGreenInD() && approachingRedInD){
//						//switch something
//				}
//				//rule 3
//				if (group.getState().equals(SignalGroupState.GREEN) && hasMoreThanMVehiclesInR(group)){
//					//don't switch
//				}
//				//rule 2
//				if (group.getState().equals(SignalGroupState.GREEN) && greenTime(group) < minGreenTime){
//					//don't switch
//				}
//				//rule 1
//				if (group.getState().equals(SignalGroupState.RED) && numberCarsInD * timeRed > n_min){
//					//switch to green
//				}
//			}
//		}
//	}
			
			
	@Override
	public void addPlan(SignalPlan plan) {
		//nothing to do here as we don't deal with plans
	}

	@Override
	public void reset(Integer iterationNumber) {
		// TODO call initialize method or similar
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem ;		
	}	

	

}
