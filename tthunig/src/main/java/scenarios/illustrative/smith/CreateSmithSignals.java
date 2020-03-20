/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.smith;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaPreprocessData;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.Lane;

import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for Smith' scenario.
 *
 * @author tthunig
 */
final class CreateSmithSignals {

	private static final Logger log = Logger.getLogger(CreateSmithSignals.class);

    private static final int CYCLE_TIME = 60;
    private static final int INTERGREEN_TIME = 3;
    
    private SignalControlType controlType = SignalControlType.FIXED_EQUAL;
    public enum SignalControlType {
    		FIXED_ALL_RIGHT,
    		FIXED_EQUAL,
    		SYLVIA,
    		LAEMMER_WITH_GROUPS,
    		LAEMMER_FLEX
    }

    private Scenario scenario;
    
    public CreateSmithSignals(Scenario scenario) {
        this.scenario = scenario;
    }

    public void createSignals(SignalControlType signalControl) {
        log.info("Create signals ...");
        this.controlType = signalControl;

        createSignalSystems();
        createSignalGroups();
        createSignalControlData();
    }

	private void createSignalSystems() {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory fac = signalSystems.getFactory(); 

		// create signal system at node 5
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem5", SignalSystem.class));
		signalSystems.addSignalSystemData(signalSystem);

		// create two signals
		SignalData signal = fac.createSignalData(Id.create("signal3.7", Signal.class));
		signal.setLinkId(Id.createLinkId("3_5"));
		signal.addLaneId(Id.create("3_5", Lane.class));
		signalSystem.addSignalData(signal);

		signal = fac.createSignalData(Id.create("signal4.6", Signal.class));
		signal.setLinkId(Id.createLinkId("4_5"));
		signal.addLaneId(Id.create("4_5", Lane.class));
		signalSystem.addSignalData(signal);
    }

    private void createSignalGroups() {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

        // create single groups for both signals
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
    }

    private void createSignalControlData() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        SignalControlDataFactory fac = signalControl.getFactory();
        
		// create a temporary, empty signal control object needed in case sylvia is used
		SignalControlData tmpSignalControl = new SignalControlDataImpl();

		// create a signal control for signal system 5
		SignalSystemData signalSystem = signalSystems.getSignalSystemData()
				.get(Id.create("signalSystem5", SignalSystem.class));

		SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());
		// add the signalSystemControl to the (final or temporary) signalControl
		if (controlType.equals(SignalControlType.SYLVIA)) {
			tmpSignalControl.addSignalSystemControllerData(signalSystemControl);
		} else {
			signalControl.addSignalSystemControllerData(signalSystemControl);
		}

		switch (controlType) {
		case FIXED_EQUAL:
			SignalPlanData signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
			changeToEqualFixedSignalControl(fac, signalPlan);
			break;
		case FIXED_ALL_RIGHT:
			signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
			changeToAllRightFixedSignalControl(fac, signalPlan);
			break;
		case SYLVIA:
			signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
			changeToEqualFixedSignalControl(fac, signalPlan);
			// convert basis fixed time plan to sylvia plan
			SylviaPreprocessData.convertSignalControlData(tmpSignalControl, signalControl);
			break;
		case LAEMMER_WITH_GROUPS:
			signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
			break;
		case LAEMMER_FLEX:
			signalSystemControl.setControllerIdentifier(FullyAdaptiveLaemmerSignalController.IDENTIFIER);
			// TODO create conflicts
			throw new UnsupportedOperationException("not yet implemented");
			// break;
		}
    }

	private SignalPlanData createBasisFixedTimePlan(SignalControlDataFactory fac,
			SignalSystemControllerData signalSystemControl) {
		
		signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		// create a default plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
		signalSystemControl.addSignalPlanData(signalPlan);
		return signalPlan;
	}

	private void changeToAllRightFixedSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan) {
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, Id.create("signal3.7", SignalGroup.class), CYCLE_TIME - 2, CYCLE_TIME - 1));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, Id.create("signal4.6", SignalGroup.class), 0, CYCLE_TIME - INTERGREEN_TIME));
		signalPlan.setOffset(0);		
	}
	
	private void changeToEqualFixedSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan) {
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, Id.create("signal3.7", SignalGroup.class), CYCLE_TIME / 2, CYCLE_TIME - INTERGREEN_TIME));
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, Id.create("signal4.6", SignalGroup.class), 0, CYCLE_TIME / 2 - INTERGREEN_TIME));
		signalPlan.setOffset(0);		
	}

    public void writeSignalFiles(String directory) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(directory + "signalSystems.xml");
        new SignalControlWriter20(signalsData.getSignalControlData()).write(directory + "signalControl.xml");
        new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(directory + "signalGroups.xml");
    }

}
