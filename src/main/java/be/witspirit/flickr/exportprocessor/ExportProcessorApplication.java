package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class ExportProcessorApplication /* implements CommandLineRunner */ {
    private static final Logger LOG = LoggerFactory.getLogger(ExportProcessorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ExportProcessorApplication.class, args);
    }


    private final MetadataService metadataService;
    private final ContentService contentService;
    private final StructuringService structuringService;

    public ExportProcessorApplication(MetadataService metadataService, ContentService contentService, StructuringService structuringService) {
        this.metadataService = metadataService;
        this.contentService = contentService;
        this.structuringService = structuringService;
    }

    // @Override
    public void run(String... args) {

//        Map<String, PhotoMeta> photoMetas = metadataService.loadPhotoMetadata();
//        metadataService.log(photoMetas);


        List<Album> albums = metadataService.loadAlbums();
//        metadataService.log(albums);


        Map<String, ContentDescriptor> contentById = contentService.loadDescriptors();
//        contentService.log(contentById);

        Set<String> processedPhotoIds = new HashSet<>();
        for (Album album : albums) {
//             processedPhotoIds.addAll(structuringService.copyIntoAlbumStructure(album, contentById));
        }

        LOG.info("Processed {}/{} content items", processedPhotoIds.size(), contentById.keySet().size());
    }


}
