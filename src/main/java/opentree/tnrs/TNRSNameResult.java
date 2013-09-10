package opentree.tnrs;

import java.util.Iterator;

public class TNRSNameResult implements Iterable<TNRSMatch> {

	private final Object id;
	private final TNRSMatchSet matches;
	
	public TNRSNameResult(Object id, TNRSMatchSet matches) {
		this.id = id;
	    this.matches = matches;
	}
	
	public TNRSMatchSet getMatches() {
	    return matches;
	}
	
	public Object getId() {
		return id;
	}

    public Iterator<TNRSMatch> iterator() {
        return matches.iterator();
    }
}
