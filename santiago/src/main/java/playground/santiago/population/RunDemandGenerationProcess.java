package playground.santiago.population;

import java.io.File;

import org.apache.log4j.Logger;

import playground.santiago.landuse.RandomizeCoordinates;
import playground.santiago.utils.ModifyAgentAttributes;

/**
 * @author LeoCamus. 
 * args[0]= runsWorkingDir
 * args[1]= svnWorkingDir
 * args[2]= landUseShapeDir
 * args[3]= percentage
 * args[4]= standardDeviation
 *
 */

public class RunDemandGenerationProcess {
	
	private static final Logger log = Logger.getLogger(RunDemandGenerationProcess.class);

	public static void main(String[] args) {
		
		String runsWorkingDir = args[0];
		File runsWorkingDirFile = new File(runsWorkingDir);
		if(!runsWorkingDirFile.exists()) createDir(runsWorkingDirFile);
		
		String svnWorkingDir = args[1];
		File svnWorkingDirFile = new File(svnWorkingDir);
		if(!svnWorkingDirFile.exists()) createDir(svnWorkingDirFile);
		
		String landUseShapeDir = args[2];
		
		double percentage = Double.parseDouble(args[3]);
		int standardDeviation = Integer.parseInt(args[4]);
		
		DemandGeneration dg = new DemandGeneration(runsWorkingDir,svnWorkingDir,percentage);
		dg.run();
		
		RandomizeEndTimes ret = new RandomizeEndTimes(runsWorkingDir,svnWorkingDir,standardDeviation);
		ret.run();
		
		RandomizeCoordinates rc = new RandomizeCoordinates(runsWorkingDir,svnWorkingDir,landUseShapeDir);
		rc.run();
		
		ModifyAgentAttributes maa = new ModifyAgentAttributes(svnWorkingDir);
		maa.run();

	}
	
	
	static void createDir(File dirPathFile){
		log.info("Directory " + dirPathFile + " created: "+ dirPathFile.mkdirs());
	}

}
