package com.athaydes.osgirun.demo.pdfbox;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Scanner;

public class PdfBoxDemo implements BundleActivator {

    @Override
    public void start( BundleContext context ) throws Exception {
        System.out.println( "Where should the PDF file be saved?" );
        Scanner scanner = new Scanner( System.in );
        try {
            String fileName = scanner.nextLine();
            if ( fileName == null || fileName.trim().isEmpty() ) {
                System.out.println( "No file name given." );
            } else {
                System.out.println( "Creating PDF file at " + fileName );
                createPdf( fileName );
            }
            System.out.println( "Re-start the installing-non-bundles bundle to create another PDF" );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        System.out.println( "Stopping PdfBoxDemo bundle" );
    }

    private void createPdf( String fileName ) {
        try ( PDDocument doc = new PDDocument() ) {
            PDPage page = new PDPage();

            doc.addPage( page );
            PDFont font = PDType1Font.HELVETICA_BOLD;

            PDPageContentStream content = new PDPageContentStream( doc, page );
            content.beginText();
            content.setFont( font, 12 );
            content.moveTextPositionByAmount( 100, 700 );
            content.drawString( "Hello from Osgi-Run Gradle plugin!" );

            content.endText();
            content.close();
            doc.save( fileName + ( fileName.endsWith( ".pdf" ) ? "" : ".pdf" ) );
            System.out.println( "Created file " + doc );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}