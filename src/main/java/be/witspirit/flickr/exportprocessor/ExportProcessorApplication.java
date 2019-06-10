package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ExportProcessorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ExportProcessorApplication.class, args);
    }


    private final MetadataService metadataService;
    private final ContentService contentService;

    public ExportProcessorApplication(MetadataService metadataService, ContentService contentService) {
        this.metadataService = metadataService;
        this.contentService = contentService;
    }

    @Override
    public void run(String... args) {

        Map<String, PhotoMeta> photoMetas = metadataService.loadPhotoMetadata();
        metadataService.log(photoMetas);


        List<Album> albums = metadataService.loadAlbums();
        metadataService.log(albums);


        Map<String, ContentDescriptor> contentById = contentService.loadDescriptors();
        contentService.log(contentById);

    }


}
