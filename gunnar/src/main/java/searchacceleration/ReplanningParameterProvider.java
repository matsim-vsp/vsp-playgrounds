package searchacceleration;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ReplanningParameterProvider {

	public double getMeanLambda(final int iteration);

	public double getDelta(final int iteration);

}
