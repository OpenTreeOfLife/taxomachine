package opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TNRSResults implements Iterable<TNRSNameResult> {
	private HashMap<String, TNRSNameResult> results;
	private HashSet<String> unambiguousNames;
	private HashSet<String> unmatchedNames;
	private String contextName;
	private String governingCode;
	private double minimumScore;

	public TNRSResults() {
	    results = new HashMap<String, TNRSNameResult>();
	    unambiguousNames = new HashSet<String>();
	    unmatchedNames = new HashSet<String>();
	    contextName = "";
	    governingCode = "";
	    minimumScore = 0;
	}
	
	public Iterator<TNRSNameResult> iterator() {
	    HashMap<String, TNRSNameResult> resultsCopy = new HashMap<String, TNRSNameResult>(results);
	    return resultsCopy.values().iterator();
	}

	// getters
	
	public Set<String> getMatchedNames() {
	    return results.keySet();
	}
	
    public Set<String> getUnmatchedNames() {
        return unmatchedNames;
    }

    public Set<String> getNamesWithDirectMatches() {
        return unambiguousNames;
    }

    public String getGoverningCode() {
        return governingCode;
    }
    
    public String getContextName() {
        return contextName;
    }

    public double getMinimumScore() {
        return minimumScore;
    }
    
    public int size() {
        return results.size();
    }
    
    public TNRSMatch getSingleMatch() throws MultipleHitsException {
        TNRSMatchSet firstMatches = results.values().iterator().next().getMatches();

        if (results.size() > 1 || firstMatches.size() > 1)
            throw new MultipleHitsException("Attempt to use getSingleMatch() when more than one match was found");

        return firstMatches.iterator().next();
    }

    // setters
    
    protected void addNameResult(TNRSNameResult result) {
        results.put(result.getQueriedName(), result);
    }

    protected void addUnmatchedName(String name) {
        unmatchedNames.add(name);
    }

    protected void addNameWithDirectMatch(String name) {
        unambiguousNames.add(name);
    }

    protected void setGoverningCode(String code) {
        governingCode = code;
    }
    
    protected void setContextName(String contextName) {
        this.contextName = contextName;
    }
    
    protected void setMinimumScore(double s) {
        minimumScore = s;
    }
}
