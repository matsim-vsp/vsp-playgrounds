package playground.vsp.cadyts.marginals;

import java.util.Map;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import org.matsim.api.core.v01.Id;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

/*package*/ class ModalDistanceSimResultsContainerImpl implements SimResults<ModalDistanceBinIdentifier> {

    private static final long serialVersionUID = 1L;

    private BeelineDistanceCollector beelineDistanceCollector;

    ModalDistanceSimResultsContainerImpl(final BeelineDistanceCollector beelineDistanceCollector) {
        this.beelineDistanceCollector = beelineDistanceCollector;
    }

    @Override
    public double getSimValue(final ModalDistanceBinIdentifier modalBinIdentifier, final int low, final int high, final TYPE type) {
        DistanceBin bin = this.beelineDistanceCollector.getOutputDistanceDistribution().getModalBinToDistanceBin().get(
                modalBinIdentifier.getId());
        if (bin==null) return 0.;
        else return bin.getCount() * modalBinIdentifier.getScalingFactor();
    }

    @Override
    public String toString() {
        final String MODE = "mode: ";
        final String distanceRange = "; distanceRange: ";
        final String VALUES = "; values:";
        final char TAB = '\t';
        final char RETURN = '\n';

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(MODE+TAB);
        stringBuffer.append(distanceRange+TAB);
        stringBuffer.append(VALUES+RETURN);

        DistanceDistribution distanceDistribution = this.beelineDistanceCollector.getOutputDistanceDistribution();
        for (Map.Entry<Id<ModalDistanceBinIdentifier>, DistanceBin> entry : distanceDistribution.getModalBinToDistanceBin().entrySet()) {
            if (entry.getValue().getCount() > 0) {
                    stringBuffer.append(entry.getKey().toString()+TAB);
                    stringBuffer.append(entry.getValue().getCount()+RETURN);
            }
        }
        return stringBuffer.toString();
    }

}