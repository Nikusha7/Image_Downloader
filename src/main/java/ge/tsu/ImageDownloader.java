package ge.tsu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/*
 * Image downloader class that requires URL and download folder path.
 * Application uses JSoup library to parse root page, find images and download them.
 */
public class ImageDownloader implements Runnable {
    private static final Logger log = LogManager.getLogger(ImageDownloader.class);

    //Website url from where images will be downloaded
    private final URL url;
    //Folder where downloaded images will be saved
    private final Path downloadFolder;

    public ImageDownloader(URL url, Path downloadFolder) {
        this.url = url;
        this.downloadFolder = downloadFolder;
    }

    @Override
    public void run() {
        Document document = null;
        try {
            /*
            In general: The Document class in Jsoup represents an HTML document,
            and it provides methods for parsing and manipulating the HTML content.
            Jsoup.connect(url.toString()) - This creates a connection to the URL specified in the url variable.
            The url.toString() method is called to convert the URL object to a string.
            .get()-This method sends an HTTP GET request to the URL and retrieves the HTML content of the webpage as a Document object.
            */
            document = Jsoup.connect(url.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        document.select() - This method is used to select elements from the HTML document based on a CSS selector.
        In this case, the CSS selector is "img[src]", which selects all <img> elements that have a src attribute.
        Elements - This is a class in the Jsoup library that represents a collection of elements selected from an HTML document.
        The select() method returns an instance of the Elements class that contains all the selected elements.
        */
        Elements imageElements = document.select("img[src]");

        for (Element imageElement : imageElements) {
            //Getting img elements absolute source URLs
            String imageDownloadAbsUrl = imageElement.absUrl("src");
            log.info(String.format("Downloading image: %s", imageDownloadAbsUrl));
            try {
                downloadImage(getImageFilename(imageDownloadAbsUrl), imageDownloadAbsUrl);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }


    }

    /**
     * Reads image name and extension from a URL string.
     *
     * @param imageDownloadUrl URL string
     * @return Image filename with extension -> example: "puppy.jpg"
     */
    public String getImageFilename(String imageDownloadUrl) {
        /*
        If the input URL string for the getImageFilename method is something like "https://example.com/images/puppy.jpg"
        then the method will return "puppy.jpg", which is the filename part of the URL string.
        The substring() method is used to extract a substring of the input URL string
        starting from the index of the last forward slash character plus one,
        which gives the index of the first character of the filename.
        In this example, the index of the last forward slash character is 29, so adding one to that gives 30,
        which is the index of the first character of "puppy.jpg".
        The substring() method then extracts the substring starting from index 30 until the end of the input URL string,
        which is "puppy.jpg".
         */
        String fileName = imageDownloadUrl.substring(imageDownloadUrl.lastIndexOf("/") + 1);

        /*
        If fileName has no extension it means that extension was not specified in the url. So fileName only contains name of image.
        We will get extension from the following code, then check if it's a real extension and append it to fileName
        Result will be something like that "imageName.extension"  (e.g., "puppy.jpeg")"
        */
        Set<String> imageExtensions = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tiff", "raw", "jp2", "svg", "psd", "eps", "heif");

        String extension = getExtension(fileName);
        boolean nameHasExtension = imageExtensions.contains(extension.toLowerCase());

        if (!nameHasExtension) {
            URL url = null;
            try {
                url = new URL(imageDownloadUrl);
                URLConnection connection = url.openConnection();
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("image/")) {
                    String imageType = contentType.substring("image/".length());
                    return fileName + "." + imageType;
                } else {
                    log.debug("url does not point to an image");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileName;
    }

    //returns extension in lowercase, if fileName has no extension returns nothing
    private String getExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Downloads image to root output folder.
     *
     * @param fileName
     * @param url
     * @throws IOException
     */
    public void downloadImage(String fileName, String url) throws IOException {
        log.debug(String.format("FileName: %s", fileName));
        /*
        * .ignoreContentType(true) - This method tells Jsoup to ignore the content type of the response and treat it as a plain string.
        By default, Jsoup checks the content type of the response and tries to parse it as HTML.
        * .execute() - This method sends an HTTP GET request to the URL and retrieves the response.
        * Response response = - This assigns the retrieved response to a variable named response.
        The Response class in Jsoup represents an HTTP response, and it provides methods for accessing the response headers,
        status code, and response body.
         */
        Response response = Jsoup.connect(url).ignoreContentType(true).execute();
         /*
        We are receiving fileName which might contain illegal characters such as "?", it's okay for the name but not for the path.
        So we are replacing such characters with safe character (e.g., underscore "_")
         */
        String illegalChars = "?";
        StringBuilder sanitizedPath = new StringBuilder();
        for (char c : fileName.toCharArray()) {
            if (illegalChars.indexOf(c) == -1) {
                sanitizedPath.append(c);
            } else {
                sanitizedPath.append("_");
            }
        }

        /*
        .resolve(fileName) - This method is called on the downloadFolder object and creates a new Path object
        that represents the path to the downloaded image file.
        The fileName parameter is a String value that represents the name of the downloaded image file.
        */
        Path downloadImageFile = downloadFolder.resolve(String.valueOf(sanitizedPath));
        /*
        * downloadImageFile - This is a Path object that represents the path to the downloaded image file.
        * response.bodyAsBytes() - This method call retrieves the response body as a byte array.
        The Response class in Jsoup provides this method to allow easy access to the response body as bytes.
        * This method call writes the byte array representing the response body to the file specified by the downloadImageFile path.
        The Files.write() method in the Java NIO API provides a convenient way to write bytes to a file.

        * The downloadImageFile path specifies the location where the file should be written,
        and the response.bodyAsBytes() method call retrieves the contents of the image as bytes.
        The Files.write() method then writes the bytes to the file specified by the path.
        After the method completes, the image data is written to the file, and the file is saved to disk.
        */
        Files.write(downloadImageFile, response.bodyAsBytes());
        log.debug("Image downloaded!");
    }


}
