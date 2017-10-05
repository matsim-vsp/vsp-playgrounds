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

package playground.agarwalamit.nemo.demand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by amit on 23.09.17.
 */

public class NRWControler {

    public static void main(String[] args) {

        String plansFile = "/Users/amit/Documents/gitlab/nemo/data/input/matsim_initial_plans/nrw_plans_1pct.xml.gz";
        String networkFile = "/Users/amit/Documents/gitlab/nemo/data/input/network/motorway-tertiary/network_motorway-tertiary.xml.gz";
        String outputDir = "/Users/amit/Documents/gitlab/nemo/data/output/";

        if (args.length>0){
            plansFile = args[0];
            networkFile = args[1];
            outputDir = args[2];
        }

        Config config = ConfigUtils.createConfig();

        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);

        configSetting(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);
        controler.run();
    }

    private static void configSetting(final Config config) {
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(10*3600.);
        planCalcScoreConfigGroup.addActivityParams(home);

        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(8*3600.);
        planCalcScoreConfigGroup.addActivityParams(work);

        PlanCalcScoreConfigGroup.ActivityParams shopping = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
        shopping.setTypicalDuration(1*3600.);
        planCalcScoreConfigGroup.addActivityParams(shopping);

        PlanCalcScoreConfigGroup.ActivityParams leisure = new PlanCalcScoreConfigGroup.ActivityParams("leisure");
        leisure.setTypicalDuration(2*3600.);
        planCalcScoreConfigGroup.addActivityParams(leisure);

        PlanCalcScoreConfigGroup.ActivityParams other = new PlanCalcScoreConfigGroup.ActivityParams("other");
        other.setTypicalDuration(2*3600.);
        planCalcScoreConfigGroup.addActivityParams(other);

        QSimConfigGroup qSimConfigGroup = config.qsim();
        qSimConfigGroup.setFlowCapFactor(0.01);
        qSimConfigGroup.setStorageCapFactor(0.03);
        qSimConfigGroup.setEndTime(30*3600.);
        qSimConfigGroup.setUsingFastCapacityUpdate(true);

        StrategyConfigGroup strategyConfigGroup = config.strategy();
        StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
        reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
        reRoute.setWeight(0.15);
        strategyConfigGroup.addStrategySettings(reRoute);

        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
        modeChoice.setWeight(0.1);
        strategyConfigGroup.addStrategySettings(modeChoice);
        config.changeMode().setModes(new String[] {"car","pt"});

        StrategyConfigGroup.StrategySettings timeChoice = new StrategyConfigGroup.StrategySettings();
        timeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.name());
        timeChoice.setWeight(0.05);
        strategyConfigGroup.addStrategySettings(timeChoice);

        TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup = config.timeAllocationMutator();
        timeAllocationMutatorConfigGroup.setMutationRange(7200.);
        timeAllocationMutatorConfigGroup.setAffectingDuration(false);

        StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
        changeExpBeta.setWeight(0.7);
        strategyConfigGroup.addStrategySettings(changeExpBeta);

        PlansCalcRouteConfigGroup plansCalcRouteConfigGroup = config.plansCalcRoute();
        PlansCalcRouteConfigGroup.ModeRoutingParams pt = new PlansCalcRouteConfigGroup.ModeRoutingParams("pt");
        pt.setTeleportedModeFreespeedFactor(1.5);
        plansCalcRouteConfigGroup.addModeRoutingParams(pt);
    }
}
