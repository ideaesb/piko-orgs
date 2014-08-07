package org.ideademo.orgs.pages.pdf;


import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.StreamResponse;


public class Index {
	@InjectPage
	private org.ideademo.orgs.pages.Index index;
	
	public StreamResponse onActivate()
    {
		return index.onSelectedFromPdf();
    }
}