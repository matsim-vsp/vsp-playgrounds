/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package saleem.stockholmmodel.resultanalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
/**
 * This class calculates different relevant statistics about Stockholm scenario,
 * based on an event file.
 * 
 * @author Mohammad Saleem
 *
 */
public class StockholmScenarioAnalysis {
	public static void main(String[] args){
		String path = "./ihop2/matsim-input/configoptimisationcarpt.xml";
		final Config config = ConfigUtils.loadConfig(path);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final EventsManager eventmanager = EventsUtils.createEventsManager(config);
		StockholmScenarioStatisticsCalculator handler = new StockholmScenarioStatisticsCalculator(scenario.getPopulation().getPersons());
		eventmanager.addHandler(handler);
		final MatsimEventsReader reader = new MatsimEventsReader(eventmanager);
		reader.readFile("./ihop2/matsim-input/500.events.xml.gz");
		handler.getTT();
		handler.getTotalrips();
	}
}
