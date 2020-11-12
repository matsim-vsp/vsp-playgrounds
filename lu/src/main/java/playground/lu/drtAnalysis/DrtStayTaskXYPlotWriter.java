package playground.lu.drtAnalysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lu.drtAnalysis.StayTaskManager.StayTaskDataEntry;

public class DrtStayTaskXYPlotWriter {
	private final int eventsQueueSize = 1048576 * 32;

	public static void main(String[] args) throws IOException {
		String eventsFile = args[0];
		String networkFile = args[1];
		String outputFile = args[2];

		DrtStayTaskXYPlotWriter drtStayTaskXYPlotWriter = new DrtStayTaskXYPlotWriter();
		drtStayTaskXYPlotWriter.run(eventsFile, networkFile, outputFile);
	}

	public void run(String eventsFile, String networkFile, String outputFile) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();
		StayTaskManager stayTaskManager = new StayTaskManager();

		ParallelEventsManager eventManager = new ParallelEventsManager(false, eventsQueueSize);

		eventManager.addHandler(stayTaskManager);
		eventManager.initProcessing();

		MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager);
		matsimEventsReader.readFile(eventsFile);

		List<StayTaskDataEntry> stayTaskDataEntries = stayTaskManager.getStayTaskDataEntriesList();
		System.out.println("there are " + stayTaskDataEntries.size() + " stay tasks in total");
		writeResultIntoCSVFile(stayTaskDataEntries, network, outputFile);
	}

	private void writeResultIntoCSVFile(List<StayTaskDataEntry> stayTaskDataEntries, Network network, String outputFile)
			throws IOException {
		System.out.println("Writing CSV File now");
		FileWriter csvWriter = new FileWriter(outputFile);

		csvWriter.append("Stay Task ID");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append(",");
		csvWriter.append("Start Time");
		csvWriter.append(",");
		csvWriter.append("End Time");
		csvWriter.append(",");
		csvWriter.append("Driver Id");
		csvWriter.append("\n");

		for (StayTaskDataEntry stayTaskDataEntry : stayTaskDataEntries) {
			double X = network.getLinks().get(stayTaskDataEntry.getLinkId()).getCoord().getX();
			double Y = network.getLinks().get(stayTaskDataEntry.getLinkId()).getCoord().getY();
			List<String> elements = new ArrayList<>();

			elements.add(stayTaskDataEntry.getStayTaskId());
			elements.add(Double.toString(X));
			elements.add(Double.toString(Y));
			elements.add(Double.toString(stayTaskDataEntry.getStartTime()));
			elements.add(Double.toString(stayTaskDataEntry.getEndTime()));
			elements.add(stayTaskDataEntry.getPersonId().toString());

			csvWriter.append(String.join(",", elements));
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();
	}

}
