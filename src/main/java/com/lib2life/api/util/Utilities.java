package com.lib2life.api.util;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class contains utilies such as:
 * constants,
 * PDF file,
 * HTML file etc.
 */
public class Utilities {
    public static final Logger LOGGER = Grizzly.logger(HttpServer.class);
    public static final String FILE_DIRECTORY = "files/";
    public static final Pattern TOC_PAGE_PATTERN = Pattern.compile("\\d+\\s*$", Pattern.MULTILINE);
    private static String pdfFileName = "";
    private static String htmlFileName = "";

    public static String getPdfFileName() {
        return pdfFileName;
    }

    public static void setPdfFileName(String pdfFileName) {
        Utilities.pdfFileName = pdfFileName;
    }

    public static String getHtmlFileName() {
        return htmlFileName;
    }

    public static void setHtmlFileName(String htmlFileName) {
        Utilities.htmlFileName = htmlFileName;
    }


}
