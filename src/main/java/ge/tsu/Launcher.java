package ge.tsu;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.nio.file.Paths;


public class Launcher {
    public static void main(String[] args) {

        URL url = getUrl(args[0]);
        Path downloadFolder = getDownloadFolder(args[1]);


        ImageDownloader imageDownloader = new ImageDownloader(url, downloadFolder);
        imageDownloader.run();

    }

    private static URL getUrl(String urlString) {
        try {
            return URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Invalid URL passed: %s\n", urlString));
        }
    }

    private static Path getDownloadFolder(String downloadFolderPathString) {
        Path downloadFolderPath = Paths.get(downloadFolderPathString);
        if (Files.notExists(downloadFolderPath)) {
            try {
                //if such a directory doesn't exist we create one
                Files.createDirectories(downloadFolderPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (!Files.isDirectory(downloadFolderPath)) {
            throw new IllegalArgumentException(String.format("Not a directory: %s\n", downloadFolderPathString));
        }
        return downloadFolderPath;
    }


}
