package com.lib2life.api.rest;

import com.lib2life.api.util.Utilities;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.fit.pdfdom.PDFDomTree;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Stream;

@Path("/")
public class ExtractTableOfContentService {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("getToc")
    public Response getTableOfContent() {
        Integer tocPageNo;
        FileInputStream htmlFileStream;
        PDDocument pdfFile;

        try {
            pdfFile = PDDocument.load(new File(Utilities.FILE_DIRECTORY + Utilities.getPdfFileName()));


            /* Find the page number with Table of Content */
            tocPageNo = getTocStartPageNo(pdfFile.getPages());

            if (tocPageNo != null) {
               Utilities.LOGGER.log(Level.INFO, "[ExtractTableOfContentService] Found page of TOC at " + tocPageNo);

                /* Create a PDF file only with Table of Content pages */
                PDDocument tocPdfFile = new PDDocument();
                tocPdfFile.addPage(pdfFile.getPages().get(tocPageNo));
                tocPdfFile.save(Utilities.FILE_DIRECTORY + "toc-" + Utilities.getPdfFileName());

                /* Write the text from PDF to HTML file */
                Writer output = new PrintWriter(Utilities.FILE_DIRECTORY + "toc-" + Utilities.getHtmlFileName(), "utf-8");
                new PDFDomTree().writeText(tocPdfFile, output);

                output.close();
                tocPdfFile.close();
                pdfFile.close();

                /* Save HTML file */
                htmlFileStream = new FileInputStream(Utilities.FILE_DIRECTORY + "toc-" + Utilities.getHtmlFileName());

                return Response
                        .status(Response.Status.OK)
                        .entity(htmlFileStream)
                        .build();
            }
            pdfFile.close();

        } catch (Exception e) {
            e.printStackTrace();

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity("No Table of Content Found")
                .build();
    }

    /**
     * Utility function that returns the page number of Table of Content
     */
    private static Integer getTocStartPageNo(PDPageTree pages) {
        String pageText;
        try {
            /* Iterate through the whole PDF file */
            for (int i = 0; i < pages.getCount(); i++) {
                /* Get the content of current page */
                pageText = ExtractTableOfContentService.getPageText(pages.get(i));

                /* Check if current page matches the start of a Table of Content */
                if (pageText != null) {
                    if (pageText.toLowerCase().contains("cuprins") || pageText.toLowerCase().contains("tabla de materii")) {

                        /* Check if current page has multiple lines ending with numbers */
                        if (countPattern(pageText) >= 3) {
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Utility function that returns the content of a page as in String
     */
    private static String getPageText(PDPage page) {
        PDFTextStripper textStripper;
        PDDocument document;

        try {
            textStripper = new PDFTextStripper();
            document = new PDDocument();

            document.addPage(page);
            document.close();

            return textStripper.getText(document);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Utility function that returns the number of matches of TOC_PAGE_PATTERN for multiple lines.
     * This pattern of a line ending in a number is found in Table of Content pages.
     */
    private static Integer countPattern(String pageText) {
        Matcher matcher = Utilities.TOC_PAGE_PATTERN.matcher(pageText);

        return Stream.iterate(0, i -> i + 1)
                .filter(i -> !matcher.find())
                .findFirst()
                .orElse(null);
    }
}
