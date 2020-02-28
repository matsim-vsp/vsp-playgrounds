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
package analysis.signals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.LinkSensorManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import analysis.GnuplotUtils;

/**
 * @author tthunig
 */
@Singleton
public class TtQueueLengthAnalysisTool implements MobsimBeforeSimStepListener, MobsimInitializedListener, IterationEndsListener, IterationStartsListener {
	
	private static final Logger LOG = Logger.getLogger(TtQueueLengthAnalysisTool.class);
	
	private final SignalsData signals;
	private final LinkSensorManager sensorManager;
	private final int noSystems;
	private final Lanes lanes;
	
	private Map<Id<Signal>, Double> totalWaitingTimePerSignal = new HashMap<>();
	private Map<Id<SignalSystem>, Double> totalWaitingTimePerSystem = new HashMap<>();
	
	private final int lastIteration;
	private boolean analyzeThisIt = false;
	
	private String itersDir;
	
	private PrintStream queueLengthOverTime;
	private Map<Id<SignalSystem>, PrintStream> queueLengthPerSignalOverTime = new HashMap<>();
	
	
	@Inject
	public TtQueueLengthAnalysisTool(Scenario scenario, LinkSensorManager sensorManager) {
		this.sensorManager = sensorManager;
		this.signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.noSystems = signals.getSignalSystemsData().getSignalSystemData().keySet().size();
		this.lanes = scenario.getLanes();
		
		this.lastIteration = scenario.getConfig().controler().getLastIteration();
		this.itersDir = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/";
	}
	
