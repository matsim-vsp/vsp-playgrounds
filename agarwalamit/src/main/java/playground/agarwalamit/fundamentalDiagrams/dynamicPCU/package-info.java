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
 * Created by amit on 17.07.17.
 */

/**
 * The idea is to use one of the following approach to estimate the dynamic PCU.
 * <ul>
 *   <li>let's say: PCU as the ratio of speeds to the ratio of projected areas</li>
 *     <ul>
 *       <li> set it at the end of track; speed of the leaving vehicle is estimated from actual travel time and speed of car would be the last known speed.</li>
 *       <li> at the beginning of the track, take the average speed of vehicles types (vehicles already on the link) same as the entering vehicle; for speed of car, take the average speed of cars on the track. </li>
 *     </ul>
 * </ul>
 */

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU;

