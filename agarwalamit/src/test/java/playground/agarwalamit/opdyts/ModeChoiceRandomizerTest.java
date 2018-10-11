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

package playground.agarwalamit.opdyts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Created by amit on 27.11.17.
 */

@RunWith(Parameterized.class)
public class ModeChoiceRandomizerTest {

    @Rule
    public final MatsimTestUtils helper = new MatsimTestUtils();

    public ModeChoiceRandomizerTest(ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizerStyle){
        this.ascRandomizerStyle = ascRandomizerStyle;
    }

    @Parameterized.Parameters(name = "{index}: ASCRandomizerStyle == {0};")
    public static List<Object[]> considerCO2 () {
        Object[] [] ascRandomizingStyples = new Object [] [] {
                { ModeChoiceRandomizer.ASCRandomizerStyle.axial_fixedVariation},
                {ModeChoiceRandomizer.ASCRandomizerStyle.axial_randomVariation},
                {ModeChoiceRandomizer.ASCRandomizerStyle.grid_fixedVariation},
                {ModeChoiceRandomizer.ASCRandomizerStyle.grid_randomVariation}
        };
        return Arrays.asList(ascRandomizingStyples);
    }

    private final ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizerStyle;
    private final OpdytsScenario opdytsScenario = OpdytsScenario.PATNA_1Pct;
    private final Collection<String> consideredModes = Arrays.asList("car","motorbike","bike","pt","walk");
    private final double stepSize = 0.25;
    private final Map<String, Double> modeToASCs = new HashMap<>();

    @Test
    public void test(){

        Config config = ConfigUtils.createConfig();

        modeToASCs.put("car", 0.0);
        modeToASCs.put("motorbike", -0.50);
        modeToASCs.put("bike", 0.75);
        modeToASCs.put("pt", 1.0);
        modeToASCs.put("walk", -0.25);

        PlanCalcScoreConfigGroup initialScoringConfigGroup = config.planCalcScore();
        for(String mode : this.consideredModes) {
            initialScoringConfigGroup.getOrCreateModeParams(mode).setConstant(modeToASCs.get(mode));
        }

        OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.class);
        opdytsConfigGroup.setDecisionVariableStepSize(stepSize);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(
                initialScoringConfigGroup,
                scenario,
                this.consideredModes,
                this.opdytsScenario);

        ModeChoiceRandomizer modeChoiceRandomizer = new ModeChoiceRandomizer(scenario,
                RandomizedUtilityParametersChoser.ONLY_ASC,
                this.opdytsScenario,
                null,
                this.consideredModes,
                this.ascRandomizerStyle);

        List<ModeChoiceDecisionVariable> result = modeChoiceRandomizer.newRandomVariations(initialDecisionVariable, 0); // FIXME: I added a second argument but I am not sure if this is correct. ihab oct '18

        // check number of new decision variables
        switch (this.ascRandomizerStyle) {
            case axial_randomVariation:
                Assert.assertEquals("Wrong number of new decision variables", (this.consideredModes.size()-1)*2, result.size(), MatsimTestUtils.EPSILON);
                break;
            case grid_randomVariation:
                Assert.assertEquals("Wrong number of new decision variables", Math.pow(2,this.consideredModes.size()-1), result.size(), MatsimTestUtils.EPSILON);
                break;
            case axial_fixedVariation:
                Assert.assertEquals("Wrong number of new decision variables", (this.consideredModes.size()-1)*2, result.size(), MatsimTestUtils.EPSILON);
                break;
            case grid_fixedVariation:
                Assert.assertEquals("Wrong number of new decision variables", Math.pow(2,this.consideredModes.size()-1), result.size(), MatsimTestUtils.EPSILON);
                break;
        }

        // before implementation, values must be same as input
        for (String mode : this.consideredModes) {
            Assert.assertEquals("wrong value of ASC ",
                    modeToASCs.get(mode),
                    scenario.getConfig().planCalcScore().getOrCreateModeParams(mode).getConstant(),
                    MatsimTestUtils.EPSILON);
        }

        result.forEach(e -> System.out.println(e.toString()));

        List<String> variationsFromMethod = new ArrayList<>();
        double randomVariation = 0.;
        for (ModeChoiceDecisionVariable newDecisionVariable : result) {
            newDecisionVariable.implementInSimulation();
            variationsFromMethod.add(getASCAsString(scenario));
            Assert.assertEquals("Wrong ASC for mode car in new decision variables",
                    0.0,
                    scenario.getConfig().planCalcScore().getOrCreateModeParams("car").getConstant(),
                    MatsimTestUtils.EPSILON);
            for(String mode: this.consideredModes) {
                double value = Math.abs(modeToASCs.get(mode) - scenario.getConfig().planCalcScore().getOrCreateModeParams(mode).getConstant());
                randomVariation = Math.max(randomVariation, value); // 0 or random variation (same amount in all cases)
            }
        }

        List<String> variations = new ArrayList<>();
        switch (this.ascRandomizerStyle) {
            case axial_randomVariation:
            case axial_fixedVariation:
                variations = getAxialVariations(randomVariation);
                break;
            case grid_randomVariation:
            case grid_fixedVariation:
                variations = getDiagonalVariations(randomVariation);
                break;
        }

        Assert.assertTrue("random variation must be greater than or equal to step size",randomVariation >= stepSize*0 );
        Assert.assertTrue("random variation must be smaller than or equal to step size",randomVariation <= stepSize*1 );
        Collections.sort(variations);
        Collections.sort(variationsFromMethod);
        Assert.assertThat(variationsFromMethod, Is.is(variationsFromMethod));
    }

    private List<String> getAxialVariations(double randomVariation){
        List<String> variations = new ArrayList<>();
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt"),this.modeToASCs.get("walk")));
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt"),this.modeToASCs.get("walk")));
        //motorbike
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt"),this.modeToASCs.get("walk")));
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt"),this.modeToASCs.get("walk")));
        //PT
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")));
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")));
        //walk
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt"),this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString(this.modeToASCs.get("car"),this.modeToASCs.get("bike"),this.modeToASCs.get("motorbike"),this.modeToASCs.get("pt"),this.modeToASCs.get("walk")-randomVariation));
        return variations;
    }

    private List<String> getDiagonalVariations(double randomVariation) {
        List<String> variations = new ArrayList<>();
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")+randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")+randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")+randomVariation,this.modeToASCs.get("walk")-randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")+randomVariation));
        variations.add( getASCAsString( this.modeToASCs.get("car"),this.modeToASCs.get("bike")-randomVariation,this.modeToASCs.get("motorbike")-randomVariation,this.modeToASCs.get("pt")-randomVariation,this.modeToASCs.get("walk")-randomVariation));
        return variations;
    }

    private String getASCAsString(Scenario scenario){
        StringBuilder stringBuilder = new StringBuilder();
        for (String mode : this.consideredModes) {
            stringBuilder.append(mode+"\t"+ NumberUtils.round(scenario.getConfig().planCalcScore().getOrCreateModeParams(mode).getConstant(), 2));
        }
        return stringBuilder.toString();
    }

    private String getASCAsString(double carASC, double bikeASC, double motorbikeASC, double PTASC, double walkASC){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("car\t"+ NumberUtils.round(carASC,2));
        stringBuilder.append("motorbike\t"+ NumberUtils.round(motorbikeASC,2));
        stringBuilder.append("bike\t"+ NumberUtils.round(bikeASC,2));
        stringBuilder.append("pt\t"+ NumberUtils.round(PTASC,2));
        stringBuilder.append("walk\t"+ NumberUtils.round(walkASC,2));
        return stringBuilder.toString();
    }

}
