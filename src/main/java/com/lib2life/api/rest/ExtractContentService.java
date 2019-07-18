package com.lib2life.api.rest;

import com.lib2life.api.util.Utilities;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

// Sample URL for pdf:  "http://www.africau.edu/images/default/sample.pdf"
//                      "http://www.pdf995.com/samples/pdf.pdf"
// "https://batthyaneumblog.files.wordpress.com/2016/10/iacob_marza_transylvanica_la_bb_v_carti_de_la_cluj.pdf"
// "https://cdn4.libris.ro/userdocspdf/836/Matilda%20-%20Roald%20Dahl.pdf"
// "https://blogdescoala.files.wordpress.com/2014/09/39387610-legendele-olimpului-zeii.pdf"

@Path("/")
public class ExtractContentService {


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String displayStatus() {
        return "Server is working!";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("getContent")
    public Response getContent() {
        FileInputStream htmlFileStream;
        try {
            Utilities.LOGGER.log(Level.INFO, "[ExtractContentService] Getting HTML file from local storage ");

            htmlFileStream = new FileInputStream(Utilities.FILE_DIRECTORY + Utilities.getHtmlFileName());

        } catch (Exception e) {
            e.printStackTrace();

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(htmlFileStream)
                .build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    @Path("uploadFile")
    public Response convertPdfToHtml(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {
        FileInputStream htmlFileStream;

        try {
            /* Validate parameters */
            if (uploadedInputStream == null || fileDetail == null)
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("Invalid data")
                        .build();

            Utilities.LOGGER.log(Level.INFO, "[ExtractContentService] Getting uploaded file");

            /* Set the name of PDF and HTML files */
            Utilities.setPdfFileName(fileDetail.getFileName());
            Utilities.setHtmlFileName(Utilities.getPdfFileName().replace(".pdf", ".html"));

            /* Copy the uploaded file in FILE_DIRECTORY */
            Files.copy(
                    uploadedInputStream,
                    Paths.get(Utilities.FILE_DIRECTORY + Utilities.getPdfFileName()),
                    StandardCopyOption.REPLACE_EXISTING);

            Utilities.LOGGER.log(Level.INFO, "[ExtractContentService] PDF file copied. "
                    + "You can find it in " + Utilities.FILE_DIRECTORY + Utilities.getPdfFileName());

            uploadedInputStream.close();

            /* Convert from PDF to HTML */
            ExtractContentService.generateHtmlFromPdf(
                    Utilities.getPdfFileName(),
                    Utilities.getHtmlFileName());

            /* Save HTML file */
            htmlFileStream = new FileInputStream(Utilities.FILE_DIRECTORY + Utilities.getHtmlFileName());

        } catch (IOException e) {
            e.printStackTrace();

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error")
                    .build();
        }

        /* Send HTML file to Frontend */
        return Response
                .status(Response.Status.OK)
                .entity(htmlFileStream)
                .build();
    }

    /**
     * Utility function to convert a PDF file to HTML
     */
    private static void generateHtmlFromPdf(String pdfFileName, String htmlFileName) {
        Writer output;

        Utilities.LOGGER.log(Level.INFO, "[ExtractContentService] Begin conversion");

        try {
            /* Get the PDF file */
            PDDocument pdf = PDDocument.load(new File(Utilities.FILE_DIRECTORY + pdfFileName));

            /* Write the text from PDF to HTML file */
            output = new PrintWriter(Utilities.FILE_DIRECTORY + htmlFileName, "utf-8");
            new PDFDomTree().writeText(pdf, output);

            output.close();
            pdf.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Utilities.LOGGER.log(Level.INFO, "[ExtractContentService] Conversion finished. "
                + "The HTML file can be found in " + Utilities.FILE_DIRECTORY + htmlFileName);
    }
}
