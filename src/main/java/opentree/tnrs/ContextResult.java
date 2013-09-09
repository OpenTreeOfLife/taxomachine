package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import opentree.taxonomy.contexts.TaxonomyContext;

public class ContextResult {
	public TaxonomyContext context;
<<<<<<< HEAD
	public LinkedList<String> nameIdsNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<String> nameIdsNotMatched) {
		this.context = context;
		this.nameIdsNotMatched = new LinkedList<String>(nameIdsNotMatched);
=======
//	public LinkedList<String> namesNotMatched;
	public List<Object> nameIdsNotMatched;
	
	public ContextResult(TaxonomyContext context, Collection<Object> namesNotMatched) {
		this.context = context;
		this.nameIdsNotMatched = new LinkedList<Object>(nameIdsNotMatched);
>>>>>>> 35464a1265fc06be6fee448ab9349353ca37298e
	}
}
