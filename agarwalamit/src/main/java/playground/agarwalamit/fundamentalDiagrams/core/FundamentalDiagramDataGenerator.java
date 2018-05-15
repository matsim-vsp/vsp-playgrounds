/* *********************************************************************** *
 * project: org.matsim.*
 * DreieckNModes													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author amit after ssix
 */

public class FundamentalDiagramDataGenerator {

	public static final Logger LOG = Logger.getLogger(FundamentalDiagramDataGenerator.class);

	public static final double MAX_ACT_END_TIME = 1800.;

	private String runDir ;

	static boolean isUsingLiveOTFVis = false;

	private int flowUnstableWarnCount [] ;
	private int speedUnstableWarnCount [] ;

	private PrintStream writer;
	private final Scenario scenario;

	private static GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;

	static FDNetworkGenerator fdNetworkGenerator;

	private Map<String, Double> mode2PCUs = null;

	private Integer[] startingPoint;
	private Integer [] maxAgentDistribution;
	private Integer [] stepSize;

	private String[] travelModes;
	private Double[] modalShareInPCU;

	private FundamentalDiagramConfigGroup fundamentalDiagramConfigGroup;

	private List<AbstractModule> abstractModules = new ArrayList<>();

	public FundamentalDiagramDataGenerator( final Scenario scenario){
		fundamentalDiagramConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FundamentalDiagramConfigGroup.class);
		fdNetworkGenerator = new FDNetworkGenerator(fundamentalDiagramConfigGroup);
		fdNetworkGenerator.createNetwork(scenario);
		this.scenario = scenario;
	}

	public void run(){
		checkForConsistencyAndInitialize();
		setUpConfig();

		openFileAndWriteHeader(runDir+"/data.txt");

		if(fundamentalDiagramConfigGroup.isRunningDistribution()){
			parametricRunAccordingToDistribution();
		} else parametricRunAccordingToGivenModalSplit();

		new ConfigWriter(scenario.getConfig()).write(this.runDir+"/output_config.xml");
		new NetworkWriter(scenario.getNetwork()).write(this.runDir+"/output_network.xml");
		new VehicleWriterV1(scenario.getVehicles()).writeFile(this.runDir+"/output_vehicles.xml");

		closeFile();
	}

	private void checkForConsistencyAndInitialize(){
		this.runDir = scenario.getConfig().controler().getOutputDirectory();
		if(runDir==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");

		createLogFile();

		if(fundamentalDiagramConfigGroup.getReduceDataPointsByFactor() != 1) {
			LOG.info("===============");
			LOG.warn("Number of modes for each mode type in FD will be reduced by a factor of "+fundamentalDiagramConfigGroup.getReduceDataPointsByFactor()+". This will not change the traffic dynamics.");
			if (scenario.getConfig().qsim().getTrafficDynamics()== QSimConfigGroup.TrafficDynamics.queue) LOG.warn("Make sure this is what you want because it will be more likely to have less or no points in congested regime in absence of queue model with holes.");
			LOG.info("===============");
		}

		if(fundamentalDiagramConfigGroup.isWritingEvents()) Log.warn("This will write one event file corresponding to each iteration and thus ");

		travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);

		if (scenario.getVehicles().getVehicleTypes().isEmpty()) {
			if (travelModes.length==1 && travelModes [0].equals("car")) {
				LOG.warn("No vehicle information is provided for "+this.travelModes[0]+". Using default vehicle (i.e. car) with maximum speed same as" +
						"allowed speed on the link.");

				VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
            	car.setPcuEquivalents(1.0);
            	car.setMaximumVelocity( fundamentalDiagramConfigGroup.getTrackLinkSpeed() );
            	scenario.getVehicles().addVehicleType(car);
			} else {
				throw new RuntimeException("Vehicle type information for modes "+ Arrays.toString(travelModes)+" is not provided. Aborting...");
			}
		}

		mode2PCUs = scenario.getVehicles()
							.getVehicleTypes()
							.values()
							.stream()
							.collect(Collectors.toMap(v -> v.getId().toString(), VehicleType::getPcuEquivalents));

		flowUnstableWarnCount = new int [travelModes.length];
		speedUnstableWarnCount = new int [travelModes.length];

		if (this.modalShareInPCU==null) {
			LOG.warn("No modal split is provided for mode(s) : " + Arrays.toString(this.travelModes)+". Using equla modal split in PCU.");
			this.modalShareInPCU = new Double[this.travelModes.length];
			Arrays.fill(this.modalShareInPCU, 1.0);
		} else if (this.modalShareInPCU.length != this.travelModes.length) {
			LOG.warn("Number of modes is not equal to the provided modal share (in PCU). Running for equal modal share");
			this.modalShareInPCU = new Double[this.travelModes.length];
			Arrays.fill(this.modalShareInPCU, 1.0);
		}
		this.fundamentalDiagramConfigGroup.setModalShareInPCU(
				Arrays.stream(this.modalShareInPCU).map(String::valueOf).collect(Collectors.joining(","))
		);
		
		if (scenario.getConfig().controler().getOverwriteFileSetting().equals(OverwriteFileSetting.deleteDirectoryIfExists)) {
			LOG.warn("Overwrite file setting is set to "+scenario.getConfig().controler().getOverwriteFileSetting() 
					+ ", which will also remove the fundamental diagram data file. Setting it back to "+OverwriteFileSetting.overwriteExistingFiles);
			scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		}
	}

	private void setUpConfig() {
		// required if using controler
		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(home);

		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(work);

		scenario.getConfig().controler().setLastIteration(0);
		scenario.getConfig().controler().setCreateGraphs(false);
		scenario.getConfig().controler().setDumpDataAtEnd(false);

		scenario.getConfig().qsim().setEndTime(100.0*3600.); // qsim should not go beyond 100 hrs it stability is not achieved.

		// following is necessary, in order to achieve the data points at high density
		if(this.travelModes.length==1 && this.travelModes[0].equals("car")) scenario.getConfig().qsim().setStuckTime(60.);
		else  if (this.travelModes.length==1 && this.travelModes[0].equals("truck")) scenario.getConfig().qsim().setStuckTime(180.);

		if ( scenario.getConfig().network().isTimeVariantNetwork() ) {
			Network netImpl = scenario.getNetwork();
			netImpl.getFactory().setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		}
	}

	private void parametricRunAccordingToGivenModalSplit(){
		//	Creating minimal configuration respecting modal split in PCU and integer agent numbers
		List<Double> pcus = Arrays.stream(travelModes)
								  .map(this.mode2PCUs::get)
								  .collect(Collectors.toList());

		List<Integer> minSteps = Arrays.stream(modalShareInPCU).map(modalSplit -> (int) (modalSplit * 100))
									   .collect(Collectors.toList());

		int commonMultiplier = 1;
		for (int i=0; i<travelModes.length; i++){
			double pcu = pcus.get(i);
			//heavy vehicles
			if ( (pcu>1) && (minSteps.get(i)%pcu != 0) ){
				double lcm = getLCM((int) pcu, minSteps.get(i));
				commonMultiplier = (int) (commonMultiplier * lcm/minSteps.get(i) );
			}
		}
		for (int i=0; i<travelModes.length; i++){
			minSteps.set(i, (int) (minSteps.get(i)*commonMultiplier/pcus.get(i)));
		}
		int pgcd = getGCDOfList(minSteps);
		for (int i=0; i<travelModes.length; i++){
			minSteps.set(i, minSteps.get(i)/pgcd);
		}

		if(minSteps.size()==1){
			minSteps.set(0, 1);
		}

		if(fundamentalDiagramConfigGroup.getReduceDataPointsByFactor()!=1) {
			for(int index=0;index<minSteps.size();index++){
				minSteps.set(index, minSteps.get(index)*fundamentalDiagramConfigGroup.getReduceDataPointsByFactor());
			}
		}

		//set up number of Points to run.
		double cellSizePerPCU = scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fundamentalDiagramConfigGroup.getTrackLinkLanes() / cellSizePerPCU;
		double sumOfPCUInEachStep = IntStream.range(0, travelModes.length)
											 .mapToDouble(index -> minSteps.get(index) * this.mode2PCUs.get(travelModes[index]))
											 .sum();

		int numberOfPoints = (int) Math.ceil( networkDensity / sumOfPCUInEachStep ) + 5 ;

		List<List<Integer>> pointsToRun = new ArrayList<>();
		for ( int m=1; m<numberOfPoints; m++ ){
			List<Integer> pointToRun = new ArrayList<>();
			for (int i=0; i<travelModes.length; i++){
				pointToRun.add(minSteps.get(i)*m);
			}
			LOG.info("Number of Agents - \t"+pointToRun);
			pointsToRun.add(pointToRun);
		}

		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			LOG.info("===============");
			LOG.info("Going into run where number of Agents are - \t"+pointToRun);
			Log.info("Further, " + (pointsToRun.size() - i) +" combinations will be simulated.");
			LOG.info("===============");
			this.singleRun(pointToRun);
		}
	}

	private void parametricRunAccordingToDistribution(){

		this.startingPoint = new Integer [travelModes.length];
		this.stepSize = new Integer [travelModes.length];

		Arrays.fill(this.stepSize, this.fundamentalDiagramConfigGroup.getReduceDataPointsByFactor());
		Arrays.fill(this.startingPoint, 1);

		this.maxAgentDistribution = new Integer [travelModes.length];
		double cellSizePerPCU = this.scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fundamentalDiagramConfigGroup.getTrackLinkLanes() / cellSizePerPCU;

		IntStream.range(0, travelModes.length)
				 .forEach(index -> this.maxAgentDistribution[index] = (int) Math.floor(networkDensity / this.mode2PCUs.get(
						 travelModes[index])) + 1);

		List<List<Integer>> pointsToRun = this.createPointsToRun();
		for (List<Integer> pointToRun : pointsToRun) {
			double density = IntStream.range(0, travelModes.length)
									  .mapToDouble(index -> pointToRun.get(index) * this.mode2PCUs.get(travelModes[index]))
									  .sum();

			if (density <= networkDensity + 5) {
				LOG.info("Going into run " + pointToRun);
				this.singleRun(pointToRun);
			}
		}
	}

	private List<List<Integer>> createPointsToRun() {

		int numberOfPoints = IntStream.range(0, travelModes.length)
									  .map(jj -> (int) Math.floor((maxAgentDistribution[jj] - startingPoint[jj]) / stepSize[jj]) + 1)
									  .reduce(1, (a, b) -> a * b);

		if(numberOfPoints > 1000) LOG.warn("Total number of points to run is "+numberOfPoints+". This may take long time. "
				+ "For lesser time to get the data reduce data points by some factor.");

		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(Arrays.asList(maxAgentDistribution), Arrays.asList(stepSize), startingPoint);
		List<List<Integer>> pointsToRun = new ArrayList<>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[maxAgentDistribution.length];
			System.arraycopy(iterationModule.getPoint(), 0, newPoint, 0, newPoint.length);
			pointsToRun.add(Arrays.asList(newPoint));
			LOG.info("Just added point "+ Arrays.toString(iterationModule.getPoint()) +" to the collection.");
			if (i<numberOfPoints-1){
				iterationModule.addPoint();
			}
		}
		return pointsToRun;
	}

	public void addOverridingModules(AbstractModule abstractModule) {
		this.abstractModules.add( abstractModule );
	}

	private void singleRun(List<Integer> pointToRun) {
		Population population = scenario.getPopulation();

		//remove existing persons and person attributes
		population.getPersons().clear();
		population.getPersonAttributes().clear();

		Map<String, TravelModesFlowDynamicsUpdator> mode2FlowData = Arrays.stream(travelModes).map(mode -> {
			VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
			return new TravelModesFlowDynamicsUpdator(vehicleType, travelModes.length,
				fdNetworkGenerator.getFirstLinkIdOfTrack(), fdNetworkGenerator.getLengthOfTrack());
		}).collect(Collectors.toMap(m->m.getModeId().toString(), m->m));


		for (int i=0; i<travelModes.length; i++){
			for (int ii = 0; ii < pointToRun.get(i); ii++){
				Id<Person> personId = Id.createPersonId(population.getPersons().size());
				Person person = population.getFactory().createPerson(personId);
				// a blank plan is necessary otherwise VspPlansCleaner will throw a NPE. Amit Apr'18
				person.addPlan(population.getFactory().createPlan());
				population.addPerson(person);
				population.getPersonAttributes().putAttribute(personId.toString(), FDQSimProvider.PERSON_MODE_ATTRIBUTE_KEY, travelModes[i]);
			}
			mode2FlowData.get(travelModes[i]).setnumberOfAgents(pointToRun.get(i));
		}


		Controler controler = new Controler( scenario ) ;

		globalFlowDynamicsUpdator = new GlobalFlowDynamicsUpdator(
				mode2FlowData,
				fdNetworkGenerator.getFirstLinkIdOfTrack() ,
				fdNetworkGenerator.getLengthOfTrack());
		PassingEventsUpdator passingEventsUpdator = new PassingEventsUpdator(
				scenario.getConfig().qsim().getSeepModes(),
				fdNetworkGenerator.getFirstLinkIdOfTrack(),
				fdNetworkGenerator.getLastLinkIdOfTrack(),
				fdNetworkGenerator.getLengthOfTrack());
		
		EventWriterXML eventWriter = null ;
		if(fundamentalDiagramConfigGroup.isWritingEvents()){
			String eventsDir = runDir+"/events/";
			if (! new File(eventsDir).exists() ) new File(eventsDir).mkdir();
			eventWriter = new EventWriterXML(eventsDir+"/events"+pointToRun.toString()+".xml");
		}

		final EventWriterXML eventsWriter = eventWriter;

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(FDNetworkGenerator.class).toInstance(fdNetworkGenerator); // required for FDTrackMobsimAgent
				bind(GlobalFlowDynamicsUpdator.class).toInstance(globalFlowDynamicsUpdator); // required for FDTrackMobsimAgent

				addEventHandlerBinding().toInstance(globalFlowDynamicsUpdator);

				if(travelModes.length > 1)	addEventHandlerBinding().toInstance(passingEventsUpdator);

				if(fundamentalDiagramConfigGroup.isWritingEvents()){
					addEventHandlerBinding().toInstance(eventsWriter);
				}

				bind(PrintStream.class).toInstance(writer);
			}
		});

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindMobsim().toProvider(FDQSimProvider.class);
			}
		});

		this.abstractModules.forEach(controler::addOverridingModule);

		controler.run();

		if(! scenario.getConfig().controler().getSnapshotFormat().isEmpty()) {
			//remove and renaming of the files which are generated from controler and not required.
			updateTransimFileNameAndDir(pointToRun);
		}
		cleanOutputDir();

		boolean stableState = true;
		for(int index=0;index<travelModes.length;index++){
			String veh = travelModes[index];
			if(!mode2FlowData.get(veh).isFlowStable())
			{
				stableState = false;
				int existingCount = flowUnstableWarnCount[index]; existingCount++;
				flowUnstableWarnCount[index] = existingCount;
				LOG.warn("Flow stability is not reached for travel mode "+ veh
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + flowUnstableWarnCount[index]+ "th warning.");
			}
			if(!mode2FlowData.get(veh).isSpeedStable())
			{
				stableState = false;
				int existingCount = speedUnstableWarnCount[index]; existingCount++;
				speedUnstableWarnCount[index] = existingCount;
				LOG.warn("Speed stability is not reached for travel mode "+ veh
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + speedUnstableWarnCount[index]+ "th warning.");
			}
		}
		if(!globalFlowDynamicsUpdator.isPermanent()) stableState=false;

		// sometimes higher density points are also executed (stuck time), to exclude them density check.
		double cellSizePerPCU = scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fundamentalDiagramConfigGroup.getTrackLinkLanes() / cellSizePerPCU;

		if(stableState){
			double globalLinkDensity = globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity();
			if(globalLinkDensity > networkDensity / 3 + 10 ) stableState =false; //+10; since we still need some points at max density to show zero speed.
		}

		if( stableState ) {
			writer.print("\n"); //always stats with a new line

			writer.format("%d\t",globalFlowDynamicsUpdator.getGlobalData().getnumberOfAgents());
			for (String travelMode : travelModes) {
				writer.format("%d\t", mode2FlowData.get(travelMode).getnumberOfAgents());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", mode2FlowData.get(travelMode).getPermanentDensity());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentFlow());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", mode2FlowData.get(travelMode).getPermanentFlow());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentAverageVelocity());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", mode2FlowData.get(travelMode).getPermanentAverageVelocity());
			}

			if( travelModes.length > 1 ) {

				writer.format("%.2f\t", passingEventsUpdator.getNoOfCarsPerKm());

				writer.format("%.2f\t", passingEventsUpdator.getAvgBikesPassingRate());
			}

