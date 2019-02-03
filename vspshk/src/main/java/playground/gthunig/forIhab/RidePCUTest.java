package playground.gthunig.forIhab;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;

public class RidePCUTest {

    private static String EQUIL_DIR = "../matsim-code-examples/scenarios/equil-ride/";

    public static void main(String[] args) {


        Config config = ConfigUtils.loadConfig(EQUIL_DIR + "config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setMainModes(Arrays.asList(TransportMode.car, TransportMode.ride));
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.travelTimeCalculator().setAnalyzedModesAsString(TransportMode.car + "," + TransportMode.ride );
        config.plansCalcRoute().setNetworkModes(Arrays.asList(TransportMode.car, TransportMode.ride));
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
        PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("ride");
        modeParams.setConstant(-11);
        modeParams.setMarginalUtilityOfTraveling(0);
        modeParams.setMonetaryDistanceRate(-1.4E-4);
        config.planCalcScore().addModeParams(modeParams);
        config.vehicles().setVehiclesFile("modeVehicleTypes_car_ride.xml");
//        config.plans().setInputFile("plans10.xml");
        config.plans().setInputFile("plans20withRide.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();
    }
}
