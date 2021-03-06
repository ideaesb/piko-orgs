package org.ideademo.orgs.pages.org;


import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;
import org.ideademo.orgs.entities.Org;
import org.ideademo.orgs.services.util.PDFStreamResponse;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


public class ViewOrg
{
	
  private static Logger logger = Logger.getLogger(ViewOrg.class);	
	
  @PageActivationContext 
  @Property
  private Org entity;
  
  @Inject
  @Path("context:layout/images/image067.gif")
  private Asset logoAsset;

  @Inject
  private AssetSource assetSource;

  void onPrepareForRender()  {if(this.entity == null){this.entity = new Org();}}
  void onPrepareForSubmit()  {if(this.entity == null){this.entity = new Org();}}
  
  
  public StreamResponse onSubmit() 
  {

  	
      // step 1: creation of a document-object
      Document document = new Document();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try {
              // step 2:
              // we create a writer that listens to the document
              // and directs a PDF-stream to a file
              PdfWriter writer = PdfWriter.getInstance(document, baos);
              // step 3: we open the document
              document.open();
              
              java.awt.Image awtImage = Toolkit.getDefaultToolkit().createImage(logoAsset.getResource().toURL());
              if (awtImage != null)
              {
            	  com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(awtImage, null); 
            	  logo.scalePercent(50);
            	  if (logo != null) document.add(logo);
              }

              DateFormat formatter = new SimpleDateFormat
                      ("EEE MMM dd HH:mm:ss zzz yyyy");
                  Date date = new Date(System.currentTimeMillis());
                  TimeZone eastern = TimeZone.getTimeZone("Pacific/Honolulu");
                  formatter.setTimeZone(eastern);

              document.add(new Paragraph("Piko Partners & Programs " + formatter.format(date)));
              document.add(Chunk.NEWLINE);document.add(Chunk.NEWLINE);
              
              // create table, 2 columns
          		
          		String name = entity.getName();
          		String description = entity.getDescription();
          		String url = entity.getUrl();
          		String logoFile = entity.getLogo();
          		
                PdfPTable table = new PdfPTable(2);
                table.setWidths(new int[]{1, 4});
                table.setSplitRows(false);
                
                
  	
                
                
                PdfPCell nameTitle = new PdfPCell(new Phrase("Name")); 
                PdfPCell nameCell = new PdfPCell(new Phrase(name));
                
                nameTitle.setBackgroundColor(BaseColor.CYAN);  nameCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                
                table.addCell(nameTitle);  table.addCell(nameCell);          		          		
          		
          		if (StringUtils.isNotBlank(url))
          		{
            	  Anchor link = new Anchor(StringUtils.trimToEmpty(url)); link.setReference(StringUtils.trimToEmpty(url));
          		  table.addCell(new PdfPCell(new Phrase("Web")));  table.addCell(new PdfPCell(link));
          		}

          		if (StringUtils.isNotBlank(description))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Description")));  table.addCell(new PdfPCell(new Phrase(StringUtils.trimToEmpty(description))));
          		}
          		
          		
          		if (StringUtils.isNotBlank(logoFile))
          		{
          			try
          			{
  	          			Asset logoAss = assetSource.getClasspathAsset("context:layout/images/" + StringUtils.trimToEmpty(logoFile));
  	                    java.awt.Image logoImage = Toolkit.getDefaultToolkit().createImage(logoAss.getResource().toURL());
  	                    if (logoImage != null)
  	                    {
  	                  	  com.itextpdf.text.Image logo2 = com.itextpdf.text.Image.getInstance(logoImage, null); 
  	                  	  if (logo2 != null) 
  	                  	  {
  	                  		table.addCell(new PdfPCell(new Phrase("Logo")));
  	                  		table.addCell(logo2);
  	                  	  }
  	                    }
          			}
          			catch (Exception e)
          			{
          				logger.info("Unable to process logo file " + logoFile + " while rendering PDF of Orgs List");
          				logger.info(e);;
          			}
          		}
          		
          		
          		
          		
          	    // compile the disciples list
          		com.itextpdf.text.List types = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
          		if (entity.isFederal()) 
          		{
          			ListItem item = new ListItem("Federal"); types.add(item);
          		}
          		if (entity.isState()) 
          		{
          			ListItem item = new ListItem("State"); types.add(item);
          		}
          		if (entity.isLocal()) 
          		{
          			ListItem item = new ListItem("Local"); types.add(item);
          		}
          		if (entity.isInteragency()) 
          		{
          			ListItem item = new ListItem("Interagency"); types.add(item);
          		}
          		if (entity.isAcademic()) 
          		{
          			ListItem item = new ListItem("Academic"); types.add(item);
          		}
          		if (entity.isNgo()) 
          		{
          			ListItem item = new ListItem("Non-governmental Organization (NGO)"); types.add(item);
          		}
          		if (entity.isOtherPartnerType()) 
          		{
          			ListItem item = new ListItem("Unclassified/Other"); types.add(item);
          		}


          		if(types.size() > 0)
          		{
          		  PdfPCell typesCell = new PdfPCell(); typesCell.addElement(types);
          		  table.addCell(new PdfPCell(new Phrase("Types")));  table.addCell(typesCell);
          		}

          		
          		
          		if (StringUtils.isNotBlank(entity.getContact()))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Contact")));  table.addCell(new PdfPCell(new Phrase(StringUtils.trimToEmpty(entity.getContact()))));
          		}

          		document.add(table);
          		document.add(Chunk.NEWLINE);
      		
              
              
      } catch (DocumentException de) {
              logger.fatal(de.getMessage());
      }
      catch (IOException ie)
      {
    	 logger.warn("Could not find NOAA logo (likely)");
    	 logger.warn(ie);
      }

      // step 5: we close the document
      document.close();
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      return new PDFStreamResponse(bais,"PacisOrganization" + System.currentTimeMillis());
  }

  
  
}
