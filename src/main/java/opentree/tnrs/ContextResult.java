package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import opentree.taxonomy.contexts.TaxonomyContext;

public class ContextResult {
	public TaxonomyContext context;
//	public LinkedList<String> namesNotMatched;
	public List<Object> nameIdsNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<Object> namesNotMatched) {
		this.context = context;
		this.nameIdsNotMatched = new LinkedList<Object>();
		for (Object name : nameIdsNotMatched) {
			nameIdsNotMatched.add(name);
		}
	}
}
