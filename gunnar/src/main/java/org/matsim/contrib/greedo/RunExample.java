/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.greedo;

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RunExample {

	static public void main(String[] args) {
		// URL configURL =
		// IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"),
		// "config_default.xml");
		URL configURL = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Greedo greedo = new Greedo();

		Config config = ConfigUtils.loadConfig(configURL);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("output");
		greedo.meet(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		greedo.meet(scenario);

		Controler controller = new Controler(scenario);
		for (AbstractModule module : greedo.getModules()) {
			controller.addOverridingModule(module);
		}

		controller.run();
	}
}
