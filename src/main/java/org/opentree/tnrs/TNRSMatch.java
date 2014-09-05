package org.opentree.tnrs;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.opentree.taxonomy.Taxon;

/**
 * A TNRSMatch object represents a validated hit for some query string to a recognized name or synonym in some source, and as such they
 * contain relevant metadata for the represented match. TNRSMatch objects are returned by various methods of the TNRSMatchSet objects
 * that contain them, and are immutable, as they represent external database records that cannot be changed except by external means.
 * @author Cody Hinchliff
 *
 */
public abstract class TNRSMatch {
    
    public abstract String getSearchString();
    public abstract String getMatchedName();
    @Deprecated
    public abstract Node getMatchedNode(); // remove once the v1 TNRSResultsRepresentation is removed
    public abstract Taxon getMatchedTaxon();
    @Deprecated
    public abstract Node getParentNode(); // remove once the v1 TNRSResultsRepresentation is removed
    public abstract String getNomenCode();
    public abstract boolean getIsApproximate();
    public abstract boolean getIsSynonym();
    public abstract boolean getIsDeprecated();
    public abstract boolean getIsDubious();
    public abstract List<String> getSynonyms();
    public abstract String getRank();
    public abstract Boolean getIsHigherTaxon();
    public abstract String getUniqueName();
    public abstract double getScore();
    public abstract String toString();
}
