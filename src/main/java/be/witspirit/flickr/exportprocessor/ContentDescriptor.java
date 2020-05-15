package be.witspirit.flickr.exportprocessor;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDescriptor {
    private static final Pattern CONTENT_PATTERN_1 = Pattern.compile("^(\\d{11})_(.{10})(_o)?\\.(.+)$");
    private static final Pattern CONTENT_PATTERN_2 = Pattern.compile("^(.+)_(\\d+)(_o)?\\.(.+)$");

    private Path path;

    private String id;
    private String name;
    private String extension;

    public ContentDescriptor(Path path) {
        this.path = path;

        String fileName = path.getFileName().toString();

        Matcher pattern1Matcher = CONTENT_PATTERN_1.matcher(fileName);
        if (pattern1Matcher.matches()) {
            this.name = pattern1Matcher.group(2);
            this.id = pattern1Matcher.group(1);
            this.extension = pattern1Matcher.group(4);
        } else {
            Matcher pattern2Matcher = CONTENT_PATTERN_2.matcher(fileName);
            if (pattern2Matcher.matches()) {
                this.name = pattern2Matcher.group(1);
                this.id = pattern2Matcher.group(2);
                this.extension = pattern2Matcher.group(4);
            } else {
                throw new IllegalArgumentException(path + " does not represent a Content filename");
            }
        }
    }

    public Path getPath() {
        return path;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return String.format("%-11s : %-60s (%s) @ %s", id, name, extension, path);
    }
}
