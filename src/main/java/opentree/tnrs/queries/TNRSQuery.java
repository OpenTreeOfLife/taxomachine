package opentree.tnrs.queries;

import org.apache.lucene.queryParser.ParseException;

import opentree.TaxonomyContext;
import opentree.tnrs.TNRSResults;

public interface TNRSQuery {

	public TNRSQuery setContext(TaxonomyContext context);
	public TNRSQuery setDefaults();
	public TNRSQuery clear();
	public TNRSQuery runQuery() throws ParseException;
	public TNRSResults getResults();
	public TaxonomyContext getContext();
	
}
