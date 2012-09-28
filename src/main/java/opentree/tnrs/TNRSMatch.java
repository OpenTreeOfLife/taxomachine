package opentree.tnrs;

import org.neo4j.graphdb.Node;

public abstract class TNRSMatch {

    public abstract String getSearchString();
    public abstract long getMatchedNodeId();
    public abstract String getMatchedNodeName();
    public abstract Node getMatchedNode();
    public abstract String getSource();
    public abstract String getMatchType();
    public abstract String toString();
}
