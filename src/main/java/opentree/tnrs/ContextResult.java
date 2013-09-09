package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;

import opentree.taxonomy.contexts.TaxonomyContext;

public class ContextResult {
	public TaxonomyContext context;
	public LinkedList<String> nameIdsNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<String> nameIdsNotMatched) {
		this.context = context;
		this.nameIdsNotMatched = new LinkedList<String>(nameIdsNotMatched);
	}
}
