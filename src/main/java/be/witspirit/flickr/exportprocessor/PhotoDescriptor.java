package be.witspirit.flickr.exportprocessor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PhotoDescriptor {
    private static final Set<String> IGNORED_TAGS = Set.of("flickrandroidapp:filter=none", "iphoneography", "instagram app", "uploaded:by=instagram", "Normal", "square", "square format");

    private final String id;
    private final String name;
    private final String description;
    private final LocalDateTime dateTaken;
    private final SortedSet<String> tags = new TreeSet<>();
    private final String flickrFilename;

    private final String fileExtension;
    private final String destinationFileName;

    private PhotoDescriptor(String id, String name, String description, LocalDateTime dateTaken, String flickrFilename, Set<String> tags) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dateTaken = dateTaken;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.flickrFilename = flickrFilename;
        this.fileExtension = extractFileExtension(flickrFilename);
        this.destinationFileName = deriveDestinationFileName();
    }

    private String extractFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.'));
    }


    private String deriveDestinationFileName() {
        String tagEncoding;
        if (tags.isEmpty()) {
            tagEncoding = "";
        } else {
            tagEncoding = "___#" +tags.stream().map(this::sanitizeTagForFilename).filter(Optional::isPresent).map(Optional::get).collect(Collectors.joining("#"));
        }
        // Add original flickr id to ensure uniqueness. Experience has shown that we have duplicate names for distinct photos !
        // It's not very readable, but it at least properly disambiguates in a potentially sensible way. Put it after the name, to not disrupt the more natural name ordering.
        return FileNameSanitizer.text(name)+ "+"+id + tagEncoding + fileExtension;
    }

    private Optional<String> sanitizeTagForFilename(String input) {
        if (IGNORED_TAGS.contains(input)) {
            return Optional.empty();
        }
        return Optional.of(input
                .replaceAll(" ", "-")
                .replaceAll("\\\\", "-")
                .replaceAll("/", "-")
        );
    }

    public static PhotoDescriptorBuilder builder() {
        return new PhotoDescriptorBuilder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDateTaken() {
        return dateTaken;
    }

    public String getFlickrFilename() {
        return flickrFilename;
    }

    public SortedSet<String> getTags() {
        return tags;
    }

    public String getDestinationFileName() {
        return destinationFileName;
    }

    public static class PhotoDescriptorBuilder {
        private String id;
        private String name;
        private String description;
        private String flickrFilename;
        private LocalDateTime dateTaken;
        private Set<String> tags;

        public PhotoDescriptorBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PhotoDescriptorBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PhotoDescriptorBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PhotoDescriptorBuilder flickrFilename(String flickrFilename) {
            this.flickrFilename = flickrFilename;
            return this;
        }

        public PhotoDescriptorBuilder dateTaken(LocalDateTime dateTaken) {
            this.dateTaken = dateTaken;
            return this;
        }

        public PhotoDescriptorBuilder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public PhotoDescriptor build() {
            return new PhotoDescriptor(id, name, description, dateTaken, flickrFilename, tags);
        }
    }
}
