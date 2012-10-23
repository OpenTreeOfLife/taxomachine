package opentree.tnrs;

import org.neo4j.graphdb.Node;

/**
 * A TNRSMatch object represents a validated hit for some query string to a recognized name or synonym in some source, and as such they
 * contain relevant metadata for the represented match. TNRSMatch objects are returned by various methods of the TNRSMatchSet objects
 * that contain them, and are immutable, as they represent external database records that cannot be changed except by external means.
 * @author Cody Hinchliff
 *
 */
public abstract class TNRSMatch {
    public abstract String getSearchString();
    public abstract long getMatchedNodeId();
    public abstract String getMatchedNodeName();
    public abstract Node getMatchedNode();    
    public abstract long getSynonymNodeId();
    public abstract String getSynonymNodeName();
    public abstract String getSource();
    public abstract boolean getIsPerfectMatch();
    public abstract boolean getIsApproximate();
    public abstract boolean getIsSynonym();
    public abstract boolean getIsHomonym();
    public abstract double getScore();
    public abstract String getMatchType();
    public abstract String toString();
}
