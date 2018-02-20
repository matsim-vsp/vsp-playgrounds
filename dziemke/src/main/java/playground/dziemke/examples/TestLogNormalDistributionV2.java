package playground.dziemke.examples;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

public class TestLogNormalDistributionV2 {
	private static final Logger LOG = Logger.getLogger( TestLogNormalDistributionV2.class ) ;

	
	private double normalization ;
	private double sigma ;

	private Random random;

	// "cache" of the random value
	private double logNormalRnd;

	private static int normalisationWrnCnt = 0;

	
	public static void main(String[] args) {
		TestLogNormalDistributionV2 exLogNormalDistribution = new TestLogNormalDistributionV2();
		
		int numberOfNumbers = 10000;
		double sum = 0;
		
		for (int i = 0; i < numberOfNumbers; i++) {
			double logNormalRandomNumber = exLogNormalDistribution.getLogNormalRandomNumber();
			LOG.info("Log-normal Random number is = " + logNormalRandomNumber);
			sum += logNormalRandomNumber;
		}
		LOG.info("Average is = " + sum / numberOfNumbers);
		
	}
	
	
	public TestLogNormalDistributionV2() {
		this.sigma = 3.;
		double normalization = 1;
		if (sigma != 0.) {
			normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
			if (normalisationWrnCnt < 10) {
				normalisationWrnCnt++;
				LOG.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization);
			}
		}
		this.normalization = normalization;
	}

	
	public double getLogNormalRandomNumber() {
		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;
		
		if ( sigma != 0. ) {
				logNormalRnd = Math.exp( sigma * random.nextGaussian() ) ;
				logNormalRnd *= normalization ;
				// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
				// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

				/* The argument is something like this:<ul> 
				 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
				 * <li> The mean of this is exp( mu + sigma^2/2 ) .  
				 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
				 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
				 * </ul>
				 * Should be tested. kai, jan'14 */
			
		} else {
			logNormalRnd = 1. ;
		}
		return logNormalRnd;
	}
}