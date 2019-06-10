package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ExportProcessorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ExportProcessorApplication.class, args);
    }

    @Value("${folder.metadata}")
    private String metadataFolder;

    @Value("${folder.photos}")
    private String photosFolder;

    @Autowired
    private PhotoMetadataService photoMetadataService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private ContentService contentService;

    @Override
    public void run(String... args) {
        Path metadataPath = Path.of(metadataFolder);


        Map<String, PhotoMeta> photoMetas = photoMetadataService.loadPhotoMetadata(metadataPath);
        photoMetadataService.log(photoMetas);


        List<Album> albums = albumService.loadAlbums(metadataPath.resolve("albums.json"));
        albumService.log(albums);


        Path photosPath = Path.of(photosFolder);
        Map<String, ContentDescriptor> contentById = contentService.loadDescriptors(photosPath);
        contentService.log(contentById);

    }


}
