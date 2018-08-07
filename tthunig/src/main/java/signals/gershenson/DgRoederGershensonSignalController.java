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
		//This is a fixed value and and doesn't have to be calculated in every iteration
		maximalNumberOfAgentsInDistance = (int)(gershensonConfig.getMonitoredDistance()/this.scenario.getConfig().jdeqSim().getCarSize());
		//TODO Adjust the Threshold according to the proposed formula.
		setThresholdfromCycleTime();
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
				//Ensures that if a Group is switched from GREEN to RED that it is not the active Group anymore
				if(group.equals(activeSignalGroup)) activeSignalGroup=null;
			}
		}	
	}
	

	
	//Method to monitor approaching cars for a signal group
	private void carsOnInLinks(SignalGroup group, double now) {
		if (!approachingVehiclesMap.containsKey(group.getId())) {
			approachingVehiclesMap.put(group.getId(), new HashMap<Id<Link>,Integer>());
		}
		for (Link link : signalGroupIdMetadataMap.get(group.getId()).getInLinks()) {		
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
	
	
	//If a minimum Cycle-Time for Rule 1 should be used, call this method.
	public void setThresholdfromCycleTime(){
		int minCycleTime = gershensonConfig.getMinmumCycleTime();
		double intergreenTime = gershensonConfig.getInterGreenTime();
		int numberOfGroups = signalGroupIdMetadataMap.size();
		
		
		//This ensures that the threshold adjusts if there are more than one in InLink per Group that the Counter is not reached to quickly
		double averageInLinksinGroup = 0.;
		for (SignalGroup signalGroup : this.system.getSignalGroups().values()){
			// count number of lanes of this signal group
//			for (Signal signal : signalGroup.getSignals().values()) {
//				if (signal.getLaneIds().isEmpty()) {
//					averageInLinksinGroup += 1;
//				} else {
//					averageInLinksinGroup += signal.getLaneIds().size();
//				}
//			}
			averageInLinksinGroup += signalGroupIdMetadataMap.get(signalGroup.getId()).getInLinks().size();
		}
		
		if (averageInLinksinGroup != 0.){
			averageInLinksinGroup /= numberOfGroups;
		} else averageInLinksinGroup = 1;
		
		//TODO Explanation
		double adjustedThreshold = averageInLinksinGroup*maximalNumberOfAgentsInDistance*(minCycleTime-intergreenTime*numberOfGroups)/numberOfGroups;
		
		
		double oldThreshold = gershensonConfig.getThreshold();	
		gershensonConfig.setThreshold(adjustedThreshold);
		log.info("Set Threshold from "+ oldThreshold + " to "+ adjustedThreshold +" [veh*s]");
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
				
				//Fix Agents distance here
				if(maximalNumberOfAgentsInDistance < approachingVehiclesMap.get(group.getId()).get(link)){
					vehicles += maximalNumberOfAgentsInDistance;
				} else {
					vehicles += approachingVehiclesMap.get(group.getId()).get(link);
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
					
				if (group.getState() != null){
					if (group.getState().equals((SignalGroupState.RED))){
						counter.put(group.getId(), counter.get(group.getId()) + new Double(approachingVehiclesGroupMap.get(group.getId())));
					} 
				}
			}
		}
	}
	
	private Boolean rule3Tester(Id<SignalGroup> signal, double time) {
		Boolean VehInR = false;
		for (Link link : signalGroupIdMetadataMap.get(signal).getInLinks()) {
			if(!VehInR) {
				int numberVehiclesInShortDistance = sensorManager.getNumberOfCarsInDistance(link.getId(), gershensonConfig.getMonitoredPlatoonTail(), time);
				if(numberVehiclesInShortDistance < gershensonConfig.getLengthOfPlatoonTails() && 
						numberVehiclesInShortDistance > 0 &&
						numberVehiclesInShortDistance - approachingVehiclesMap.get(signal).get(link.getId()) == 0
						) {
					VehInR = true;
					//log.info("There were "+sensorManager.getNumberOfCarsInDistance(link.getId(), gershensonConfig.getMonitoredPlatoonTail(), time) +" Vehicles in "+gershensonConfig.getMonitoredPlatoonTail() +"m in " + signal + " at time " +time + "s on link " + link.getId() );
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
	
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > approachingVehiclesMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Integer>>();
	private Map<Id<SignalGroup>, Integer> approachingVehiclesGroupMap = new HashMap<Id<SignalGroup>, Integer>();
	private Map<Id<SignalGroup>, Map<Id<Link>, Boolean> > jammedOutLinkMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Boolean>>();	
	private Map<Id<SignalGroup>, Double>  timecounter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Double> counter = new HashMap<Id<SignalGroup>, Double>();	
	private Map<Id<SignalGroup>, Boolean> jammedSignalGroup = new HashMap<Id<SignalGroup>,Boolean>();
	private SignalGroup activeSignalGroup = null;
	private int maximalNumberOfAgentsInDistance;

	

	
	
	@Override
	public void updateState(double timeSeconds) {
	// Get data which is needed before going through the rule set	
		
		
//		//This is just to demonstrate that the algorithm works
//		if (timeSeconds%1==0) {
//			//log.info("At "+timeSeconds +" "+ activeSignalGroup.getId() + " is active and is if not null");
//			if (activeSignalGroup!= null){
//				log.info("At "+timeSeconds +" "+ activeSignalGroup.getId().toString() + " is active and is" + activeSignalGroup.getState());
//			} else {
//				log.info("At "+timeSeconds + " active Signalgroup is not initilised");
//			}
//			log.info("At "+timeSeconds +" : "+ counter.toString() + " ");
//			log.info(" ");
//		}
		
		//add one second to the timecounter of eachgroup
		//TODO adjust this so only the activegroup is counted to avoid this loop
		for (SignalGroup group : this.system.getSignalGroups().values()){
			timecounter.merge(group.getId(), 0., (a,b) -> a+1.);
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
			
			if (!jammedSignalGroup.containsValue(false)  || !thereAreVehiclesApproaching) {
				//log.info("Envoke Rule 6 at "+timeSeconds + !vehiclesApproachingSomewhere());
//				log.info("Rule 6");
//				log.info("all jammed: "+ !jammedSignalGroup.containsValue(false));
//				log.info("no cars approaching: "+!thereAreVehiclesApproaching);
				
				
					for (SignalGroup group : this.system.getSignalGroups().values()){ 			
						if (activeSignalGroup != null && activeSignalGroup.equals(group)) switchlight(group,timeSeconds);
					}
					
				
				//if(!vehiclesApproachingSomewhere()) activeSignalGroup=null;
			} else {
		//Rule 5
		//At least one Outlink is jammed - from all groups, which are not jammed turn the one with the highest counter to GREEN
		//the others to RED
		//The switchlightCounter ensures that the activeSignalGroup is set to null if necessary
				if (jammedSignalGroup.containsValue(true) && activeSignalGroup !=null) {
					log.error("Envoke Rule 5 at "+timeSeconds + jammedSignalGroup.toString());
					
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
							if ( !jammedSignalGroup.get(group.getId()) && 
									counter.get(group.getId())==signalGrouphighestCount && 
									counter.get(group.getId())!=0. &&
									group.getState().equals(SignalGroupState.RED) 			
								|| (!activeSignalGroup.equals(group) && group.getState().equals(SignalGroupState.GREEN))){
								
								switchlight(group,timeSeconds);
								switchlightcounter++;
//								log.info("Fehler 2 - Rule 5 " + switchlightcounter);
							}
						}	
					}
			//If only one switch happened, all groups should be red and active group should be null
					if (switchlightcounter==1) activeSignalGroup=null;
					
				} else {
		//Complement to Rule 6
		//All Signals are RED, but there is no jam on Outlinks anymore. Restore a single Green for the Group with the highest count
					if (activeSignalGroup==null) {
						if (jammedSignalGroup.containsValue(false)){
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (signalGrouphighestCount!=0.0 && counter.get(group.getId()) == signalGrouphighestCount) {
									switchlight(group,timeSeconds);
//									log.info("Complement Rule 6");
									break;
								}
							}
						}
					} else {
		//Rule 4
		//One Group has GREEN but no cars approaching and another group has RED and Cars approaching (switch them)
						if(approachingVehiclesGroupMap.containsValue(0) && thereAreVehiclesApproaching) {		
							int highest = 0;
							SignalGroup firstlight=null; 
							for (SignalGroup group : this.system.getSignalGroups().values()) {
								if (activeSignalGroup != null){
									if (group.equals(activeSignalGroup) &&
											(approachingVehiclesGroupMap.get(group.getId())==0 )){
										switchlight(group,timeSeconds);
//										log.info("Rule 4");
									} else {
										if(approachingVehiclesGroupMap.get(activeSignalGroup.getId())==0  &&
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
							if (activeSignalGroup == null) switchlight(firstlight,timeSeconds);
						} else {
		//Rule 3
		//A signal should switch from GREEN to RED (according to Rule 1 and 2), but there are just a few cars left to pass: Don't switch
							boolean hasfewerThanMVehiclesInR= false;
							if (rule3Tester(activeSignalGroup.getId(), timeSeconds) ){
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
										if (!group.equals(activeSignalGroup) && counter.get(group.getId())>gershensonConfig.getThreshold()) {
											potentialRule1.add(group);
//											log.info("Rule 1: Signalgroup "+ group.getId().toString() + " reached the threshold");
										}
									}
									if(!potentialRule1.isEmpty()) {
										SignalGroup groupR1 = null;
										for(SignalGroup group : potentialRule1) {
											if (groupR1==null) groupR1=group;
											if(counter.get(groupR1.getId())<counter.get(group.getId())) groupR1=group;
										}
										
										for (SignalGroup group : this.system.getSignalGroups().values()) {	
											if (group.equals(activeSignalGroup)) switchlight(group,timeSeconds);
											if (group.getState().equals(SignalGroupState.GREEN)) switchlight(group,timeSeconds);
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