//			if (fundamentalDiagramConfigGroup.isUsingDynamicPCU() ) {
//				for (String travelMode : travelModes) {
//					String str = String.valueOf( scenario.getVehicles().getVehicleTypes().get(Id.create(travelMode,VehicleType.class)).getPcuEquivalents() );
//					writer.print(str + "\t");
//				}
//			}
		}

		if(fundamentalDiagramConfigGroup.isWritingEvents()) {
			assert eventWriter != null;
			eventWriter.closeFile();
		}
	}

	private void updateTransimFileNameAndDir(List<Integer> runningPoint) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		//Check if Transim veh dir exists, if not create it
		if(! new File(outputDir+"/TransVeh/").exists() ) new File(outputDir+"/TransVeh/").mkdir();
		//first, move T.veh.gz file
		String sourceTVehFile = outputDir+"/ITERS/it.0/0.T.veh.gz";
		String targetTVehFilen = outputDir+"/TransVeh/T_"+runningPoint.toString()+".veh.gz";
		try {
			Files.move(new File(sourceTVehFile).toPath(), new File(targetTVehFilen).toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("File not found.");
		}
	}

	private void cleanOutputDir(){
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File(outputDir+"/ITERS/").toPath());
		IOUtils.deleteDirectoryRecursively(new File(outputDir+"/tmp/").toPath());
		new File(outputDir+"/logfile.log").delete();
		new File(outputDir+"/logfileWarningsErrors.log").delete();
		new File(outputDir+"/scorestats.txt").delete();
		new File(outputDir+"/modestats.txt").delete();
		new File(outputDir+"/stopwatch.txt").delete();
		new File(outputDir+"/traveldistancestats.txt").delete();
	}

	private void openFileAndWriteHeader(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("n \t");
		Arrays.stream(travelModes).forEach(travelMode -> writer.print("n_" + travelMode + "\t"));

		writer.print("k \t");
		Arrays.stream(travelModes).forEach(travelMode -> writer.print("k_" + travelMode + "\t"));

		writer.print("q \t");
		Arrays.stream(travelModes).forEach(travelMode -> writer.print(("q_" + travelMode) + "\t"));

		writer.print("v \t");
		Arrays.stream(travelModes).forEach(travelMode -> writer.print(("v_" + travelMode) + "\t"));

		if( travelModes.length > 1 ) {
			writer.print("noOfCarsPerkm \t");

			writer.print("avgBikePassingRatePerkm \t");
		}
//		if (fundamentalDiagramConfigGroup.isUsingDynamicPCU() ) {
//			Arrays.stream(travelModes).forEach(travelMode -> writer.print(("pcu_" + travelMode) + "\t"));
//		}
//		writer.print("\n");
	}

	private void closeFile() {
		writer.close();
	}

	private int getGCD(int a, int b){
		if(b==0) return a;
		else return getGCD(b, a%b);
	}

	private int getLCM(int a, int b){
		return a*b/getGCD(a,b);
	}

	private int getGCDOfList(List<Integer> list){
		int i, a, b, gcd;
		a = list.get(0);
		gcd = 1;
		for (i = 1; i < list.size(); i++){
			b = list.get(i);
			gcd = a*b/getLCM(a, b);
			a = gcd;
		}
		return gcd;
	}

	private void createLogFile(){
		PatternLayout layout = new PatternLayout();
		String conversionPattern = " %d %4p %c{1} %L %m%n";
		layout.setConversionPattern(conversionPattern);
		FileAppender appender;
		String filename = runDir + "/fdlogfile.log";
		try {
			appender = new FileAppender(layout, filename,false);
		} catch (IOException e1) {
			throw new RuntimeException("File "+filename+" not found.");
		}
		LOG.addAppender(appender);
	}
}
