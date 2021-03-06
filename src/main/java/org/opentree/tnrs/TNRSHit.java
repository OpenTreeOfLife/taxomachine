package org.opentree.tnrs;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.Taxonomy;

/**
 * TNRSHit objects are mutable precursors to TNRSMatch objects. They are intended to be used as organizational containers, which temporarily hold
 * information about matches, and they can be passed to the TNRSMatchSet method addMatch() in order to an actual TNRSMatch object to the set. They
 * should not be used for other purposes.
 * 
 * @author Cody Hinchliff
 */
public class TNRSHit {

    Taxon matchedTaxon = null;
    String searchString = "";
    String matchedName = "";
    String nomenCode = "undetermined";
    boolean isApprox = false;
    boolean isSynonym = false;
    boolean isDubious = false;
    boolean isDeprecated = false;
    String rank = "";
//    boolean nameStatusIsKnown;
    double score = -1;

    public TNRSHit() {}
    
    public TNRSHit setMatchedTaxon(Taxon matchedTaxon) {
        this.matchedTaxon = matchedTaxon;
        return this;
    }

    public TNRSHit setSearchString(String searchString) {
        this.searchString = searchString;
        return this;
    }

    public TNRSHit setMatchedName(String matchedName) {
        this.matchedName = matchedName;
        return this;
    }

    public TNRSHit setNomenCode(String nomenCode) {
        this.nomenCode = nomenCode;
        return this;
    }
    
    public TNRSHit setIsDubious(boolean isDubious) {
        this.isDubious = isDubious;
        return this;
    }
    
    public TNRSHit setRank(String rank) {
    	this.rank = rank;
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

    public TNRSHit setScore(double score) {
        this.score = score;
        return this;
    }
    
    /*
    public Node getMatchedNode() {
        return matchedTaxon.getNode();
    }*/
    
    public String getMatchedName() {
    	return matchedName;
    }
    
    public Taxon getMatchedTaxon() {
    	return matchedTaxon;
    }
    
    public String getSearchString() {
        return searchString;
    }
    
    public String getNomenCode() {
        return nomenCode;
    }
    
    public String getRank() {
    	return rank;
    }
     
    public boolean getIsApprox() {
        // whether the match is a fuzzy match (presumably misspelling)
        return isApprox;
    }

    public boolean getIsSynonym() {
        // indicates that this match points to a known synonym
        return isSynonym;
    }
    
    public double getScore() {
        // the score of this match
        return score;
    }
}
