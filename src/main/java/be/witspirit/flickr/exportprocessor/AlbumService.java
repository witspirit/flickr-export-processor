package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.Albums;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class AlbumService {
    private ObjectMapper objectMapper;

    public AlbumService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Album> loadAlbums(Path albumsPath) {
        try {
            return objectMapper.readValue(albumsPath.toFile(), Albums.class).getAlbums();
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
}
