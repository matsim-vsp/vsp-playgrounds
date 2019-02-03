/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Runner
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.dgrether.analysis.charts.DgTravelTimeCalculatorChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.koehlerstrehlersignal.analysis.DgMfd;
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;


/**
 * @author dgrether
 *
 */
public class DgFigure9Runner {
//	private String configFile = "../../shared-svn/studies/dgrether/koehlerStrehler2010/scenario5/config_signals_coordinated.xml";
	
	private String configFile = "../../shared-svn/studies/dgrether/koehlerStrehler2010/scenario5/config_testing.xml";

	private TTInOutflowEventHandler handler23, handler27, handler54, handler58;

	private DgMfd mfdHandler;
	
	private void runFromConfig(String conf) {
		if (conf == null){
			conf = configFile;
		}
		Controler controler = new Controler(conf);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		this.addControlerListener(controler);

		controler.run();
	}


	private void addControlerListener(MatsimServices c) {
		
		//add some EventHandler to the EventsManager after the services is started
		handler23 = new TTInOutflowEventHandler(Id.create("23", Link.class));
		handler27 = new TTInOutflowEventHandler(Id.create("27", Link.class));
		handler54 = new TTInOutflowEventHandler(Id.create("54", Link.class));
		handler58 = new TTInOutflowEventHandler(Id.create("58", Link.class));
		
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent e) {
				mfdHandler = new DgMfd(e.getServices().getScenario().getNetwork(), 1.0);
				e.getServices().getEvents().addHandler(handler23);
				e.getServices().getEvents().addHandler(handler27);
				e.getServices().getEvents().addHandler(handler54);
				e.getServices().getEvents().addHandler(handler58);
				
				e.getServices().getEvents().addHandler(mfdHandler);
			}
		});

		//write some output after each iteration
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent e) {
				handler23.iterationsEnds(e.getIteration());
				handler27.iterationsEnds(e.getIteration());
				handler54.iterationsEnds(e.getIteration());
				handler58.iterationsEnds(e.getIteration());

				if ( e.getIteration() % 10 == 0 ) {
					//					DgTravelTimeCalculatorChart ttcalcChart = new DgTravelTimeCalculatorChart((TravelTimeCalculator)e.getServices().getLinkTravelTimes());
					DgTravelTimeCalculatorChart ttcalcChart = null ;
					if ( true ) {
						throw new RuntimeException( "The above fails after I made TravelTimeCalculator final.  But I don't think that it can have worked before since some " +
												"other changes.  kai, feb'19" ) ;
					}

					ttcalcChart.setStartTime(0.0);
					ttcalcChart.setEndTime(3600.0 * 1.5);
					List<Id<Link>> list = new ArrayList<>();
					list.add(Id.create("23", Link.class));
					list.add(Id.create("34", Link.class));
					list.add(Id.create("45", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("27", Link.class));
					list.add(Id.create("78", Link.class));
					list.add(Id.create("85", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("54", Link.class));
					list.add(Id.create("43", Link.class));
					list.add(Id.create("32", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("58", Link.class));
					list.add(Id.create("87", Link.class));
					list.add(Id.create("72", Link.class));
					ttcalcChart.addLinkId(list);
					DgChartWriter.writeChart(e.getServices().getControlerIO().getIterationFilename(e.getIteration(), "ttcalculator"),
							ttcalcChart.createChart());

					DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getServices().getConfig().controler());
					chart.addCountEventHandler(handler23);
					chart.addCountEventHandler(handler27);
					chart.addCountEventHandler(handler54);
					chart.addCountEventHandler(handler58);
					DgChartWriter.writeChart(e.getServices().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
				
				
					mfdHandler.writeFile(e.getServices().getControlerIO().getIterationFilename(e.getIteration(), "mfd.txt"));
				}
			}
		});
  	//write some output at shutdown
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent e) {
				DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getServices().getConfig().controler());
				chart.addCountEventHandler(handler23, "Number of cars on link 23");
				chart.addCountEventHandler(handler27, "Number of cars on link 27");
				chart.addCountEventHandler(handler54, "Number of cars on link 54");
				chart.addCountEventHandler(handler58, "Number of cars on link 58");
				DgChartWriter.writeChart(e.getServices().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
			}
		});



	}


	public static void main(String[] args) {
		if (args == null || args.length == 0){
			new DgFigure9Runner().runFromConfig(null);
		}
		else {
			new DgFigure9Runner().runFromConfig(args[0]);
		}
	}


}
