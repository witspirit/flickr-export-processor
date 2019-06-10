package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Albums;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class ExportProcessorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ExportProcessorApplication.class, args);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${folder.metadata}")
    private String metadataFolder;

    @Override
    public void run(String... args) throws Exception {
        Path metadataPath = Path.of(metadataFolder);


        Stream<Path> metadataList = Files.list(metadataPath);
        Map<String, PhotoMeta> photoMetas = metadataList
                .filter(this::isPhotoMetadataFile)
                .map(this::parsePhotoMetadata)
                .collect(Collectors.toMap(PhotoMeta::getId, meta -> meta));

        photoMetas.values().forEach(meta -> {
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

        Path albumsJsonPath = metadataPath.resolve("albums.json");
        Albums albums = objectMapper.readValue(albumsJsonPath.toFile(), Albums.class);
        albums.getAlbums().forEach(a -> {
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
        return fileName.startsWith("photo_") && fileName.endsWith(".json");
    }

    private PhotoMeta parsePhotoMetadata(Path photoMetadataPath) {
        try {
            PhotoMeta photoMeta = objectMapper.readValue(photoMetadataPath.toFile(), PhotoMeta.class);
            System.out.println(photoMetadataPath+" : OK");
            return photoMeta;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Photo metadata from "+photoMetadataPath, e);
        }
    }
}
