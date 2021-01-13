package playground.gleich.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import playground.vsp.andreas.utils.pt.TransitLineRemover;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.util.HashSet;
import java.util.Set;

public class TransitLinesRemover {

	public static void main(String[] args) {
//		final String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedSchedule.xml.gz";
//		final String inNetworkFile  = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz";
//		final String outScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/optimizedSchedule_wo-TB247-TB252-TB253-TB254-TB256.xml.gz";

		final String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
		final String inNetworkFile  = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";
		final String outScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedSchedule_wo_523-4.xml.gz";


		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:25832");
		config.transit().setTransitScheduleFile(inScheduleFile);
		config.network().setInputFile(inNetworkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule inTransitSchedule = scenario.getTransitSchedule();
		
		Set<Id<TransitLine>> linesToRemove = new HashSet<>();
//		linesToRemove.add(Id.create("ASTTB247 - 1", TransitLine.class));
//		linesToRemove.add(Id.create("ASTTB256 - 1", TransitLine.class));
//
//		linesToRemove.add(Id.create("ASTTB252 - 1", TransitLine.class));
//		linesToRemove.add(Id.create("ASTTB253 - 1", TransitLine.class));
//		linesToRemove.add(Id.create("ASTTB254 - 1", TransitLine.class));
		linesToRemove.add(Id.create("Bus 523 - 4", TransitLine.class));
		linesToRemove.add(Id.create("Bus 523 - 5", TransitLine.class));

		TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);
		TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);
		new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);
	}

}
