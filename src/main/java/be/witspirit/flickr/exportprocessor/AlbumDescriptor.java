package be.witspirit.flickr.exportprocessor;

import java.nio.file.Path;
import java.util.List;

public class AlbumDescriptor {
    private final String id;
    private final String name;
    private final List<PhotoDescriptor> photos;
    private Path albumPath;

    public AlbumDescriptor(String id, String name, List<PhotoDescriptor> photos, Path albumPath) {
        this.id = id;
        this.name = name;
        this.photos = photos;
        this.albumPath = albumPath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<PhotoDescriptor> getPhotos() {
        return photos;
    }

    public Path getAlbumPath() {
        return albumPath;
    }
}
