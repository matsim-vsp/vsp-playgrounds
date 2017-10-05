package playground.santiago.utils;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.roadpricing.RoadPricingConfigGroup;
//TODO: no longer needed.
public class ConfigsForDepartureScenarios {

	private static final String INPUT_CONFIG = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/expanded_config_0.xml";

	private static String caseName="baseCase10pct";
	private static String stepName="Step2xA";
	private static double sampleSize = 0.1;

	private static String runDir = "../../../runs-svn/santiago/" + caseName + "/";



	public static void main(String[] args) {
		writeConfig2DepartureScenarios();

	}

	private static void writeConfig2DepartureScenarios(){

		Config inputConfig = ConfigUtils.loadConfig(INPUT_CONFIG);

		/*QSim*/		
		QSimConfigGroup qsim = inputConfig.qsim();
		//The capacity factor is equal to the sample size rate.
		qsim.setFlowCapFactor(sampleSize);
		//storageCapFactor obtained by expression proposed by Nicolai and Nagel (2013).
		double storageCapFactor;
		if(sampleSize==0.1){			
			storageCapFactor = Math.ceil(((sampleSize / (Math.pow(sampleSize, 0.25))))*100)/100;
		}else if(sampleSize==0.01){
			storageCapFactor = Math.ceil(((sampleSize / (Math.pow(sampleSize, 0.25))))*1000)/1000;
		} else {
			throw new RuntimeException("There are only two sample size rates: 1% and 10%");
		}

		qsim.setStorageCapFactor(storageCapFactor);

		/*Counts*/
		CountsConfigGroup counts = inputConfig.counts();
		counts.setCountsScaleFactor(Math.pow(sampleSize,-1));


		/*ASCs values: depends on the plansFile used (i.e. on sampleSize)*/
		if(sampleSize==0.1){
			//Values from calibration - v2b (10%)
			PlanCalcScoreConfigGroup planCalc = inputConfig.planCalcScore();
			planCalc.getModes().get(TransportMode.car).setConstant((double) 0.8383);
			planCalc.getModes().get(TransportMode.pt).setConstant((double) -1.6764);
			planCalc.getModes().get(TransportMode.walk).setConstant((double) -0.2536);
		}else if(sampleSize==0.01){
			//Values from calibration - v2a (1%)
			PlanCalcScoreConfigGroup planCalc = inputConfig.planCalcScore();
			planCalc.getModes().get(TransportMode.car).setConstant((double) 1.2652);
			planCalc.getModes().get(TransportMode.pt).setConstant((double) -0.6953);
			planCalc.getModes().get(TransportMode.walk).setConstant((double) -1.1828);
		} else {
			throw new RuntimeException("There are only two sample size rates: 1% and 10%");
		}


		/*Paths*/		
		PlansConfigGroup plans = inputConfig.plans();
		plans.setInputFile(runDir + "inputFor" + stepName + "/.xml.gz");

		if(sampleSize==0.1){
			plans.setInputPersonAttributeFile(runDir + "inputFor" + stepName + "/expandedAgentAttributes.xml");
		}else if(sampleSize==0.01){
			plans.setInputPersonAttributeFile(runDir + "inputFor" + stepName + "/sampledAgentAttributes.xml");
		} else {
			throw new RuntimeException("There are only two sample size rates: 1% and 10%");
		}


		ControlerConfigGroup cc = inputConfig.controler();
		cc.setOutputDirectory(runDir + "outputOf" + stepName + "/" );

		counts.setInputFile(runDir + "inputFor" + stepName + "/countsCorrected.xml" );

		NetworkConfigGroup net = inputConfig.network();
		net.setInputFile(runDir + "inputFor" + stepName + "/network_merged_cl.xml.gz" );


		TransitConfigGroup transit = inputConfig.transit();
		transit.setTransitScheduleFile(runDir + "inputFor" + stepName + "/transitschedule_simplified.xml" );
		transit.setVehiclesFile(runDir + "inputFor" + stepName + "/transitvehicles.xml" );

		RoadPricingConfigGroup rpcg = ConfigUtils.addOrGetModule(inputConfig, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);

		if(caseName.equals("baseCase1pct")||caseName.equals("baseCase10pct")){

			rpcg.setTollLinksFile(runDir + "inputFor" + stepName + "/gantries.xml");

		} else if (caseName.equals("policyRuns/1pct")||caseName.equals("policyRuns/10pct")){
			
			if (stepName.substring(0,stepName.length()-1).equals("StepOuter")){	
				
				rpcg.setTollLinksFile(runDir + "inputFor" + stepName + "/outerCordonWithTolledTollways.xml");
				
			} else if (stepName.substring(0,stepName.length()-1).equals("StepTriangle")){
				
				rpcg.setTollLinksFile(runDir + "inputFor" + stepName + "/triangleCordonWithTolledTollways.xml");
				
			} else {
				
				throw new RuntimeException("There are only two policy step names: Outer and Triangle");
			}

		} else {
			
			throw new RuntimeException("There are only two cases: baseCase1pct/10pct and policyRuns/1pct/10pct");
		}

		new ConfigWriter(inputConfig).write(runDir + "config" + stepName + ".xml");

	}

}
