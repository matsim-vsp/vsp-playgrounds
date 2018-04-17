package searchacceleration;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConstantReplanningParameters implements ReplanningParameterProvider {

	private final double meanLambda;

	private final double delta;

	public ConstantReplanningParameters(final double meanLambda, final double delta) {
		this.meanLambda = meanLambda;
		this.delta = delta;
	}

	@Override
	public double getMeanLambda(int iteration) {
		return this.meanLambda;
	}

	@Override
	public double getDelta(int iteration) {
		return this.delta;
	}
}
