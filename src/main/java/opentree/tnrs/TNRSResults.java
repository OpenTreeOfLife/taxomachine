package opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TNRSResults implements Iterable<TNRSNameResult> {
//	private HashMap<String, TNRSNameResult> results;
	private HashMap<Object, TNRSNameResult> results;
//	private HashSet<String> unambiguousNames;
//	private HashSet<String> unmatchedNames;
//	private Map<String, String> unambiguousNames;
//	private Map<String, String> unmatchedNames;
	private Map<Object, String> unambiguousNames;
	private Map<Object, String> unmatchedNames;
	private String contextName;
	private String governingCode;
	private double minimumScore;

	public TNRSResults() {
//		results = new HashMap<String, TNRSNameResult>();
		results = new HashMap<Object, TNRSNameResult>();
//		unambiguousNames = new HashSet<String>();
//		unmatchedNames = new HashSet<String>();
//		unambiguousNames = new HashMap<String, String>();
//		unmatchedNames = new HashMap<String, String>();
		unambiguousNames = new HashMap<Object, String>();
		unmatchedNames = new HashMap<Object, String>();
		contextName = "";
		governingCode = "";
		minimumScore = 0;
	}

	public Iterator<TNRSNameResult> iterator() {
//		HashMap<String, TNRSNameResult> resultsCopy = new HashMap<String, TNRSNameResult>(results);
		HashMap<Object, TNRSNameResult> resultsCopy = new HashMap<Object, TNRSNameResult>(results);
		return resultsCopy.values().iterator();
	}

	// getters

	/*
	 * public Set<String> getMatchedNames() {
	 * return results.keySet();
	 * }
	 * 
	 * public Set<String> getUnmatchedNames() {
	 * return unmatchedNames;
	 * }
	 * 
	 * public Set<String> getNamesWithDirectMatches() {
	 * return unambiguousNames;
	 * }
	 */

//	public Set<String> getMatchedNameIds() {
	public Set<Object> getMatchedNameIds() {
		return results.keySet();
	}

//	public Set<String> getUnmatchedNameIds() {
	public Set<Object> getUnmatchedNameIds() {
		return unmatchedNames.keySet();
	}

//	public Set<String> getNameIdsWithDirectMatches() {
	public Set<Object> getNameIdsWithDirectMatches() {
		return unambiguousNames.keySet();
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

	public void addNameResult(TNRSNameResult result) {
//		results.put(result.getQueriedName(), result);
		results.put(result.getId(), result);
	}

/*	public void addUnmatchedName(String name) {
		unmatchedNames.add(name);
	}

	public void addNameWithDirectMatch(String name) {
		unambiguousNames.add(name);
	} */
	
//	public void addUnmatchedName(String id, String name) {
	public void addUnmatchedName(Object id, String name) {
		unmatchedNames.put(id, name);
	}

//	public void addNameWithDirectMatch(String id, String name) {
	public void addNameWithDirectMatch(Object id, String name) {
		unambiguousNames.put(id, name);
	}

	public void setGoverningCode(String code) {
		governingCode = code;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	public void setMinimumScore(double s) {
		minimumScore = s;
	}
}
