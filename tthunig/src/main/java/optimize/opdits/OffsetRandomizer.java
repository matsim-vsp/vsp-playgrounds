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
package optimize.opdits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;

import floetteroed.opdyts.DecisionVariableRandomizer;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Axially vary the offset decision variables.
 * Fix the offset of one system, vary all others in positive and negative direction based on the step size given in the opdyts config group.
 * 
 * @author tthunig
 */
public class OffsetRandomizer implements DecisionVariableRandomizer<OffsetDecisionVariable> {

	private static final Logger LOG = Logger.getLogger(OffsetRandomizer.class);

	private final Scenario scenario;
	private final Id<SignalSystem> fixedSystemId;
    private final OpdytsConfigGroup opdytsConfigGroup;

    private final Random random = MatsimRandom.getRandom();
	
	public OffsetRandomizer(Scenario scenario) {
		this.scenario = scenario;
		this.fixedSystemId = ((SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData().getSignalSystemData().keySet().iterator().next();
        this.opdytsConfigGroup = (OpdytsConfigGroup) scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME);
        if (opdytsConfigGroup.getDecisionVariableStepSize() < 1) {
        		throw new RuntimeException("for offset optimization we need an variation size of at least 1 second (otherwise nothing is changed by opdyts)");
        }
	}

	@Override
	public Collection<OffsetDecisionVariable> newRandomVariations(OffsetDecisionVariable decisionVariable) {
		List<OffsetDecisionVariable> result = new ArrayList<>();
		
		SignalControlData oldOffsets = decisionVariable.getCurrentSignalControlData();
		// iterate over all signal systems and vary offsets axially (except the first system, which is kept fixed)
		for (Id<SignalSystem> systemId : oldOffsets.getSignalSystemControllerDataBySystemId().keySet()) {
			if (systemId.equals(fixedSystemId)) {
//			if (!systemId.equals(Id.create("SignalSystem4", SignalSystem.class))) {
				continue;
			}
			int delta = random.nextInt((int)opdytsConfigGroup.getDecisionVariableStepSize()-1)+1;
			{
				SignalControlData newOffsets = SignalUtils.copySignalControlData(oldOffsets);
				for (SignalPlanData plan : newOffsets.getSignalSystemControllerDataBySystemId().get(systemId).getSignalPlanData().values()) {
					plan.setOffset(plan.getOffset() + delta);
				}
				OffsetDecisionVariable variation = new OffsetDecisionVariable(newOffsets, scenario);
				result.add(variation);
			}
			{
				SignalControlData newOffsets = SignalUtils.copySignalControlData(oldOffsets);
				for (SignalPlanData plan : newOffsets.getSignalSystemControllerDataBySystemId().get(systemId).getSignalPlanData().values()) {
					plan.setOffset(plan.getOffset() - delta);
				}
				OffsetDecisionVariable variation = new OffsetDecisionVariable(newOffsets, scenario);
				result.add(variation);
			}
		}
		LOG.warn("input decision variable:");
        LOG.warn(decisionVariable.toString());

        LOG.warn("giving the following new decision variables to opdyts:");
        for (OffsetDecisionVariable var : result) {
            LOG.warn(var.toString());
        }
		
		return result;
	}

}
