package be.witspirit.flickr.exportprocessor.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExifCompositeValue {
    private String full;
    private String label;
    private String value;
    @JsonProperty("raw_value")
    private String rawValue;

    public String getFull() {
        return full;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
    }
}
