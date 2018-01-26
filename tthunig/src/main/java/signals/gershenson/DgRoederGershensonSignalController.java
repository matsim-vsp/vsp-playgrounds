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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;

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
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.LanesToLinkAssignment;

import com.google.inject.Provider;

import signals.sensor.LinkSensorManager;


/**
 * @author dgrether
 *
 */
public class DgRoederGershensonSignalController implements SignalController {
	
	public final static String IDENTIFIER = "gretherRoederGershensonSignalControl";
	
	public final static class SignalControlProvider implements Provider<SignalController> {
		private final LinkSensorManager sensorManager;
		private final Scenario scenario;

		public SignalControlProvider(LinkSensorManager sensorManager, Scenario scenario) {
			this.sensorManager = sensorManager;
			this.scenario = scenario;
		}

		@Override
		public SignalController get() {
			return new DgRoederGershensonSignalController(scenario, sensorManager);
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

	protected int tGreenMin =  0; // time in seconds
	protected int minCarsTime = 0; //
	protected double storageCapacityOutlinkJam = 0.8;
	protected double maxRedTime = 15.0;
	
	private boolean interim = false;
	private double interimTime;

	protected boolean outLinkJam;
	protected boolean maxRedTimeActive = false;
	protected double compGreenTime;
	protected double approachingRed;
	protected double approachingGreenLink;
	protected double approachingGreenLane;
	protected double carsOnRefLinkTime;
	protected boolean compGroupsGreen;
	protected SignalGroupState oldState;
	
	protected double signalgrouptimecounter = 0.0;
	
	protected double threshold = 13.33;

	private double switchedGreen = 0;

	private LinkSensorManager sensorManager;
	private Scenario scenario;

	private Map<Id<SignalGroup>, SignalGroupMetadata> signalGroupIdMetadataMap;	
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > jammedOutLinksInSignalGroup;
	
	private DgRoederGershensonSignalController(Scenario scenario, LinkSensorManager sensorManager) {
		this.scenario = scenario;
		this.sensorManager = sensorManager;
	}

	private void registerAndInitializeSensorManager() {
		for (SignalGroupMetadata metadata : this.signalGroupIdMetadataMap.values()){
			for (Link outLink : metadata.getOutLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(outLink.getId());
				log.info("Register Outlink "+ outLink.getId().toString());
			}
			
			//TODO initialize inLinks
			for (Link inLink : metadata.getInLinks()){
				this.sensorManager.registerNumberOfCarsMonitoring(inLink.getId());
				log.info("Register Inlink "+ inLink.getId().toString());
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
			if (!outLink.getFromNode().equals(inLink.getToNode())){
				metadata.addOutLink(outLink);
			}
		}
	}
	
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		SignalGroup last = null;
		this.groupStateMap = new HashMap<>();
		for (SignalGroup g : this.system.getSignalGroups().values()){
			GroupState state = new GroupState();
			state.lastDropping = simStartTimeSeconds;
			this.groupStateMap.put(g.getId(), state);
			system.scheduleDropping(simStartTimeSeconds, g.getId());
			last = g;
			signalGroupstatesMap.put(g.getId(), g.getState());
		}
		last.setState(SignalGroupState.GREEN);
		this.switchGroup2Green(simStartTimeSeconds, last);
	}
	
	private void switchGroup2Green(double timeSeconds, SignalGroup g){
		this.greenGroup =  g;			
		this.system.scheduleOnset(timeSeconds, g.getId());
	}
	
	private void switchGroup2Red(double timeSeconds, SignalGroup g){
		this.groupStateMap.get(g.getId()).lastDropping = timeSeconds;
		this.system.scheduleDropping(timeSeconds, g.getId());
	}
	
	
	//Check if there is an an Outlink jam and if yes, map them.
	private boolean hasOutLinkJam(SignalGroup group, SignalGroupMetadata metadata){
		for (Link link : metadata.getOutLinks()){
			double storageCap = (link.getLength() * link.getNumberOfLanes()) / (7.5 * this.scenario.getConfig().qsim().getStorageCapFactor());
			if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) > (storageCap * this.storageCapacityOutlinkJam)){
				return true;
			}
		}
		return false;
	}


	
	
	/**
	 * The signal group for that the next onset will be triggered
	 */
	private SignalGroup greenGroup;
	
	private Map<Id<SignalGroup>, GroupState> groupStateMap;

	private boolean allRed;
	
	private class GroupState {
		double lastDropping;
	}

	
	private SignalGroup checkMaxRedTime(double timeSeconds){
		if (timeSeconds > 15.0){
			log.info("");
		}
		for (SignalGroup group : this.system.getSignalGroups().values()){
			GroupState state = this.groupStateMap.get(group.getId());
			log.error("  Group " + group.getId()  + " state " + group.getState() + " last drop: " + state.lastDropping);
			if (SignalGroupState.RED.equals(group.getState()) 
					&& ((timeSeconds - state.lastDropping) > this.maxRedTime)){
				log.error("  Group " + group.getId() + " red for " + (timeSeconds - state.lastDropping));
				return group;
			}
		}
		return null;
	}
	
