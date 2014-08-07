package org.ideademo.orgs.pages;

import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;

import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;

import org.ideademo.orgs.services.util.PDFStreamResponse;
import org.ideademo.orgs.entities.Org;
import org.apache.log4j.Logger;

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


public class Index 
{
	 
  private static Logger logger = Logger.getLogger(Index.class);
  private static final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_31); 

  
  /////////////////////////////
  //  Drives QBE Search
  @Persist
  private Org example;
  
  
  //////////////////////////////////////////////////////////////
  // Used in rendering within Loop just as in Grid (Table) Row
  @SuppressWarnings("unused")
  @Property 
  private Org row;

    
  @Property
  @Persist
  private String searchText;

  @Inject
  private Session session;
  
  @Inject
  private HibernateSessionManager sessionManager;

  @Property 
  @Persist
  int retrieved; 
  @Property 
  @Persist
  int total;
  
  @Inject
  @Path("context:layout/images/image067.gif")
  private Asset logoAsset;
  
  @Inject
  private AssetSource assetSource;
  
  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Entity List generator - QBE, Text Search or Show All 
  //

  @SuppressWarnings("unchecked")
  public List<Org> getList()
  {
	//////////////////////////////////
	// first interpret search criteria
	  
	// text search string 
	logger.info("Search Text = " + searchText);
	
	
    // Get all records anyway - for showing total at bottom of presentation layer
    List <Org> alst = session.createCriteria(Org.class).list();
    total = alst.size();

	
    // then makes lists and sublists as per the search criteria 
    List<Org> xlst=null; // xlst = Query by Example search List
    if(example != null)
    {
       Example ex = Example.create(example).excludeFalse().ignoreCase().enableLike(MatchMode.ANYWHERE);
       
       xlst = session.createCriteria(Org.class).add(ex).list();
       
       
       if (xlst != null)
       {
    	   logger.info("Org Example Search Result List Size  = " + xlst.size() );
    	   Collections.sort(xlst);
       }
       else
       {
         logger.info("Org Example Search result did not find any results...");
       }
    }
    
    List<Org> tlst=null;
    if (searchText != null && searchText.trim().length() > 0)
    {
      FullTextSession fullTextSession = Search.getFullTextSession(sessionManager.getSession());  
      try
      {
        fullTextSession.createIndexer().startAndWait();
       }
       catch (java.lang.InterruptedException e)
       {
         logger.warn("Lucene Indexing was interrupted by something " + e);
       }
      
       QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Org.class ).get();
       
       // fields being covered by text search 
       TermMatchingContext onFields = qb
		        .keyword()
		        .onFields("code","name","description", "keywords","contact", "url", "worksheet", "logo");
       
       BooleanJunction<BooleanJunction> bool = qb.bool();
       /////// Tokenize the search string for default AND logic ///
       TokenStream stream = analyzer.tokenStream(null, new StringReader(searchText));
       CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
       try
       {
        while (stream.incrementToken()) 
         {
    	   String token = cattr.toString();
    	   logger.info("Adding search token " +  token + " to look in Orgs database");
    	   bool.must(onFields.matching(token).createQuery());
         }
        stream.end(); 
        stream.close(); 
       }
       catch (IOException ioe)
       {
    	   logger.warn("Orgs Text Search: Encountered problem tokenizing search term " + searchText);
    	   logger.warn(ioe);
       }
       
       /////////////  the lucene query built from non-simplistic English words 
       org.apache.lucene.search.Query luceneQuery = bool.createQuery();
       
       tlst = fullTextSession.createFullTextQuery(luceneQuery, Org.class).list();
       if (tlst != null) 
       {
    	   logger.info("TEXT Search for " + searchText + " found " + tlst.size() + " Orgs records in database");
    	   Collections.sort(tlst);
       }
       else
       {
          logger.info("TEXT Search for " + searchText + " found nothing in Orgs");
       }
    }
    
    
    // organize what type of list is returned...either total, partial (subset) or intersection of various search results  
    if (example == null && (searchText == null || searchText.trim().length() == 0))
    {
    	// Everything...
    	if (alst != null && alst.size() > 0)
    	{
    		logger.info ("Returing all " + alst.size() + " Orgs records");
        	Collections.sort(alst);
    	}
    	else
    	{
    		logger.warn("No Org records found in the database");
    	}
    	retrieved = total;
        return alst; 
    }
    else if (xlst == null && tlst != null)
    {
    	// just text search results
    	logger.info("Returing " + tlst.size() + " Orgs records as a result of PURE text search (no QBE) for " + searchText);
    	retrieved = tlst.size();
    	return tlst;
    }
    else if (xlst != null && tlst == null)
    {
    	// just example query results
    	logger.info("Returning " + xlst.size() + " Orgs records as a result of PURE Query-By-Example (QBE), no text string");
    	retrieved = xlst.size();
    	return xlst;
    }
    else 
    {

        ////////////////////////////////////////////
    	// get the INTERSECTION of the two lists
    	
    	// TRIVIAL: if one of them is empty, return the other
    	// if one of them is empty, return the other
    	if (xlst.size() == 0 && tlst.size() > 0)
    	{
        	logger.info("Returing " + tlst.size() + " Orgs records as a result of ONLY text search, QBE pulled up ZERO records for " + searchText);
        	retrieved = tlst.size();
    		return tlst;
    	}

    	if (tlst.size() == 0 && xlst.size() > 0)
    	{
        	logger.info("Returning " + xlst.size() + " Orgs records as a result of ONLY Query-By-Example (QBE), text search pulled up NOTHING for string " + searchText);
        	retrieved = xlst.size();
	        return xlst;
    	}
    	
    	
    	List <Org> ivec = new Vector<Org>();
    	// if both are empty, return this Empty vector. 
    	if (xlst.size() == 0 && tlst.size() == 0)
    	{
        	logger.info("Neither QBE nor text search for string " + searchText +  " pulled up ANY Orgs Records.");
        	retrieved = 0;
    		return ivec;
    	}
    	


    	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    	// now deal with BOTH text and QBE being non-empty lists - implementing intersection by Database Primary Key -  Id
    	Iterator<Org> xiterator = xlst.iterator();
    	while (xiterator.hasNext()) 
    	{
    		Org x = xiterator.next();
    		Long xid = x.getId();
    		
        	Iterator<Org> titerator = tlst.iterator();
    		while(titerator.hasNext())
    		{
        		Org t = titerator.next();
        		Long tid = t.getId();
    			
        		if (tid == xid)
        		{
        			ivec.add(t); break;
        		}
        		
    		}
    			
    	}
    	// sort again - 
    	if (ivec.size() > 0)  Collections.sort(ivec);
    	logger.info("Returning " + ivec.size() + " Orgs records from COMBINED (text, QBE) Search");
    	retrieved = ivec.size();
    	return ivec;
    }
    
  }
  
  
  public StreamResponse onSelectedFromPdf() 
  {
      // Create PDF
	  InputStream is = getPdfTable(getList());
      // Return response
      return new PDFStreamResponse(is,"PacisOrganizations" + System.currentTimeMillis());
  }

  private InputStream getPdfTable(List list) 
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
              
              String subheader = "Printing " + retrieved + " of total " + total + " records.";
              if (StringUtils.isNotBlank(searchText))
              {
            	  subheader += "  Searching for \"" + searchText + "\""; 
              }
              
              document.add(new Paragraph(subheader));
              document.add(Chunk.NEWLINE);document.add(Chunk.NEWLINE);
              
              // create table, 2 columns
           	Iterator<Org> iterator = list.iterator();
           	int count=0;
       		while(iterator.hasNext())
      		{
       			count++;
          		Org org = iterator.next();
          		
          		String name = org.getName();
          		String description = org.getDescription();
          		String url = org.getUrl();
          		String logoFile = org.getLogo();
          		
                PdfPTable table = new PdfPTable(2);
                table.setWidths(new int[]{1, 4});
                table.setSplitRows(false);
                
                
 	
                
                
                PdfPCell nameTitle = new PdfPCell(new Phrase("#" + count + ") Name")); 
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
          		if (org.isFederal()) 
          		{
          			ListItem item = new ListItem("Federal"); types.add(item);
          		}
          		if (org.isState()) 
          		{
          			ListItem item = new ListItem("State"); types.add(item);
          		}
          		if (org.isLocal()) 
          		{
          			ListItem item = new ListItem("Local"); types.add(item);
          		}
          		if (org.isInteragency()) 
          		{
          			ListItem item = new ListItem("Interagency"); types.add(item);
          		}
          		if (org.isAcademic()) 
          		{
          			ListItem item = new ListItem("Academic"); types.add(item);
          		}
          		if (org.isNgo()) 
          		{
          			ListItem item = new ListItem("Non-governmental Organization (NGO)"); types.add(item);
          		}
          		if (org.isOtherPartnerType()) 
          		{
          			ListItem item = new ListItem("Unclassified/Other"); types.add(item);
          		}


          		if(types.size() > 0)
          		{
          		  PdfPCell typesCell = new PdfPCell(); typesCell.addElement(types);
          		  table.addCell(new PdfPCell(new Phrase("Types")));  table.addCell(typesCell);
          		}

          		
          		
          		if (StringUtils.isNotBlank(org.getContact()))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Contact")));  table.addCell(new PdfPCell(new Phrase(StringUtils.trimToEmpty(org.getContact()))));
          		}

          		document.add(table);
          		document.add(Chunk.NEWLINE);
      		}
              
              
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
      return bais;
}

  
  ////////////////////////////////////////////////
  //  QBE Setter 
  //  

  public void setExample(Org x) 
  {
    this.example = x;
  }

  

  
  ///////////////////////////////////////////////////////////////
  //  Action Event Handlers 
  //
  
  Object onSelectedFromSearch() 
  {
    return null; 
  }

  Object onSelectedFromClear() 
  {
    this.searchText = "";
    this.example = null; 
    return null; 
  }
  
  public StreamResponse onReturnStreamResponse(long orgId) 
  {

	  
	  Org org =  (Org) session.load(Org.class, orgId);
  	
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
          		
          		String name = org.getName();
          		String description = org.getDescription();
          		String url = org.getUrl();
          		String logoFile = org.getLogo();
          		
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
          		if (org.isFederal()) 
          		{
          			ListItem item = new ListItem("Federal"); types.add(item);
          		}
          		if (org.isState()) 
          		{
          			ListItem item = new ListItem("State"); types.add(item);
          		}
          		if (org.isLocal()) 
          		{
          			ListItem item = new ListItem("Local"); types.add(item);
          		}
          		if (org.isInteragency()) 
          		{
          			ListItem item = new ListItem("Interagency"); types.add(item);
          		}
          		if (org.isAcademic()) 
          		{
          			ListItem item = new ListItem("Academic"); types.add(item);
          		}
          		if (org.isNgo()) 
          		{
          			ListItem item = new ListItem("Non-governmental Organization (NGO)"); types.add(item);
          		}
          		if (org.isOtherPartnerType()) 
          		{
          			ListItem item = new ListItem("Unclassified/Other"); types.add(item);
          		}


          		if(types.size() > 0)
          		{
          		  PdfPCell typesCell = new PdfPCell(); typesCell.addElement(types);
          		  table.addCell(new PdfPCell(new Phrase("Types")));  table.addCell(typesCell);
          		}

          		
          		
          		if (StringUtils.isNotBlank(org.getContact()))
          		{
          		  table.addCell(new PdfPCell(new Phrase("Contact")));  table.addCell(new PdfPCell(new Phrase(StringUtils.trimToEmpty(org.getContact()))));
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