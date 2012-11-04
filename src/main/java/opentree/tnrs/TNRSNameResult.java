package opentree.tnrs;

import java.util.Iterator;

public class TNRSNameResult implements Iterable<TNRSMatch> {

    private final String _queriedName;
	private final TNRSMatchSet _matches;
	
	public TNRSNameResult(String queriedName, TNRSMatchSet matches) {
	    _queriedName = queriedName;
	    _matches = matches;
	}
	
	public TNRSMatchSet getMatches() {
	    return _matches;
	}

	public String getQueriedName() {
	    return _queriedName;
	}

    @Override
    public Iterator<TNRSMatch> iterator() {
        return _matches.iterator();
    }
}
