/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
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
package optimize.opdyts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalSystemsManager;

import floetteroed.opdyts.DecisionVariable;

/**
 * Contains the offsets of all signal systems.
 * 
 * @author tthunig
 */
public class OffsetDecisionVariable implements DecisionVariable {

	/* save offsets in the already existing (and for this context, quite complex) container for SignalControlData 
	 * to be more flexible for extensions of opdyts to more than offset optimization */
	private final SignalControlData newOffsets;
    private final SignalsData currentSignalsData;
	
	public OffsetDecisionVariable(SignalControlData newSignalControl, Scenario scenario) {
		currentSignalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        newOffsets = newSignalControl;
	}
	
	public OffsetDecisionVariable(SignalControlData newSignalControl, Scenario scenario, SignalSystemsManager manager) {
		this(newSignalControl, scenario);
	}
	
	public SignalControlData getCurrentSignalControlData() {
		return newOffsets;
	}

	@Override
	public void implementInSimulation() {
		// set possibly changed offsets into the original signalsData (possibly changed by OffsetRandomizer)
		for (SignalSystemControllerData newSystemControl : newOffsets.getSignalSystemControllerDataBySystemId().values()) {
			SignalSystemControllerData originalSystemControl = currentSignalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(newSystemControl.getSignalSystemId());
			for (SignalPlanData newPlanData : newSystemControl.getSignalPlanData().values()) {
				originalSystemControl.getSignalPlanData().get(newPlanData.getId()).setOffset(newPlanData.getOffset());
				
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder();
		for (SignalSystemControllerData signalControllerData : newOffsets.getSignalSystemControllerDataBySystemId().values()) {
			strb.append("system " + signalControllerData.getSignalSystemId());
			for (SignalPlanData planData : signalControllerData.getSignalPlanData().values()) {
				strb.append(", plan " + planData.getId() + ": offset " + planData.getOffset());
			}
			strb.append(".");
		}
		return strb.toString();
	}

}
