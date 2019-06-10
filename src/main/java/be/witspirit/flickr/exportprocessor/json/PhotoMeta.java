package be.witspirit.flickr.exportprocessor.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class PhotoMeta {
    private String id;
    private String name;
    private String description;
    @JsonProperty("date_taken")
    private String dateTaken;

    private Map<String, ExifValue> exif = new LinkedHashMap<>();

    private List<Tag> tags = new ArrayList<>();


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public Map<String, ExifValue> getExif() {
        if (exif == null) {
            return Collections.emptyMap(); // Special case, triggered by the EmptyArray as NullObject deserialization, which we require for exif
        }
        return exif;
    }

    public List<Tag> getTags() {
        return tags;
    }
}