	private boolean isSwitching(){
		if (SignalGroupState.GREEN.equals(this.greenGroup.getState())){
			return false;
		}
		return true;
	}
	
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
		//rule 4


		
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

		
	

	

//TODO WorkInProgress----------------------------------------------------Soehnke

	

	public void switchlight(SignalGroup group) {
		if (group.getState() == null) {
			timecounter.put(group.getId(), 0);
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
			group.setState(SignalGroupState.RED);
		} else {
			timecounter.put(group.getId(), 0);
//			approachingRedMap.get(group.getId()).forEach((k,v) -> v=0);
			approachingVehiclesMap.get(group.getId()).forEach((k,v) -> v=0);
		
			if (group.getState().equals(SignalGroupState.RED)){
				group.setState(SignalGroupState.GREEN);
				signalGroupstatesMap.put(group.getId(), SignalGroupState.GREEN);
				log.error("Switched "+ group.getId() + " from RED to GREEN");
			} else {
				group.setState(SignalGroupState.RED);
				signalGroupstatesMap.put(group.getId(), SignalGroupState.RED);
				log.error("Switched "+ group.getId() + " from GREEN to RED");
			}
		}	
	}
	
	public Map<Id<Link>, Boolean> vehiclesStoppedAfterSignal(SignalGroup group, SignalGroupMetadata metadata) {
		Map<Id<Link>, Boolean> vehiclesOnOutLinks = new HashMap<Id<Link>, Boolean>();
		
		for (Link link : metadata.getOutLinks()){
			double storageCap = (link.getLength() * link.getNumberOfLanes()) / (7.5 * this.scenario.getConfig().qsim().getStorageCapFactor());
		
			if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) > (storageCap * this.storageCapacityOutlinkJam)){
				vehiclesOnOutLinks.put(link.getId(), true);
			} else vehiclesOnOutLinks.put(link.getId(), false);
		}
		return vehiclesOnOutLinks;
	}
	
	//Method to monitor approaching cars for a signal group
	private void carsOnInLinks(SignalGroup group) {
		if (!approachingVehiclesMap.containsKey(group.getId())) {
			approachingVehiclesMap.put(group.getId(), new HashMap<Id<Link>,Integer>());
		}
		for (Link link : signalGroupIdMetadataMap.get(group.getId()).getInLinks()) {
			Integer cars = this.sensorManager.getNumberOfCarsOnLink(link.getId());
			approachingVehiclesMap.get(group.getId()).put(link.getId(), cars);
		} 
	}
	//method to monitor cars behind intersection
	private void jamOnOutLink(SignalGroup group) {
		if (!jammedOutLinkMap.containsKey(group.getId())) {
			jammedOutLinkMap.put(group.getId(), new HashMap<Id<Link>,Boolean>());
		}
		for (Link link : signalGroupIdMetadataMap.get(group.getId()).getOutLinks()) {
			double storageCap = (link.getLength() * link.getNumberOfLanes()) / (7.5 * this.scenario.getConfig().qsim().getStorageCapFactor());
			Boolean jammed = false;
			if (this.sensorManager.getNumberOfCarsOnLink(link.getId()) > (storageCap * this.storageCapacityOutlinkJam)){
				jammed = true;
			}
			jammedOutLinkMap.get(group.getId()).put(link.getId(), jammed);
		} 	
	}
	
	private void initSignalStates() {
		for(SignalGroup group : this.system.getSignalGroups().values()) {
			group.setState(SignalGroupState.RED);
		}		
	}
	private boolean jammedafterIntersection(SignalGroup group) {
		boolean jammed = false;
		if (jammedOutLinkMap.get(group.getId()) != null){
			if(jammedOutLinkMap.get(group.getId()).containsValue(true)) jammed = true;
		}
		return jammed;
	}
	
	
	
	
	
	
	
	//-----------------------------

	//-------------------------
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > approachingRedMap;
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > approachingGreenMap;
	
	private Map<Id<SignalGroup>, Map<Id<Link>, Integer> > approachingVehiclesMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Integer>>();
	private Map<Id<SignalGroup>, Map<Id<Link>, Boolean> > jammedOutLinkMap = new HashMap<Id<SignalGroup>, Map<Id<Link>, Boolean>>();
	
	private Map<Id<SignalGroup>, Integer>  timecounter = new HashMap<Id<SignalGroup>, Integer>();	
	private Map<Id<SignalGroup>,SignalGroupState> signalGroupstatesMap = new HashMap<Id<SignalGroup>,SignalGroupState>();

	
	Map<Id<SignalGroup>,Integer> notJammedOutlinks = new HashMap<Id<SignalGroup>,Integer>();
	
	Map<Id<Link>,Boolean> vehiclesStoppedAfterGREEN = new HashMap<Id<Link>,Boolean>();
	Map<Id<Link>,Boolean> vehiclesStoppedAfterRED = new HashMap<Id<Link>,Boolean>();
	Map<Id<SignalGroup>,Map<Id<Link>,Boolean>> vehiclesStoppedAfterGREENGroup = new HashMap<Id<SignalGroup>,Map<Id<Link>,Boolean>>();
	Map<Id<SignalGroup>,Map<Id<Link>,Boolean>> vehiclesStoppedAfterREDGroup = new HashMap<Id<SignalGroup>,Map<Id<Link>,Boolean>>();
	
	Map<Id<SignalGroup>,Boolean> jammedGroupGREEN = new HashMap<Id<SignalGroup>,Boolean>();
	Map<Id<SignalGroup>,Boolean> jammedGroupRED = new HashMap<Id<SignalGroup>,Boolean>();
	
	Map<Id<SignalGroup>,Integer> maximalCountREDGROUP = new HashMap<Id<SignalGroup>,Integer>();
	Map<Id<SignalGroup>,Integer> maximalCountGREENGROUP = new HashMap<Id<SignalGroup>,Integer>();
	
	
	
	int m = 2;
	int minimumGREENtime = 4;
	@Override
	public void updateState(double timeSeconds) {
		//log.info("Signal system: " + this.system.getId() + " at time " + timeSeconds);
		
		if (timeSeconds == 0.0) {
			log.info("init SignalSystem if haven't done yet");
			this.setSignalSystem(this.system);
			initSignalStates();
		}

		
		//log.info("Iterating over all Signalgroups");
		for (SignalGroup group : this.system.getSignalGroups().values()){
			//log.info(group.getId());
			//increase the time counter
			//Init maybe at another place??

			timecounter.merge(group.getId(), 0, (a,b) -> a+1);
			
			maximalCountREDGROUP.put(group.getId(), 0);
			maximalCountGREENGROUP.put(group.getId(), 0);
			//fills approachingVehiclesMap
			carsOnInLinks(group); 
			//fills jammedOutLinkMap
			jamOnOutLink(group);
			
			if (SignalGroupState.RED.equals(group.getState())) {
				signalGroupstatesMap.put(group.getId(), SignalGroupState.RED);
				if (jammedafterIntersection(group)) {
					jammedGroupRED.put(group.getId(), true);
				} else jammedGroupRED.put(group.getId(), false);
				//Map if the belonging OutLink is jammed
//				vehiclesStoppedAfterRED = vehiclesStoppedAfterSignal(group, this.signalGroupIdMetadataMap.get(group.getId()));
//				vehiclesStoppedAfterREDGroup.put(group.getId(), vehiclesStoppedAfterRED);
//				jammedGroupRED.put(group.getId(), vehiclesStoppedAfterRED.containsValue(true));
				//Map<Id<Link>, Integer> approachingRedOnLink = new HashMap<Id<Link>, Integer>();
				//approachingRedMap.put(group.getId(), approachingRedOnLink);
				
				Iterator<Link> it = this.signalGroupIdMetadataMap.get(group.getId()).getInLinks().iterator();
				
				
		//TODO //count the number of incoming agents at a red light (denoted as k_ij)
		//OPTION 1:	Since Gershenson is not thinking in Signal Groups I'm adding up the cars in each group to make them comparable,
		// even though there are more then one InLink per Group. Thus k_ij is the sum of all InLinks of the group.
		//OPTION 2: Or taking the max-Count of all links as equivalent to the k value of the group
				
		//Option1
//				int approachingCarsOption1 = 0;
//				while(it.hasNext()) {
//					
//					approachingCarsOption1 += sensorManager.getNumberOfCarsOnLink(it.next().getId());
//					maximalCountREDGROUP.put(group.getId(), approachingCarsOption1);
//				}
//				log.info("There are " + approachingCarsOption1 + " vehicles approuching the Red light of SignalGroup "+group.getId().toString() );
		//Option2
				int maxValue = 0;
				while(it.hasNext()) {
					int approachingCarsOption2 = approachingVehiclesMap.get(group.getId()).get(it.next().getId());			
					if(approachingCarsOption2 > maxValue) maxValue = approachingCarsOption2;
				
				}
				maximalCountREDGROUP.put(group.getId(), maxValue);
			} else {
				signalGroupstatesMap.put(group.getId(), SignalGroupState.GREEN);
				
//				vehiclesStoppedAfterGREEN = vehiclesStoppedAfterSignal(group, this.signalGroupIdMetadataMap.get(group.getId()));
//				//useless Map??
//				vehiclesStoppedAfterGREENGroup.put(group.getId(), vehiclesStoppedAfterGREEN);
//				jammedGroupGREEN.put(group.getId(), vehiclesStoppedAfterRED.containsValue(true));
				//took here Option 1 might need change

				if (jammedafterIntersection(group)) {
					jammedGroupGREEN.put(group.getId(), true);
				} else jammedGroupGREEN.put(group.getId(), false);
				
				Iterator<Link> it = this.signalGroupIdMetadataMap.get(group.getId()).getInLinks().iterator();
				int approachingCarsOption1 = 0;
				while(it.hasNext()) {			
					approachingCarsOption1 = approachingVehiclesMap.get(group.getId()).get(it.next().getId());
					maximalCountGREENGROUP.put(group.getId(), approachingCarsOption1);
				}
				//log.info("There are " + approachingCarsOption1 + " vehicles approaching the Green light of SignalGroup "+group.getId().toString());
			}
			//log.info("This signal group has now the internal time " + timecounter.get(group.getId()));
		}	
//-----------------End of Preparation--------------
			
		if (jammedGroupGREEN.containsValue(true)) {
//rule 6
			if (jammedGroupRED.containsValue(true)) {
				for (SignalGroup group : this.system.getSignalGroups().values()) {
					if (group.getState().equals(SignalGroupState.GREEN)){
						switchlight(group);
						log.error("Pull rule 6");
					}
				}
//rule 5	
			} else {
				Id<SignalGroup> maxCounter = null;
				Integer maxValue = 0;
				for (SignalGroup group : this.system.getSignalGroups().values()) {
					//TODO fishy
					group.setState(SignalGroupState.RED);
					if (maximalCountREDGROUP.get(group.getId())>maxValue &&
							!jammedGroupRED.containsValue(true)){
						maxValue = maximalCountREDGROUP.get(group.getId());
						maxCounter = group.getId();
					}	
				}
				switchlight(this.system.getSignalGroups().get(maxCounter));
				log.error("Pull rule 5");
			}			
		} else if (!jammedGroupRED.containsValue(true)) {
			Id<SignalGroup> maxCounter = null;
			Integer maxValue = 0;
			
			for (SignalGroup group : this.system.getSignalGroups().values()) {
				if (maxCounter == null) {
					maxValue = maximalCountREDGROUP.get(group.getId());
					maxCounter = group.getId();
				}
				
				
				if (maximalCountREDGROUP.get(group.getId())>maxValue &&
						group.getState().equals(SignalGroupState.RED)) {
					maxValue = maximalCountREDGROUP.get(group.getId());
					maxCounter = group.getId();
				}
			}
//complement to rule 6
			if (signalGroupstatesMap.containsValue(SignalGroupState.GREEN)) {
				switchlight(this.system.getSignalGroups().get(maxCounter));
				//log.error("Pull rule 6");
			}
//Rule 4
			// TODO this is wrong!
			if (!maximalCountREDGROUP.containsValue(0) && maximalCountGREENGROUP.containsValue(0)) {
				switchlight(this.system.getSignalGroups().get(maxCounter));
				for (Id<SignalGroup> id : maximalCountGREENGROUP.keySet()) {
					switchlight(this.system.getSignalGroups().get(id));
					log.error("Pull rule 4");
				}
			} else {
				boolean ruleThree = true;
				for (Integer i : maximalCountGREENGROUP.values()) {
					if (i<=0 || i>=m) ruleThree = false;
				}
//Rule 3				
				if(ruleThree) {
					Integer minGreen = -1;
					Id<SignalGroup> idMinTime;
					for (Id<SignalGroup> id : maximalCountGREENGROUP.keySet()) {
						if (minGreen == -1) {
							minGreen = timecounter.get(id);
							idMinTime = id;
						}
						if (timecounter.get(id)<minGreen) {
							minGreen = timecounter.get(id);
							idMinTime = id;
						}
					}
//Rule 2					
					if (minGreen < minimumGREENtime) {

						for (Id<SignalGroup> groupid : maximalCountREDGROUP.keySet()) {
							if (maximalCountREDGROUP.get(groupid)>maxValue &&
									signalGroupstatesMap.get(groupid).equals(SignalGroupState.RED)) {
								maxValue = maximalCountREDGROUP.get(groupid);
								maxCounter = groupid;
								log.error("Pull rule 2");
							}
						}
//Rule 1						
						if (maximalCountREDGROUP.get(maxCounter) >= 14) {
							switchlight(this.system.getSignalGroups().get(maxCounter));
							log.error("Pull rule 1");
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
		// TODO Auto-generated method stub
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem ;		
	
		/* start initialization process. 
		 * can not be started earlier in the constructor because the signal system ID is needed. theresa, jan'17 */
		initSignalGroupMetadata();
		registerAndInitializeSensorManager();
	}	

	

}