	/* initialize sensor manager and fields. can not be done earlier because the sensor manager resets all sensors before mobsim starts */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (SignalSystemData system : signals.getSignalSystemsData().getSignalSystemData().values()) {
			totalWaitingTimePerSystem.put(system.getId(), 0.);
			for (SignalData signal : system.getSignalData().values()) {
				totalWaitingTimePerSignal.put(signal.getId(), 0.);
	
				if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
					for (Id<Lane> laneId : signal.getLaneIds()) {
						this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
					}
				}
				// always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?)
				this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
			}
		}
	}

	// writes the values at the beginning of the time step. afterwards the agents may move further
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		if (analyzeThisIt) {
			// write a line for each time step of the last iteration in all queue length files
			StringBuffer queueLengthLine = new StringBuffer("" + event.getSimulationTime());
			long totalQueueLength = 0;
			for (SignalSystemData system : signals.getSignalSystemsData().getSignalSystemData().values()) {
				StringBuffer queueLengthPerSignalLine = new StringBuffer("" + event.getSimulationTime());
				long systemQueueLength = 0;
				for (SignalData signal : system.getSignalData().values()) {
					long signalQueueLength = 0;
					// no lane events are thrown for links with only one lane -> use link events
					if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty() && 
							lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().size() > 1) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							signalQueueLength += sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(),
									laneId, 0., event.getSimulationTime());
						}
					} else {
						signalQueueLength += sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), 0.,
								event.getSimulationTime());
					}
					queueLengthPerSignalLine.append("\t" + signalQueueLength);
					totalWaitingTimePerSignal.put(signal.getId(),
							totalWaitingTimePerSignal.get(signal.getId()) + signalQueueLength);
					systemQueueLength += signalQueueLength;
				}
				if (queueLengthPerSignalOverTime.get(system.getId()) != null) {
					queueLengthPerSignalOverTime.get(system.getId()).println(queueLengthPerSignalLine.toString());
				}
				queueLengthLine.append("\t" + systemQueueLength);
				totalWaitingTimePerSystem.put(system.getId(),
						totalWaitingTimePerSystem.get(system.getId()) + systemQueueLength);
				totalQueueLength += systemQueueLength;
			}
			double avgQueueLength = totalQueueLength / noSystems;
			queueLengthLine.append("\t" + totalQueueLength + "\t" + avgQueueLength);
			if (queueLengthOverTime != null) {
				queueLengthOverTime.println(queueLengthLine.toString());
			}
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// prepare writing of queue length over time
//		if (event.getIteration() == lastIteration) {
			analyzeThisIt = true;
			
			// create analysis output directories
			String thisItAnalysisDir = itersDir + "/it." + event.getIteration() + "/analysis/";
			String thisItAnalysisDirPerSystem = thisItAnalysisDir + "perSystem/";
			new File(thisItAnalysisDirPerSystem).mkdirs();
			
			// create writing streams
			try {
				this.queueLengthOverTime = new PrintStream(new File(thisItAnalysisDir + "queueLengthOverTimePerSystem.txt"));
				String headerQueueLengthOverTime = "time";
				for (SignalSystemData system : signals.getSignalSystemsData().getSignalSystemData().values()) {
					headerQueueLengthOverTime += "\t" + system.getId();
					
					queueLengthPerSignalOverTime.put(system.getId(), 
							new PrintStream(new File(thisItAnalysisDirPerSystem + 
									"queueLengthOverTimePerSignal_System" + system.getId() + ".txt")));
					// print header for every system-file
					String headerQueueLengthPerSignalOverTime = "time";
					for (Id<Signal> signalId : system.getSignalData().keySet()) {
						headerQueueLengthPerSignalOverTime += "\t" + signalId;
					}
					queueLengthPerSignalOverTime.get(system.getId()).println(headerQueueLengthPerSignalOverTime);
				}
				headerQueueLengthOverTime += "\ttotal\tavg";
				queueLengthOverTime.println(headerQueueLengthOverTime);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
//		}
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
//		if (event.getIteration() == lastIteration) {
			// close queue length writing streams
			queueLengthOverTime.close();
			for (Id<SignalSystem> systemId : signals.getSignalSystemsData().getSignalSystemData().keySet()) {
				queueLengthPerSignalOverTime.get(systemId).close();
			}
			
			String thisItAnalysisDir = itersDir + "/it." + event.getIteration() + "/analysis/";

			writeWaitingTimes(thisItAnalysisDir);
			
			// call gnuplot scripts
			runGnuplotScript("plot_avgQueueLengthOverTime", thisItAnalysisDir);
			runGnuplotScript("plot_totalQueueLengthOverTime", thisItAnalysisDir);
//			runGnuplotScript("plot_queueLengthPerSystemOverTime", thisItAnalysisDir); // TODO one file with all system queue length
//			runGnuplotScript("plot_queueLengthPerSignalOverTime", thisItAnalysisDirPerSystem); // TODO file per system
			runGnuplotScript("plot_waitingTimePerSystem", thisItAnalysisDir);
//			runGnuplotScript("plot_waitingTimePerSignal", thisItAnalysisDirPerSystem); // TODO file per system
//		}
	}

	private void writeWaitingTimes(String outputDir) {
		PrintStream waitingTimesStream;
		String filenameWaitingTimes = outputDir + "totalWaitingTimesPerSystem.txt";
		try {
			waitingTimesStream = new PrintStream(new File(filenameWaitingTimes));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "system\ttotalWaitingTime";
		waitingTimesStream.println(header);
		
		for (SignalSystemData system : signals.getSignalSystemsData().getSignalSystemData().values()){
			waitingTimesStream.println(system.getId() + "\t" + totalWaitingTimePerSystem.get(system.getId()));
			
			writeWaitingTimesPerSignal(system, outputDir);
		}
		
		waitingTimesStream.close();
		LOG.info("waiting times for last iteration written to " + filenameWaitingTimes);
	}

	private void writeWaitingTimesPerSignal(SignalSystemData system, String outputDir) {
		PrintStream stream;
		String filename = outputDir + "totalWaitingTimesPerSignal_System"+system.getId()+".txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		String header = "signal\ttotalWaitingTime";
		stream.println(header);
		
		for (Id<Signal> signalId : system.getSignalData().keySet()) {
			stream.println(signalId + "\t" + totalWaitingTimePerSignal.get(signalId));
		}
		stream.close();
	}
	
	
	private void runGnuplotScript(String gnuplotScriptName, String pathToInputDir) {	
		// 'Users/theresa/workspace/' is the common top level, i.e. subtract 3 levels
//		int noLevels = pathToInputDir.trim().split("/").length - 4;
		int noLevels = 7;
		String levels = "";
		for (int i=0; i<noLevels; i++) {
			levels += "../";
		}
		String relativePathToGnuplotScript = levels + "shared-svn/studies/tthunig/gnuplotScripts/" + gnuplotScriptName  + ".p";
		
//		log.info("execute command: cd " + pathToSpecificAnalysisDir);
//		log.info("and afterwards: gnuplot " + relativePathToGnuplotScript);
		
		GnuplotUtils.runGnuplotScript(pathToInputDir, relativePathToGnuplotScript);
	}
}
