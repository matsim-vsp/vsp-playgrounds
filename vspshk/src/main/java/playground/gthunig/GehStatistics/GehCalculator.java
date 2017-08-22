package playground.gthunig.GehStatistics;

/**
 * @author gthunig on 22.08.2017.
 */
public class GehCalculator {

    public static double calculateGEH(double simValue, double countValue) {
        return Math.sqrt((2*Math.pow(simValue - countValue, 2))/(simValue + countValue));
    }

}
