package be.witspirit.flickr.exportprocessor.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExifValue {
    private String full; // Full namespaced EXIF Field Name
    private String label; // Display field name (e.g. no namespace)
    private String value; // Display value (e.g. with units or conversions applied)
    private String rawValue; // The raw value (e.g. no fluff, numeric values)

    public ExifValue(String value) {
        this.full = null;
        this.label = null;
        this.value = value;
        this.rawValue = value;
    }

    public ExifValue(
            @JsonProperty("full") String full,
            @JsonProperty("label") String label,
            @JsonProperty("value") String value,
            @JsonProperty("raw_value") String rawValue) {
        this.full = full;
        this.label = label;
        this.value = value;
        this.rawValue = rawValue;
    }

    public boolean isDetailed() {
        return full != null;
    }

    public boolean isSimple() {
        return full == null;
    }

    /**
     * @return Full namespaced EXIF Field Name (or null if Simple value)
     */
    public String getFull() {
        return full;
    }

    /**
     * @return Display field name (e.g. no namespace) (or null if Simple value)
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return Display value (e.g. with units or conversions applied)
     */
    public String getValue() {
        return value;
    }

    /**
     * @return The raw value (e.g. no fluff, numeric values)
     */
    public String getRawValue() {
        return rawValue;
    }

    @Override
    public String toString() {
        if (isSimple()) {
            return value;
        }
        return String.format("{ full: %s, label: %s, value: %s, raw_value: %s}", full, label, value, rawValue);
    }
}
