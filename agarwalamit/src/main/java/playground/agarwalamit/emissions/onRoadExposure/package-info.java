/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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



/**
 * Created by amit on 20.11.17.
 *
 * <p>If the events file do not have combined events file, calculation of onRoadExposure may throw an exception or
 * the calculation will not be accurate.</p>
 *
 * <p>The problem is that: reading events file, generating emissions events and then writing them back do not write the events in
 * correct order (see EmissionEventsTest).</p>
 *
 * <p>Calculation of on road exposure is designed assuming a combined events file (in right order); this, this cant be used.</p>
 *
 * <p> A simple way is to rerun simulation using last iteration events file for 1 iteration and then write the emissions during simulation. </p>
 * <p> Alternatively, process all required events for calculation of onRoadExposure in a later time step. </p>
 */
package playground.agarwalamit.emissions.onRoadExposure;