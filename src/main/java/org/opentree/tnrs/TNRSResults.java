package org.opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TNRSResults implements Iterable<TNRSNameResult> {
	private Map<Object, TNRSNameResult> results;
	private Map<Object, String> unambiguousNames;
	private Map<Object, String> unmatchedNames;
	private String contextName;
	private String governingCode;
	private double minimumScore;

	public TNRSResults() {
		results = new HashMap<Object, TNRSNameResult>();
		unambiguousNames = new HashMap<Object, String>();
		unmatchedNames = new HashMap<Object, String>();
		contextName = "";
		governingCode = "";
		minimumScore = 0;
	}

	public Iterator<TNRSNameResult> iterator() {
		Map<Object, TNRSNameResult> resultsCopy = new HashMap<Object, TNRSNameResult>(results);
		return resultsCopy.values().iterator();
	}

	// getters

	public Set<Object> getMatchedNameIds() {
		return results.keySet();
	}

	public Set<Object> getUnmatchedNameIds() {
		return unmatchedNames.keySet();
	}

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
		results.put(result.getId(), result);
	}

	public void addUnmatchedName(Object id, String name) {
		unmatchedNames.put(id, name);
	}

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
