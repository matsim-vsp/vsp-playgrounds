package signals.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Class to convert an arbitrary signal control file of MATSim into an all day green signal control.
 * 
 * @author tthunig
 * 
 */
public class TtCreateAllDayGreenFromSignalControl {

	public static void main(String[] args) {
		
		String signalControlInputFile = "../../shared-svn/projects/cottbus/data/scenarios/parallel_scenario/AB/signalControlBC.xml";
		String signalControlOutputFile = "../../shared-svn/projects/cottbus/data/scenarios/parallel_scenario/AB/signalControlGreen.xml";
		
		convertAndWriteSignalControl(signalControlOutputFile, signalControlInputFile);
	}

	private static void convertAndWriteSignalControl(
			String outputSignalControl, String signalControlFilename) {
		
		Config config = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);
		signalsConfigGroup.setSignalControlFile(signalControlFilename);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		SignalsData signals = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		// iterate over all signal systems and get their signal control data
		for (SignalSystemControllerData signalSystemControl : signals.getSignalControlData().
				getSignalSystemControllerDataBySystemId().values()) {
			// iterate over all signal plans (there is normally only one per signal system)
			for (SignalPlanData signalPlan : signalSystemControl.getSignalPlanData().values()){
				// iterate over all signal groups of the signal system
				for (SignalGroupSettingsData signalGroupSetting : signalPlan.
						getSignalGroupSettingsDataByGroupId().values()){
					// set them to green for the whole cycle (all day green)
					signalGroupSetting.setOnset(0);
					signalGroupSetting.setDropping(signalPlan.getCycleTime());
				}
			}
		}
		
		// write all day green signal control
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		writer.setSignalControlOutputFilename(outputSignalControl);
		writer.writeSignalsData(scenario);
	}

}
