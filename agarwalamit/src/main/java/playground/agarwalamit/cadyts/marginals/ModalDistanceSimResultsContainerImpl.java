package playground.agarwalamit.cadyts.marginals;

import java.util.Map;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import org.matsim.api.core.v01.Id;
import playground.agarwalamit.cadyts.marginals.prep.DistanceBin;
import playground.agarwalamit.cadyts.marginals.prep.DistanceDistribution;
import playground.agarwalamit.cadyts.marginals.prep.ModalBinIdentifier;

/*package*/ class ModalDistanceSimResultsContainerImpl implements SimResults<ModalBinIdentifier> {

    private static final long serialVersionUID = 1L;

    private BeelineDistanceCollector beelineDistanceCollector;
    private final double countsScaleFactor;

    ModalDistanceSimResultsContainerImpl(final BeelineDistanceCollector beelineDistanceCollector, final double countsScaleFactor) {
        this.beelineDistanceCollector = beelineDistanceCollector;
        this.countsScaleFactor = countsScaleFactor;
    }

    @Override
    public double getSimValue(final ModalBinIdentifier modalBinIdentifier, final int low, final int high, final TYPE type) {
        DistanceBin bin = this.beelineDistanceCollector.getOutputDistanceDistribution().getModalBinToDistanceBin().get(
                modalBinIdentifier.getId());
        if (bin==null) return 0.;
        else return bin.getCount();
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
        for (Map.Entry<Id<ModalBinIdentifier>, DistanceBin> entry : distanceDistribution.getModalBinToDistanceBin().entrySet()) {
            if (entry.getValue().getCount() > 0) {
                    stringBuffer.append(entry.getKey().toString()+TAB);
                    stringBuffer.append(entry.getValue().getCount()+RETURN);
            }
        }
        return stringBuffer.toString();
    }

}