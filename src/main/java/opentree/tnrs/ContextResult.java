package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import opentree.taxonomy.contexts.TaxonomyContext;

public class ContextResult {
	public TaxonomyContext context;
	public LinkedList<String> namesNotMatched;
//	public List<Object> nameIdsNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<String> namesNotMatched) {
		this.context = context;
		this.namesNotMatched = new LinkedList<String>();
		for (String name : namesNotMatched) {
			this.namesNotMatched.add(name);
		}
	}
}
