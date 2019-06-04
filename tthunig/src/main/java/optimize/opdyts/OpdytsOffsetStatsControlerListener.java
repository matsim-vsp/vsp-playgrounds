/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package optimize.opdyts;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * @author tthunig copied from amit
 */
public class OpdytsOffsetStatsControlerListener implements StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

    @Inject
    private Scenario scenario;

    public static final String OPDYTS_STATS_LABEL_STARTER = "iterationNr";
    public static final String OPDYTS_STATS_FILE_NAME = "opdyts_offsetStats";

    private BufferedWriter writer;

    private double objFunValue = 0.; // useful only to check if all decision variables start at the same point.

    public OpdytsOffsetStatsControlerListener() {
		throw new RuntimeException("add dependency to agarwalamit (and enable outcommented code) to use opdyts here");
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        StringBuilder stringBuilder = new StringBuilder(OPDYTS_STATS_LABEL_STARTER + "\t");
        stringBuilder.append("fromStateObjFunValue"+"\t");
        stringBuilder.append("objectiveFunctionValue"+"\t");
        SignalsData data = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        for (SignalSystemControllerData systemControl :data.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
			for (SignalPlanData plan : systemControl.getSignalPlanData().values()) {
				stringBuilder.append("offsetSignalSystem" + systemControl.getSignalSystemId() + "plan" + plan.getId() + "\t");
			}
		}

        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/"+OPDYTS_STATS_FILE_NAME+".txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write(stringBuilder.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
//		final int iteration = event.getIteration();
//		ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(scenario.getPopulation());
//		modalShareFromPlans.run();
//		double objectiveFunctionValue = getValueOfObjFun();
//		try {
//			// write offsets
//			StringBuilder stringBuilder = new StringBuilder(iteration + "\t");
//			stringBuilder.append(objectiveFunctionValue + "\t");
//			stringBuilder.append(String.valueOf(objFunValue) + "\t"); // useful only to check if all decision variables
//																		// start at the same point.
//			SignalsData data = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
//			for (SignalSystemControllerData systemControl : data.getSignalControlData()
//					.getSignalSystemControllerDataBySystemId().values()) {
//				for (SignalPlanData plan : systemControl.getSignalPlanData().values()) {
//					stringBuilder.append(plan.getOffset() + "\t");
//				}
//			}
//
//			writer.write(stringBuilder.toString());
//			writer.newLine();
//			writer.flush();
//		} catch (IOException e) {
//			throw new RuntimeException("File not found.");
//		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// get value of objective function in advance at this point to check if all
		// decision variables start at the same point.
		// the state changes during re-planning
		objFunValue = getValueOfObjFun();
	}


    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
//		String inputFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "stateVector_networkModes.txt");
//		if (!new File(inputFile).exists()) {
//			return;
//		}
//		String outputFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "stateVector_networkModes.png");
//		StateVectorElementsSizePlotter.gnuHistogramLogScalePlot(inputFile, outputFile, "offsetStats");
	}

    private double getValueOfObjFun (){
        // evaluate objective function value as in TravelTimeObjectiveFunction
		objFunValue = 0;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			if (p.getSelectedPlan().getScore() != null) {
				objFunValue -= p.getSelectedPlan().getScore();
			} 
		}
		return objFunValue;
    }
}
