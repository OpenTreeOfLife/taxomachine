package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;

import opentree.taxonomy.contexts.TaxonomyContext;

public class ContextResult {
	public TaxonomyContext context;
	public LinkedList<String> namesNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<String> namesNotMatched) {
		this.context = context;
		this.namesNotMatched = new LinkedList<String>(namesNotMatched);
	}
}
