package cba.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.Time;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.Histogram;
import floetteroed.utilities.math.Vector;
//import gunnar.ihop2.regent.demandreading.ZonalSystem;
//import gunnar.ihop2.regent.demandreading.Zone;
//import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class EstimateTimePressure2 {

	private EstimateTimePressure2() {
	}

	static String printTimes(final List<Double> times) {
		if (times == null) {
			return null;
		}
		final StringBuffer result = new StringBuffer();
		for (double time : times) {
			result.append(Time.strFromSec((int) Math.min(24 * 3600, Math.max(0, time))));
			result.append(" ");
		}
		return result.toString();
	}

	static <K> void addTime(final Map<K, List<Double>> map, final K key, final double value) {
		List<Double> list = map.get(key);
		if (list == null) {
			list = new ArrayList<>(3);
			map.put(key, list);
		}
		list.add(value);
	}

	public static void main(String[] args) {
//
//		System.out.println("STARTED ...");
//
//		final String path = "/Users/gunnarfl/Documents/data/ihop/";
//
//		final String networkFileName = path + "ihop2-data/network-output/network.xml";
//		final Config config = ConfigUtils.createConfig();
//		config.getModule("network").addParam("inputNetworkFile", networkFileName);
//		final Scenario scenario = ScenarioUtils.loadScenario(config);
//
//		final String zoneShapeFileName = path + "ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
//		final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
//				StockholmTransformationFactory.WGS84_EPSG3857);
//		zonalSystem.addNetwork(scenario.getNetwork(), StockholmTransformationFactory.WGS84_SWEREF99);
//
//		final String eventsFileName = path + "ihop2-showcase/2015-11-23ab_LARGE_RegentMATSim/2015-11-23a_No_Toll_large/"
//				+ "summary/iteration-3/it.400/400.events.xml.gz";
//		final Set<Id<Person>> persons = new LinkedHashSet<>();
//		final Map<Tuple<Id<Person>, String>, List<Double>> personAndAct2startTimes = new LinkedHashMap<>();
//		final Map<Tuple<Id<Person>, String>, List<Double>> personAndAct2endTimes = new LinkedHashMap<>();
//		final Map<Tuple<Id<Person>, String>, Zone> personAndAct2zone = new LinkedHashMap<>();
//		final EventsManager events = EventsUtils.createEventsManager();
//		events.addHandler(new ActivityStartEventHandler() {
//			@Override
//			public void reset(int iteration) {
//			}
//
//			@Override
//			public void handleEvent(ActivityStartEvent event) {
//				persons.add(event.getPersonId());
//				final Tuple<Id<Person>, String> personAndAkt = new Tuple<>(event.getPersonId(), event.getActType());
//				addTime(personAndAct2startTimes, personAndAkt, event.getTime());
//				personAndAct2zone.put(personAndAkt,
//						zonalSystem.getZone(scenario.getNetwork().getLinks().get(event.getLinkId()).getFromNode()));
//			}
//		});
//		events.addHandler(new ActivityEndEventHandler() {
//			@Override
//			public void reset(int iteration) {
//			}
//
//			@Override
//			public void handleEvent(ActivityEndEvent event) {
//				persons.add(event.getPersonId());
//				addTime(personAndAct2endTimes, new Tuple<>(event.getPersonId(), event.getActType()), event.getTime());
//			}
//		});
//		new MatsimEventsReader(events).readFile(eventsFileName);
//
//		final Map<Zone, List<Double>> zone2timePressures = new LinkedHashMap<>();
//		final List<Double> timePressures = new ArrayList<Double>(persons.size());
//		for (Id<Person> person : persons) {
//
//			personAndAct2startTimes.get(new Tuple<>(person, "home")).add(0, 0.0);
//			personAndAct2endTimes.get(new Tuple<>(person, "home")).add(24.0 * 3600);
//
//			// System.out.println();
//			// System.out.println("PERSON: " + person);
//			// for (String act : new String[] { "home", "work", "other" }) {
//			// System.out.println(
//			// act + " start times: " +
//			// printTimes(personAndAct2startTimes.get(new Tuple<>(person,
//			// act))));
//			// System.out.println(
//			// act + " end times: " + printTimes(personAndAct2endTimes.get(new
//			// Tuple<>(person, act))));
//			// }
//
//			final Map<String, Double> act2dur = new LinkedHashMap<>();
//			for (String act : new String[] { "home", "work", "other" }) {
//				final List<Double> startTimes_s = personAndAct2startTimes.get(new Tuple<>(person, act));
//				final List<Double> endTimes_s = personAndAct2endTimes.get(new Tuple<>(person, act));
//				if (startTimes_s != null && startTimes_s.size() > 0) {
//					double dur_s = 0;
//					for (int i = 0; i < startTimes_s.size(); i++) {
//						dur_s += endTimes_s.get(i) - startTimes_s.get(i);
//					}
//					act2dur.put(act, dur_s);
//				}
//			}
//			// System.out.println("durations: " + act2dur);
//
//			double timePressure = 0;
//			double desiredDur_s = 0;
//			if (act2dur.containsKey("home")) {
//				timePressure += Math.pow(14.5 * 3600, 2) / act2dur.get("home");
//				desiredDur_s += 14.5 * 3600;
//			}
//			if (act2dur.containsKey("work")) {
//				timePressure += Math.pow(8.0 * 3600, 2) / act2dur.get("work");
//				desiredDur_s += 8.0 * 3600;
//			}
//			if (act2dur.containsKey("other")) {
//				timePressure += Math.pow(1.5 * 3600, 2) / act2dur.get("other");
//				desiredDur_s += 1.5 * 3600;
//			}
//			timePressure /= desiredDur_s;
//			// System.out.println("time pressure: " + timePressure);
//
//			if (!Double.isInfinite(timePressure)) {
//				timePressures.add(timePressure);
//				List<Double> zonalTimePressures = zone2timePressures.get(new Tuple<>(person, "work"));
//				if (zonalTimePressures == null) {
//					zonalTimePressures = new ArrayList<>();
//					zone2timePressures.put(personAndAct2zone.get(new Tuple<>(person, "work")), zonalTimePressures);
//				}
//				zonalTimePressures.add(timePressure);
//			}
//		}
//
//		System.out.println();
//		System.out.println();
//
//		double avgTimePressure = 0;
//		for (Double timePressure : timePressures) {
//			avgTimePressure += timePressure;
//		}
//		avgTimePressure /= timePressures.size();
//
//		// for (double beta_travel_matsim : new double[] { -1.0, -.9, -.8, -.7,
//		// -.6, -.5, -.4, -.3, -.2, -.1, 0 }) {
//		for (double beta_travel_matsim : new double[] { 0 }) {
//
//			final double beta_time_regent = -1.0;
//			final double beta_act_matsim = avgTimePressure * (beta_travel_matsim - beta_time_regent);
//
//			System.out.println("beta_time_regent = " + beta_time_regent);
//			System.out.println("beta_act_matsim = " + beta_act_matsim);
//			System.out.println("beta_travel_matsim = " + beta_travel_matsim);
//			System.out.println();
//
//			final Histogram hist = new Histogram(-1.55, -1.45, -1.35, -1.25, -1.15, -1.05, -0.95, -.85, -.75, -.65,
//					-.55, -.45);
//			for (Double timePressure : timePressures) {
//				hist.add(-beta_act_matsim * timePressure + beta_travel_matsim);
//			}
//
//			System.out.print(beta_travel_matsim);
//			System.out.print("\t");
//			for (int bin = 0; bin < hist.binCnt(); bin++) {
//				System.out.print(hist.freq(bin));
//				System.out.print("\t");
//			}
//			System.out.println();
//
//			final List<Double> zonalTimePressures = new ArrayList<>();
//			for (Map.Entry<Zone, List<Double>> entry : zone2timePressures.entrySet()) {
//				final double avgVal = new Vector(entry.getValue()).mean();
//				zonalTimePressures.add(avgVal);
//				if (entry.getKey() != null) {
//					System.out.println(entry.getKey().getId() + "\t" + avgVal);
//				}
//			}
//			// Collections.sort(zonalTimePressures);
//			// System.out.println();
//			// for (Double val : zonalTimePressures) {
//			// System.out.println(val);
//			// }
//
//		}
//
//		System.out.println();

	}

}
