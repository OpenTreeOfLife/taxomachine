package org.opentree.tnrs;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.opentree.taxonomy.Taxon;

/**
 * TNRSHit objects are mutable precursors to TNRSMatch objects. They are intended to be used as organizational containers, which temporarily hold
 * information about matches, and they can be passed to the TNRSMatchSet method addMatch() in order to an actual TNRSMatch object to the set. They
 * should not be used for other purposes.
 * 
 * @author Cody Hinchliff
 *
 */
public class TNRSHit {

    Node matchedNode;
    String searchString;
    String sourceName;
    String nomenCode;
    boolean isHomonym;
    boolean isPerfectMatch;
    boolean isApprox;
    boolean isSynonym;
    String rank;
    boolean nameStatusIsKnown;
    HashMap<String,String> otherData;
    double score;

    public TNRSHit() {
        matchedNode = null;
        searchString = "";
        sourceName = "";
        nomenCode = "undetermined";
        isHomonym = false;
        isPerfectMatch = false;
        isApprox = false;
        isSynonym = false;
        rank = "";
        nameStatusIsKnown = true;
        otherData = null;
        score = -1;
    }
    
    public TNRSHit setMatchedTaxon(Taxon matchedTaxon) {
        this.matchedNode = matchedTaxon.getNode();
        return this;
    }

    public TNRSHit setSearchString(String searchString) {
        this.searchString = searchString;
        return this;
    }

    public TNRSHit setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    public TNRSHit setNomenCode(String nomenCode) {
        this.nomenCode = nomenCode;
        return this;
    }
    
    public TNRSHit setIsHomonym(boolean isHomonym) {
        this.isHomonym = isHomonym;
        return this;
    }
    
    public TNRSHit setRank(String rank) {
    	this.rank = rank;
    	return this;
    }
    
    /**
     * Indicates a match for which it is known whether the matched name represents either a homonym or synonym. For fuzzy matches, this may not be known.
     * @param nameStatusIsKnown
     * @return
     */
    public TNRSHit setNameStatusIsKnown(boolean nameStatusIsKnown) {
        this.nameStatusIsKnown = nameStatusIsKnown;
        return this;
    }

    /**
     * Indicates a hit to a node this is neither a synonym nor a homonym, and whose name is an exact match to the query.
     * @param isPerfectMatch
     * @return
     */
    public TNRSHit setIsPerfectMatch(boolean isPerfectMatch) {
        this.isPerfectMatch = isPerfectMatch;
        return this;
    }

    public TNRSHit setIsApprox(boolean isApprox) {
        this.isApprox = isApprox;
        return this;
    }

    public TNRSHit setIsSynonym(boolean isSynonym) {
        this.isSynonym = isSynonym;
        return this;
    }

    public TNRSHit setOtherData(Map<String,String> otherData) {
        this.otherData = (HashMap<String, String>)otherData;
        return this;
    }

    public TNRSHit setScore(double score) {
        this.score = score;
        return this;
    }
    
    public Node getMatchedNode() {
        return matchedNode;
    }    
    
    public String getSearchString() {
        return searchString;
    }
    
    public String getNomenCode() {
        return nomenCode;
    }
    
    public boolean getIsHomonym() {
        return isHomonym;
    }
    
    public String getRank() {
    	return rank;
    }
        
    public String getSourceName() {
        // the name of the source where the match was found
        return sourceName;
    }

    public boolean getIsPerfectMatch() {
        // is this an exact direct match to a graph node?
        return isPerfectMatch;
    }

    public boolean getIsApprox() {
        // whether the match is a fuzzy match (presumably misspelling)
        return isApprox;
    }

    public boolean getIsSynonym() {
        // indicates that this match points to a known synonym
        return isSynonym;
    }
    
    public boolean getNameStatusIsKnown() {
        return nameStatusIsKnown;
    }

    public double getScore() {
        // the score of this match
        return score;
    }

    public Map<String, String> getOtherData() {
        // any other data
        return otherData;
    }

}
