package org.ideademo.orgs.entities;


import java.lang.Comparable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import org.apache.tapestry5.beaneditor.NonVisual;


@Entity @Indexed
public class Org implements Comparable<Org>
{
	
	//////////////////////////////////////////
	//Reserved indexing id 
	
	@Id @GeneratedValue @DocumentId @NonVisual
	private Long id;
	
	
	//////////////////////////////////////////////
	//String fields (being a keyword for Lucene)
	//
	
	@Field
	private String code="";

	@Field @Column (length=1024)
	private String name="";
	
	@Field @Column (length=2048)
	private String contact="";
	
	@Field 
	private String url="";
	
	@Field @Column (length=4096)
	private String description="";
	
	@Field @Column (length=4096)
	private String keywords="";
	
	@Field 
	private String worksheet="";
	
	@Field
	private String logo="";
	
	
	/////////////////////////////////////////////
	//Type of Partner
	//
	
    private boolean pmops = false;
    private boolean agency = false; 
	private boolean federal = false;
	private boolean state = false;
	private boolean local = false;
	private boolean interagency = false;
	private boolean academic = false;
	private boolean ngo = false;
	private boolean otherPartnerType = false;
	
	private boolean worksheetExists = false;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getUrl() 
	{
		if (url == null || url.trim().length() == 0)
		{
			return "";
		}
	    else if (url.startsWith("http"))
		{
		  return url;
		}
	    else
	    {
	    	return "http://" + url;
	    }
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getWorksheet() {
		return worksheet;
	}

	public void setWorksheet(String worksheet) {
		this.worksheet = worksheet;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public boolean isPmops() {
		return pmops;
	}

	public void setPmops(boolean pmops) {
		this.pmops = pmops;
	}

	public boolean isAgency() {
		return agency;
	}

	public void setAgency(boolean agency) {
		this.agency = agency;
	}

	public boolean isFederal() {
		return federal;
	}

	public void setFederal(boolean federal) {
		this.federal = federal;
	}

	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public boolean isInteragency() {
		return interagency;
	}

	public void setInteragency(boolean interagency) {
		this.interagency = interagency;
	}

	public boolean isAcademic() {
		return academic;
	}

	public void setAcademic(boolean academic) {
		this.academic = academic;
	}

	public boolean isNgo() {
		return ngo;
	}

	public void setNgo(boolean ngo) {
		this.ngo = ngo;
	}

	public boolean isOtherPartnerType() {
		return otherPartnerType;
	}

	public void setOtherPartnerType(boolean otherPartnerType) {
		this.otherPartnerType = otherPartnerType;
	}

	public boolean isWorksheetExists() {
		return worksheetExists;
	}

	public void setWorksheetExists(boolean worksheetExists) {
		this.worksheetExists = worksheetExists;
	}

	
	////////////////////////////////////////////////
	/// default/natural sort order - String  - names
	
	public int compareTo(Org ao) 
	{
	    boolean thisIsEmpty = false;
	    boolean aoIsEmpty = false; 
	    
	    if (this.getName() == null || this.getName().trim().length() == 0) thisIsEmpty = true; 
	    if (ao.getName() == null || ao.getName().trim().length() == 0) aoIsEmpty = true;
	    
	    if (thisIsEmpty && aoIsEmpty) return 0;
	    if (thisIsEmpty && !aoIsEmpty) return -1;
	    if (!thisIsEmpty && aoIsEmpty) return 1; 
	    return this.getName().compareToIgnoreCase(ao.getName());
    }

}
