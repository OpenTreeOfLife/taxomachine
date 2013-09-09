package opentree.tnrs;

import java.util.Iterator;

public class TNRSNameResult implements Iterable<TNRSMatch> {

<<<<<<< HEAD
	private final String id;
//    private final String queriedName;
	private final TNRSMatchSet matches;
	
//	public TNRSNameResult(String id, String queriedName, TNRSMatchSet matches) {
	public TNRSNameResult(String id, TNRSMatchSet matches) {
=======
//	private final String id;
	private final Object id;
    private final String queriedName;
	private final TNRSMatchSet matches;
	
//	public TNRSNameResult(String id, String queriedName, TNRSMatchSet matches) {
	public TNRSNameResult(Object id, String queriedName, TNRSMatchSet matches) {
>>>>>>> 35464a1265fc06be6fee448ab9349353ca37298e
		this.id = id;
//	    this.queriedName = queriedName;
	    this.matches = matches;
	}
	
	public TNRSMatchSet getMatches() {
	    return matches;
	}
	
//	public String getId() {
	public Object getId() {
		return id;
	}

/*	public String getQueriedName() {
	    return queriedName;
	} */

    public Iterator<TNRSMatch> iterator() {
        return matches.iterator();
    }
}
