package playground.vsptelematics.ha1;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class ControllerWithOtfvis {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0]) ;
		config.qsim().setUsingFastCapacityUpdate(false);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsDataLoader(config).loadSignalsData());
		}
		Controler c = new Controler(scenario);
		c.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		c.getConfig().controler().setCreateGraphs(false);
		// add the signals module if signal systems are used
		if (signalsConfigGroup.isUseSignalSystems()) {
//			c.addOverridingModule(new SignalsModule());
			Signals.configure( c );
		}
//		c.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				addControlerListenerBinding().to(RouteTTObserver.class);
//			}
//		});
		
		c.addOverridingModule(new OTFVisWithSignalsLiveModule());
		
		c.run();
	}
	
}
