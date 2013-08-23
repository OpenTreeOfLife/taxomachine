package opentree.tnrs.queries;

import opentree.TaxonomyContext;
import opentree.tnrs.TNRSResults;

public interface TNRSQuery {

	public TNRSQuery setContext(TaxonomyContext context);
	public TNRSQuery setDefaults();
	public TNRSQuery clear();
	public TNRSQuery runQuery();
	public TNRSResults getResults();
	public TaxonomyContext getContext();
	
}
