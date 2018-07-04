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

package playground.agarwalamit.opdyts;

import javax.inject.Inject;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;

/**
 * It simply changes the step size based on transition number and a parameter ('beta') [0.5,1.0] (from GF).
 *
 * Created by amit on 02.07.18.
 */

public class StepSizeGenerator {

    private double stepSize ;
    private double beta = 1.0; // TODO: make it configurable via opdytsConfigGroup.

    @Inject
    public StepSizeGenerator(OpdytsConfigGroup opdytsConfigGroup) {
        this.stepSize = opdytsConfigGroup.getDecisionVariableStepSize();
    }

    public double getStepSize(int opdytsTransition) {
        return stepSize * 1 / Math.pow(opdytsTransition, beta);
    }
}
