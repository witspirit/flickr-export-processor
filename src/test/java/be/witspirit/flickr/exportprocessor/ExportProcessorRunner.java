package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExportProcessorRunner {

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private StructuringService structuringService;

    @Test
    public void checkForPhotosInMultipleAlbums() {
        List<Album> albums = metadataService.loadAlbums();

        Map<String, List<Album>> photoIdToAlbum = new HashMap<>();
        for (Album album : albums) {
                for (String photoId : album.getPhotoIds()) {
                    List<Album> appearedInAlbums = photoIdToAlbum.computeIfAbsent(photoId, id -> new ArrayList<>());
                    appearedInAlbums.add(album);
                }
        }

        Map<String, List<Album>> photosAppearingInMultipleAlbums = photoIdToAlbum.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (photosAppearingInMultipleAlbums.isEmpty()) {
            System.out.println("There are no photo's appearing in multiple albums. Analyzed "+albums.size()+" albums.");
        } else {
            System.out.println("The following photo's appear in multiple Albums: ");
            for (String photoId : photosAppearingInMultipleAlbums.keySet()) {
                System.out.printf("%-10s : %s\n", photoId, photosAppearingInMultipleAlbums.get(photoId).stream().map(Album::getTitle).collect(Collectors.joining(", ")));
            }
        }
    }

    @Test
    public void doWeHaveMetadataForEachContentItemAndViceVersa() {
        Map<String, ContentDescriptor> photoIdToContentItem = contentService.loadDescriptors();
        // Map<String, PhotoMeta> photoIdToMetadata = metadataService.loadPhotoMetadata();
        Set<String> photoMetaIds = metadataService.getPhotoMetaIds();

        Set<String> photosInContentWithoutMetadata = new HashSet<>(photoIdToContentItem.keySet());
        photosInContentWithoutMetadata.removeAll(photoMetaIds);

        Set<String> photosWithMetadataButMissingContent = new HashSet<>(photoMetaIds);
        photosWithMetadataButMissingContent.removeAll(photoIdToContentItem.keySet());

        System.out.printf("Found %d content items and %d metadata items\n", photoIdToContentItem.size(), photoMetaIds.size());
        System.out.printf("%d/%d content items without metadata\n", photosInContentWithoutMetadata.size(), photoIdToContentItem.size());
        System.out.printf("%d/%d metadata items without content\n", photosWithMetadataButMissingContent.size(), photoMetaIds.size());

        System.out.println("Content items without metadata:");
        for (String photoId : photosInContentWithoutMetadata) {
            System.out.println(photoId + " : " + photoIdToContentItem.get(photoId));
        }

        System.out.println("Metadata items without content:");
        for (String photoId : photosWithMetadataButMissingContent) {
            System.out.println(photoId + " : " + metadataService.getMetadata(photoId).getName());
        }

    }

    @Test
    public void describeTargetStructure() {
        Map<String, ContentDescriptor> photoIdToContentDescriptor = contentService.loadDescriptors();
        List<Album> albums = metadataService.loadAlbums();

        List<AlbumDescriptor> albumDescriptors = structuringService.deriveAlbumStructure(albums, photoIdToContentDescriptor);

        System.out.println("Target Structure:");
        for (AlbumDescriptor album : albumDescriptors) {
            System.out.println(album.getAlbumPath());
            for (PhotoDescriptor photo : album.getPhotos()) {
                System.out.println("  " + photo.getDestinationFileName());
            }
        }
    }

}
