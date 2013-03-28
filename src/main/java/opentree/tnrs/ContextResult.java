package opentree.tnrs;

import java.util.Collection;
import java.util.LinkedList;

public class ContextResult {
	public String contextName;
	public LinkedList<String> namesNotMatched;
	
	public ContextResult(String contextName, Collection<String> namesNotMatched) {
		this.contextName = contextName;
		this.namesNotMatched = new LinkedList<String>(namesNotMatched);
	}
}
