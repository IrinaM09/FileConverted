package com.lib2life.api.rest;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.lib2life.api.util.Utilities;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.fit.pdfdom.PDFDomTree;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;

@Path("/")
public class ExtractImagesService {

    public static int imagesNo = 0;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("getImages")
    public Response getImages() {
        FileInputStream htmlFileStream = null;
        SaveImagesInPdf printer;

        try {
            /* Get PDF file */
            PDDocument document = PDDocument.load(new File(Utilities.FILE_DIRECTORY + Utilities.getPdfFileName()));

            printer = new SaveImagesInPdf();


            for (PDPage page : document.getPages()) {
                imagesNo++;
                printer.processPage(page);
            }

            document.close();

            Utilities.LOGGER.log(Level.INFO, "[ExtractImagesService] Extracted " + imagesNo + " images");

            ExtractImagesService.addImagesToPdf();

            /* Save HTML file */
            htmlFileStream = new FileInputStream(Utilities.FILE_DIRECTORY + "images-" + Utilities.getHtmlFileName());

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

    private static void addImagesToPdf() throws Exception {
        PDDocument doc = new PDDocument();

        File dir = new File("images");
        File[] directoryListing = dir.listFiles();
        int no = 0;
        int max = directoryListing.length;
        System.out.println("DIR: " + directoryListing.length);

        if (directoryListing != null) {
            for (File crtImage : directoryListing) {

                if (no++ > max) break;
                System.out.println("current: " + no + " crt image: " + crtImage.getName());

                PDPage page = new PDPage();
                doc.addPage(page);

                String image = "images/" + crtImage.getName();
                String out = "images/out-" + crtImage.getName();
                ExtractImagesService.resize(image, out, 300, 300);
                PDImageXObject pdImage = PDImageXObject.createFromFile(out, doc);

                PDPageContentStream contents = new PDPageContentStream(doc, page);
                PDRectangle mediaBox = page.getMediaBox();

                float startX = (mediaBox.getWidth() - pdImage.getWidth()) / 2;
                float startY = (mediaBox.getHeight() - pdImage.getHeight()) / 2;
                contents.drawImage(pdImage, 0, 0, startY, startX);

                contents.close();
                new File(out).delete();
                crtImage.delete();

                Utilities.LOGGER.log(Level.INFO, "[ExtractImagesService] adding...");
            }

            Utilities.LOGGER.log(Level.INFO, "[ExtractImagesService] Done adding images to PDF");
        }
        doc.save(Utilities.FILE_DIRECTORY + "images-" + Utilities.getPdfFileName());

        /* Write the text from PDF to HTML file */
        Writer output = new PrintWriter(Utilities.FILE_DIRECTORY + "images-" + Utilities.getHtmlFileName(), "utf-8");
        new PDFDomTree().writeText(doc, output);

        doc.close();
        output.close();
    }


    public static void resize(String inputImagePath,
                              String outputImagePath, int scaledWidth, int scaledHeight)
            throws IOException {
        // reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath
                .lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }
}


class SaveImagesInPdf extends PDFStreamEngine {

    public SaveImagesInPdf() {
    }

    public int imageNumber = 1;

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();

        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xobject;

                // save image to local
                BufferedImage bImage = image.getImage();

                ImageIO.write(bImage, "PNG", new File("images/" + "image_" + imageNumber + ".png"));

                System.out.println("Added new image");

                imageNumber++;

            } else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xobject;
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }
}