/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * Created by amit on 20.05.18.
 */

public class FDDataWriter implements IterationEndsListener, ShutdownListener {

    private PrintStream writer;

    private final FDDataContainer fdDataContainer;
    private final StabilityTester stabilityTester;
    private final Scenario scenario;
    private final FDNetworkGenerator fdNetworkGenerator;
    private final FundamentalDiagramConfigGroup fundamentalDiagramConfigGroup;

    private String travelModes [] ;

    @Inject
    FDDataWriter(Scenario scenario, FDDataContainer fdDataContainer,
                 StabilityTester stabilityTester, FDNetworkGenerator fdNetworkGenerator,
                 FundamentalDiagramConfigGroup fundamentalDiagramConfigGroup){

        this.scenario = scenario;
        this.fdDataContainer = fdDataContainer;
        this.stabilityTester = stabilityTester;
        this.fdNetworkGenerator = fdNetworkGenerator;
        this.fundamentalDiagramConfigGroup = fundamentalDiagramConfigGroup;

        travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);

        try {
            this.writer = new PrintStream(scenario.getConfig().controler().getOutputDirectory()+"/data.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
        writer.print("n \t");
        Arrays.stream(travelModes).forEach(travelMode -> writer.print("n_" + travelMode + "\t"));

        writer.print("k \t");
        Arrays.stream(travelModes).forEach(travelMode -> writer.print("k_" + travelMode + "\t"));

        writer.print("q \t");
        Arrays.stream(travelModes).forEach(travelMode -> writer.print(("q_" + travelMode) + "\t"));

        writer.print("v \t");
        Arrays.stream(travelModes).forEach(travelMode -> writer.print(("v_" + travelMode) + "\t"));
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

        int flowUnstableWarnCount []  = new int [travelModes.length];
        int speedUnstableWarnCount [] = new int [travelModes.length];

        boolean stableState = true;
        for(int index=0;index<travelModes.length;index++){
            String veh = travelModes[index];
            if(!fdDataContainer.getTravelModesFlowData().get(veh).isFlowStable())
            {
                stableState = false;
                int existingCount = flowUnstableWarnCount[index]; existingCount++;
                flowUnstableWarnCount[index] = existingCount;
                FundamentalDiagramDataGenerator.LOG.warn("Flow stability is not reached for travel mode "+ veh
                        +" and simulation end time is reached. Output data sheet will have all zeros for such runs."
                        + "This is " + flowUnstableWarnCount[index]+ "th warning.");
            }
            if(!fdDataContainer.getTravelModesFlowData().get(veh).isSpeedStable())
            {
                stableState = false;
                int existingCount = speedUnstableWarnCount[index]; existingCount++;
                speedUnstableWarnCount[index] = existingCount;
                FundamentalDiagramDataGenerator.LOG.warn("Speed stability is not reached for travel mode "+ veh
                        +" and simulation end time is reached. Output data sheet will have all zeros for such runs."
                        + "This is " + speedUnstableWarnCount[index]+ "th warning.");
            }
        }
        if(!stabilityTester.isStabilityAchieved()) stableState=false;

        // sometimes higher density points are also executed (stuck time), to exclude them density check.
        double cellSizePerPCU = scenario.getNetwork().getEffectiveCellSize();
        double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fundamentalDiagramConfigGroup.getTrackLinkLanes() / cellSizePerPCU;

        if(stableState){
            double globalLinkDensity = fdDataContainer.getGlobalData().getPermanentDensity();
            if(globalLinkDensity > networkDensity / 3 + 10 ) stableState =false; //+10; since we still need some points at max density to show zero speed.
        }

        if( stableState ) {
            writer.print("\n"); //always stats with a new line

            writer.format("%d\t",fdDataContainer.getGlobalData().getnumberOfAgents());
            for (String travelMode : travelModes) {
                writer.format("%d\t", fdDataContainer.getTravelModesFlowData().get(travelMode).getnumberOfAgents());
            }
            writer.format("%.2f\t", fdDataContainer.getGlobalData().getPermanentDensity());
            for (String travelMode : travelModes) {
                writer.format("%.2f\t", fdDataContainer.getTravelModesFlowData().get(travelMode).getPermanentDensity());
            }
            writer.format("%.2f\t", fdDataContainer.getGlobalData().getPermanentFlow());
            for (String travelMode : travelModes) {
                writer.format("%.2f\t", fdDataContainer.getTravelModesFlowData().get(travelMode).getPermanentFlow());
            }
            writer.format("%.2f\t", fdDataContainer.getGlobalData().getPermanentAverageVelocity());
            for (String travelMode : travelModes) {
                writer.format("%.2f\t", fdDataContainer.getTravelModesFlowData().get(travelMode).getPermanentAverageVelocity());
            }
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        this.writer.close();
    }
}
