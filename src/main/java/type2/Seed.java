package type2;

import java.util.Map;

public class Seed {
    private Double label;
    private Map<Integer, Integer> features;

    public Seed(Double label, Map<Integer, Integer> features) {
        this.label = label;
        this.features = features;
    }

    public Double getLabel() {
        return label;
    }

    public void setLabel(Double label) {
        this.label = label;
    }

    public Map<Integer, Integer> getFeatures() {
        return features;
    }

    public void setFeatures(Map<Integer, Integer> features) {
        this.features = features;
    }
}
