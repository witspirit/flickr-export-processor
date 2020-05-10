package be.witspirit.flickr.exportprocessor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class PhotoDescriptor {
    private final String id;
    private final String name;
    private final String description;
    private final LocalDateTime dateTaken;
    private final SortedSet<String> tags = new TreeSet<>();

    private final ContentDescriptor contentDescriptor;

    private PhotoDescriptor(String id, String name, String description, LocalDateTime dateTaken, Set<String> tags, ContentDescriptor contentDescriptor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dateTaken = dateTaken;
        this.tags.addAll(tags);
        this.contentDescriptor = contentDescriptor;
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

    public SortedSet<String> getTags() {
        return tags;
    }

    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }


    public static class PhotoDescriptorBuilder {
        private String id;
        private String name;
        private String description;
        private LocalDateTime dateTaken;
        private ContentDescriptor contentDescriptor;
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

        public PhotoDescriptorBuilder dateTaken(LocalDateTime dateTaken) {
            this.dateTaken = dateTaken;
            return this;
        }

        public PhotoDescriptorBuilder contentDescriptor(ContentDescriptor contentDescriptor) {
            this.contentDescriptor = contentDescriptor;
            return this;
        }

        public PhotoDescriptorBuilder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public PhotoDescriptor build() {
            return new PhotoDescriptor(id, name, description, dateTaken, tags, contentDescriptor);
        }
    }
}
