package playground.dziemke.examples;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

public class TestLogNormalDistribution {
	private static final Logger LOG = Logger.getLogger(TestLogNormalDistribution.class);

	public static void main(String[] args) {
		
		double average = 0;
		int numberOfDiceRolls = 10000;
		int normalisationWrnCnt = 0;
		double sigma = 0.2;
		
		for (int i = 0; i < numberOfDiceRolls; i++) {
			double normalization = 1;
			if (sigma != 0.) {
				normalization = 1. / Math.exp(sigma * sigma / 2);
				if (normalisationWrnCnt < 10) {
					normalisationWrnCnt++;
					LOG.info("sigma: " + sigma + " -- resulting normalization: " + normalization);
				}
			}			
	
			Random random = new Random();
			double logNormalRnd = Math.exp( sigma * random.nextGaussian() ) ;
			logNormalRnd *= normalization;
			
			LOG.info("logNormalRnd: " + logNormalRnd);
			average += logNormalRnd;
		}
		
		average = average / numberOfDiceRolls;
		LOG.info("average = " + average);
	}

}
