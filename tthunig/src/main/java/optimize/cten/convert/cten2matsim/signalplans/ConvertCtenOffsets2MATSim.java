/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Solution2MatsimConverter
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
package optimize.cten.convert.cten2matsim.signalplans;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalSystem;

import optimize.cten.convert.cten2matsim.signalplans.data.FixCrossingSolution;
import optimize.cten.data.DgProgram;
import optimize.cten.ids.DgIdConverter;
import optimize.cten.ids.DgIdPool;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class ConvertCtenOffsets2MATSim {
	
	private static final Logger log = Logger.getLogger(ConvertCtenOffsets2MATSim.class);
	private DgIdConverter idConverter;
	private int scale = 1;
	
	
	public ConvertCtenOffsets2MATSim(DgIdPool idPool){
		this.idConverter = new DgIdConverter(idPool);
	}
	
	/**
	 * overwrite all offsets in signalControl with the offsets from solutionCrossings.
	 * since solutionCrossings only contains crossings with nonzero offsets, 
	 * all offsets are reset to zero first.
	 * 
	 * @param signalControl
	 * @param solutionCrossings
	 */
	public void convertSolution(SignalControlData signalControl, List<FixCrossingSolution> solutionCrossings){
		
		// reset all offsets to zero (solutions are only specified for nonzero offsets)
		for (SignalSystemControllerData controllerData : signalControl.getSignalSystemControllerDataBySystemId().values()){
			SignalPlanData plan = controllerData.getSignalPlanData().values().iterator().next();
			plan.setOffset(0);
		}
		
		// overwrite zero offsets with the ones from solutionCrossings
		for (FixCrossingSolution solution : solutionCrossings) {
//			if (! solution.getProgramIdOffsetMap().containsKey(M2KS2010NetworkConverter.DEFAULT_PROGRAM_ID)) {
				Id<DgProgram> programId = solution.getProgramId();
				Id<SignalSystem> signalSystemId = this.idConverter.convertProgramId2SignalSystemId(programId);
				if (! signalControl.getSignalSystemControllerDataBySystemId().containsKey(signalSystemId)) {
					throw new IllegalStateException("something's wrong with program id " + programId 
							+ " = signal system id " + signalSystemId);
				}
				SignalSystemControllerData controllerData = signalControl.
						getSignalSystemControllerDataBySystemId().get(signalSystemId);
				if (! (controllerData.getSignalPlanData().size() == 1)) {
					throw new IllegalStateException("something's wrong");
				}
				SignalPlanData plan = controllerData.getSignalPlanData().values().iterator().next();
				int offset = solution.getOffset();
				offset = offset * scale;
				plan.setOffset(offset);
				log.info("SignalSystem Id " + controllerData.getSignalSystemId() + " Offset: " + offset);
//			}
		}
	}


	public void setScale(int i) {
		log.warn("Setting scale to " + Integer.toString(i) + " this might not be intented. ");
		this.scale = i;
	}

	
	
}
