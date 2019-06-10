package be.witspirit.flickr.exportprocessor.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Album {
    @JsonProperty("photo_count")
    private String photoCount;
    private String id;
    private String title;
    private String description;

    private String created;
    @JsonProperty("last_updated")
    private String lastUpdated;

    @JsonProperty("photos")
    private List<String> photoIds = new ArrayList<>();

    public String getPhotoCount() {
        return photoCount;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCreated() {
        return created;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public List<String> getPhotoIds() {
        return photoIds;
    }
}
