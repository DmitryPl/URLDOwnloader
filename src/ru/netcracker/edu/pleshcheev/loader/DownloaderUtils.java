package ru.netcracker.edu.pleshcheev.loader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DownloaderUtils {

    private DownloaderUtils() {
        throw new IllegalStateException("No instances, please");
    }

    /**
     * A utility for saving a web page file or a file from any Internet sites
     * to a local file by connecting to them via high-level protocols (HTTP, FTP, etc.).
     * @param  uri  URL of some resource
     * @param  dir if directory where the file will be saved
     * if dir equals null file will be saved in resent directory
     * @param  open if open equals true resource will be opened in your browser
     */
    public static void DownloadUtil(String uri, String dir, boolean open) throws IOException {
        dir = checkDir(dir, uri);
        Path path = DownloaderUtils.download(DownloaderUtils.toURL(uri), dir);
        if (DownloaderUtils.isHtml(path)) {
            DownloaderUtils.downloadHtml(DownloaderUtils.toURL(uri), path);
            System.out.println("is html -> True");
        } else {
            System.out.println("is html -> False");
        }
        if (open) fileView(path);
    }

    /**
     * The utility for checking the charset in the html file
     * @return Charset of html file
     * If it is not html file or charset is not specified in the file return null
     *
     * @param  path to this HTML file
     */
    public static String CharsetUtil(String path) throws IOException{
        Path thisFile = Paths.get(path).toAbsolutePath();
        if (!isHtml(thisFile)) {
            return null;
        }
        Document document = Jsoup.parse(thisFile.toFile(), "UTF-8");

        for (Element meta : document.getElementsByTag("meta")) {
            String tmp1 = meta.attr("charset");
            if (tmp1 != null && !tmp1.equals("")) {
                return tmp1.toUpperCase();
            }
            String tmp2 = meta.attr("content");
            if (tmp2 != null && tmp2.startsWith("text/html;charset=") && !tmp2.equals("")) {
                return tmp2.substring("text/html;charset=".length(), tmp2.length()).toUpperCase();
            }
        }
        return null;
    }

    private static String checkDir(String dir, String uri) throws IOException {
        if (dir == null) {
            return null;
        }
        String name = extractName(URI.create(uri).toURL());
        if (Files.exists(Paths.get(dir, name).getParent())){
            String yes = "y";
            String no  = "n";
            System.out.printf("The folder exists.\n" +
                    "Do you want to use a unique name or want to overwrite a file?\n");
            int flag_of_work = 100;
            while (flag_of_work-- > 0) {
                System.out.printf("Press \'%s\' to use unique name or press \'%s\'\n", yes, no);
                Scanner in = new Scanner(System.in);
                String input = in.nextLine();
                if (input.equals(yes)) {
                    int i = 1;
                    while (Files.exists(Paths.get(dir + "-" + Integer.toString(i), name).getParent())) {
                        i++;
                    }
                    dir = dir + "-" + Integer.toString(i);
                    return dir;
                }

                if (input.equals(no)) {
                    return dir;
                }
            }
        }
        return dir;
    }

    private static String toURL(String uri) {
        if (uri.startsWith("//")) return "https:" + uri;
        if (uri.startsWith("/")) return uri;
        if (uri.equals("")) return uri;
        return (uri.startsWith("https") || uri.startsWith("http") ? uri : "https://" + uri);
    }

    private static Path download(String uri) throws IOException {
        return download(uri, null);
    }

    private static Path download(String uri, String dir) throws IOException {
        URL url = URI.create(uri).toURL();
        return download(url, dir, extractName(url));
    }

    private static Path download(URL url, String dir, String name) throws IOException {
        try (InputStream is = url.openStream()) {
            Path path;
            if (dir != null) {
                path = Paths.get(dir, name);
                Files.createDirectories(path.getParent());
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
            } else {
                path = Paths.get(name);
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
            }

            return path;
        }
    }

    private static String extractName(URL url) {
        final String path = url.getPath();

        System.out.println("Path: " + path);

        if (path == null || path.isEmpty()) {
            return "index.html";
        }

        if (path.equals("/")) {
            return "unknown";
        }

        if (path.contains("/")) {
            String[] tokens = path.split("/");
            return tokens[tokens.length - 1];
        }

        return path;
    }

    private static String extractMimeType(String path) throws IOException {
        return extractMimeType(Paths.get(path));
    }

    private static String extractMimeType(Path path) throws IOException {
        return Files.probeContentType(path);
    }

    private static boolean isHtml(Path path) throws IOException {
        String mimeType;
        if ("text/html".equals(mimeType = extractMimeType(path))) {
            return true;
        }

        if (mimeType != null) {
            return false;
        }

        try (BufferedReader br = Files.newBufferedReader(path)) {
            int i = 0;
            while (i++ < 2) {
                if (br.readLine().toUpperCase().replaceAll(" ", "").startsWith("<!DOCTYPEHTML")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void downloadHtml(String originUri, Path path) throws IOException {
        Map<String, Path> filesDownloaded = new HashMap<>();

        Document document = Jsoup.parse(Files.newInputStream(path), "UTF-8", originUri);

        for (Element img : document.getElementsByTag("img")) {
            String uri = img.attr("src");
            Path imgPath;
            uri = toURL(uri);
            if (null == (imgPath = filesDownloaded.get(uri))) {
                imgPath = fileDownload(path, uri);
                filesDownloaded.put(uri, imgPath);
            }

            if (imgPath != null) {
                img.attr("src", imgPath.toString());
            }
        }
        for (Element link : document.getElementsByTag("link")) {
            String uri = link.attr("href");
            Path linkPath;
            uri = toURL(uri);
            if (null == (linkPath = filesDownloaded.get(uri))) {
                linkPath = fileDownload(path, uri);
                filesDownloaded.put(uri, linkPath);
            }

            if (linkPath != null) {
                link.attr("href", linkPath.toString());
            }
        }

        Files.write(path, document.toString().getBytes(Charset.forName("UTF-8")), StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Path fileDownload(Path path, String uri) throws IOException {
        try {
            Path parent = path.getParent();
            if (parent == null) {
                return download(uri, Paths.get(".") + "/files");
            }
            return download(uri, path.getParent().toString() + "/files");
        } catch (Exception e) {
            boolean reThrow = true;

            if (e.getMessage() != null && e.getMessage().startsWith("android-app")) reThrow = false;

            if (e.getMessage() != null && e.getMessage().startsWith("unknown protocol:")) reThrow = false;

            if (e.getMessage() != null && e.getMessage().startsWith("Expected authority at")) reThrow = false;

            if (e.getMessage() != null && e.getMessage().startsWith("Server returned HTTP response code: 403 for URL: ")) reThrow = false;

            if (e.getMessage() != null && e.getMessage().startsWith("Server returned HTTP response code: 401 for URL: ")) reThrow = false;

            if (e.getMessage() != null && e.getMessage().startsWith("URI is not absolute")) reThrow = false;

            if (e instanceof MalformedURLException) reThrow = false;

            if (e instanceof FileAlreadyExistsException) reThrow = false;

            if (reThrow) throw e;

            return null;
        }
    }

    private static void fileView(Path path) throws IOException {
        Desktop.getDesktop().browse(path.toUri());
    }
}
