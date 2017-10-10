/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.input;

import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.utils.objectattributes.attributeconverters.CoordConverter;

/**
 * @author dziemke
 */
public class HouseholdFileModifier {

private final static Logger LOG = Logger.getLogger(PlanFileModifier.class);
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0) { // TODO
			throw new IllegalArgumentException("Arguments array must have a length of 0, ....!"); // TODO
		}
		
		// Local use
//		String inputHouseholdFileName = "../../upretoria/data/capetown/scenario_2017/original/households.xml.gz";
//		String outputHouseholdFileName = "../../upretoria/data/capetown/scenario_2017/households_32734.xml.gz";
//		String inputHouseholdAttributesFileName = "../../upretoria/data/capetown/scenario_2017/original/householdAttributes.xml.gz";
//		String outputHouseholdAttributesFileName = "../../upretoria/data/capetown/scenario_2017/householdAttributes_32734.xml.gz";
		String inputHouseholdFileName = "../../capetown/data/scenario_2017/households_32734.xml.gz";
		String outputHouseholdFileName = "../../capetown/data/scenario_2017/households_32734_1pct.xml.gz";
		String inputHouseholdAttributesFileName = "../../capetown/data/scenario_2017/householdAttributes_32734.xml.gz";
		String outputHouseholdAttributesFileName = "../../capetown/data/scenario_2017/householdAttributes_32734_1pct.xml.gz";
		double selectionProbability = 0.1;
		String coordIdentifier = "homeCoord";
		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
		String outputCRS = "EPSG:32734";
		
		CoordinateTransformation ct;
		if (inputCRS == null && outputCRS == null) {
			ct = new IdentityTransformation();
		} else {
			ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		}
		
		// Server use, version without CRS transformation // TODO
//		if (args.length == 3) {
//			inputHouseholdFileName = args[0];
//		}
		
		modifyHouseholds(inputHouseholdFileName, outputHouseholdFileName, inputHouseholdAttributesFileName,
				outputHouseholdAttributesFileName, selectionProbability, coordIdentifier, ct);
	}
		
	public static void modifyHouseholds(String inputHouseholdFileName, String outputHouseholdFileName,
			String inputHouseholdAttributesFileName, String outputHouseholdAttributesFileName, double selectionProbability,
			String coordIdentifier, CoordinateTransformation ct) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		HouseholdsReaderV10 householdsReader = new HouseholdsReaderV10(scenario.getHouseholds());
		
		householdsReader.readFile(inputHouseholdFileName);
		
		ObjectAttributes householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader householdAttributesReader = new ObjectAttributesXmlReader(householdAttributes);
		householdAttributesReader.putAttributeConverter(Coord.class, new CoordConverter());
		householdAttributesReader.readFile(inputHouseholdAttributesFileName);
						
		Random random = MatsimRandom.getLocalInstance();
		
		Iterator<Household> it = scenario.getHouseholds().getHouseholds().values().iterator();
		
		while (it.hasNext()) {
			Household household = it.next();
			if (random.nextDouble() > selectionProbability) {
				it.remove();
			} else {
				Coord coord = (Coord) householdAttributes.getAttribute(household.getId().toString(), coordIdentifier);
				householdAttributes.putAttribute(household.getId().toString(), coordIdentifier, ct.transform(coord));
			}
		}
			
		HouseholdsWriterV10 householdsWriter = new HouseholdsWriterV10(scenario.getHouseholds());
		householdsWriter.writeFile(outputHouseholdFileName);
		LOG.info("Done writing the households file.");
		
		ObjectAttributesXmlWriter householdAttributesWriter = new ObjectAttributesXmlWriter(householdAttributes);
		householdAttributesWriter.putAttributeConverter(Coord.class, new CoordConverter());
		householdAttributesWriter.writeFile(outputHouseholdAttributesFileName);
		LOG.info("Done writing the household attributes file.");
	}
}