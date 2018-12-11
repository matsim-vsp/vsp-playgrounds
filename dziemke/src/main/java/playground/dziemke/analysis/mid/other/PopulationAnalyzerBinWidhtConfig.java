package playground.dziemke.analysis.mid.other;

public class PopulationAnalyzerBinWidhtConfig {

    private int binWidthDuration_min = 1;
    private int binWidthTime_h = 1;
    private int binWidthDistance_km = 1;
    private int binWidthSpeed_km_h = 1;

    public int getBinWidthDuration_min() {
        return binWidthDuration_min;
    }

    public void setBinWidthDuration_min(int binWidthDuration_min) {
        this.binWidthDuration_min = binWidthDuration_min;
    }

    public int getBinWidthTime_h() {
        return binWidthTime_h;
    }

    public void setBinWidthTime_h(int binWidthTime_h) {
        this.binWidthTime_h = binWidthTime_h;
    }

    public int getBinWidthDistance_km() {
        return binWidthDistance_km;
    }

    public void setBinWidthDistance_km(int binWidthDistance_km) {
        this.binWidthDistance_km = binWidthDistance_km;
    }

    public int getBinWidthSpeed_km_h() {
        return binWidthSpeed_km_h;
    }

    public void setBinWidthSpeed_km_h(int binWidthSpeed_km_h) {
        this.binWidthSpeed_km_h = binWidthSpeed_km_h;
    }
}
