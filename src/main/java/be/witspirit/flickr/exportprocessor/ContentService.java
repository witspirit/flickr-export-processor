package be.witspirit.flickr.exportprocessor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentService {
    private Path contentPath;

    public ContentService(@Value("${folder.photos}") String contentFolder) {
        this.contentPath = Path.of(contentFolder);
    }

    public Map<String, ContentDescriptor> loadDescriptors() {
        try {
            return Files.list(contentPath)
                    .map(path -> {
                        try {
                            return new ContentDescriptor(path);
                        } catch (IllegalArgumentException e) {
                            System.err.println(e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(ContentDescriptor::getId, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list "+contentPath, e);
        }
    }

    public void log(Map<String, ContentDescriptor> contentById) {
        contentById.values().forEach(System.out::println);
    }
}
