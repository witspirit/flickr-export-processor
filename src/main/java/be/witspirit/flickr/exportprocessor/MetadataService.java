package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.Albums;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MetadataService.class);

    private static final Pattern PHOTO_METADATA_PATTERN = Pattern.compile("photo_(\\d+).json");

    private final ObjectMapper objectMapper;
    private final Path metadataPath;

    public MetadataService(ObjectMapper objectMapper, @Value("${folder.metadata}") String metadataFolder) {
        this.objectMapper = objectMapper;
        this.metadataPath = Path.of(metadataFolder);
    }

    public Map<String, PhotoMeta> loadPhotoMetadata() {
        try {
            return Files.list(metadataPath)
                    .filter(this::isPhotoMetadataFile)
                    .map(this::parsePhotoMetadata)
                    .collect(Collectors.toMap(PhotoMeta::getId, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list "+metadataPath, e);
        }
    }

    public Set<String> getPhotoMetaIds() {
        try {
            return Files.list(metadataPath)
                    .filter(this::isPhotoMetadataFile)
                    .map(path -> {
                        Matcher matcher = PHOTO_METADATA_PATTERN.matcher(path.getFileName().toString());
                        matcher.matches();
                        return matcher.group(1);
                    })
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list "+metadataPath, e);
        }
    }

    public PhotoMeta getMetadata(String photoId) {
        Path photoMetaPath = metadataPath.resolve("photo_" + photoId + ".json");
        return parsePhotoMetadata(photoMetaPath);
    }

    public void log(Map<String, PhotoMeta> photoMetadata) {
        photoMetadata.values().forEach(meta -> {
            System.out.printf("%s : %s @ %s : %s\n",
                    meta.getId(),
                    meta.getName(),
                    meta.getDateTaken(),
                    meta.getTags().stream().map(Tag::getTag).collect(Collectors.toList())
            );
            meta.getExif().forEach((key, value) -> {
                System.out.printf("  %s -> %s\n", key, value);
            });
        });
    }


    public List<Album> loadAlbums() {
        try {
            return objectMapper.readValue(metadataPath.resolve("albums.json").toFile(), Albums.class).getAlbums();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Albums", e);
        }
    }

    public void log(List<Album> albums) {
        albums.forEach(a -> {
            System.out.printf("%s : %s (%s) @ %s (%s): %s\n",
                    a.getId(),
                    a.getTitle(),
                    a.getPhotoCount(),
                    a.getCreated(),
                    a.getLastUpdated(),
                    a.getPhotoIds()
            );
        });
    }



    private boolean isPhotoMetadataFile(Path path) {
        String fileName = path.getFileName().toString();
        return PHOTO_METADATA_PATTERN.matcher(fileName).matches();
        // return fileName.startsWith("photo_") && fileName.endsWith(".json");
    }

    private PhotoMeta parsePhotoMetadata(Path photoMetadataPath) {
        try {
            if (Files.exists(photoMetadataPath)) {
                PhotoMeta photoMeta = objectMapper.readValue(photoMetadataPath.toFile(), PhotoMeta.class);
                // LOG.debug(photoMetadataPath+" : OK");
                return photoMeta;
            } else {
                LOG.warn("No metadata file found at {}", photoMetadataPath);
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Photo metadata from "+photoMetadataPath, e);
        }
    }
}
