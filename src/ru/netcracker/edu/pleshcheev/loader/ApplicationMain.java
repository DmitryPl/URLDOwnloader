package ru.netcracker.edu.pleshcheev.loader;

import java.io.IOException;

public class ApplicationMain {

    public static void main(String... args) throws IOException  {
        String uri = "https://shpilenok.livejournal.com/304650.html";
        String dir = "test-downloader";
        boolean open = false;

        if (args.length > 0) {
            uri = args[0];
        }
        if (args.length > 1) {
            dir = args[1];
        }
        if (args.length > 2) {
            if (args[2].equals("true")) open = true;
            if (args[2].equals("false")) open = false;
        }
        DownloaderUtils.DownloadUtil(uri, dir, open);
    }
}
