package opentree.tnrs;

import java.util.HashMap;

import org.neo4j.graphdb.Node;

public interface TNRSMatchParser {

    public abstract Node getMatchedNode(); // the matched node
    public abstract Node getSynonymNode(); // the matched synonym node (if any)
    public abstract String getSearchString(); // the original search text queried
    public abstract String getSourceName(); // the name of the source where the match was found
    public abstract boolean getIsExactNode(); // is this an exact direct match to a graph node?
    public abstract boolean getIsApprox(); // indicates matches that are not direct, i.e. fuzzy matches (presumably misspellings)
    public abstract boolean getIsSynonym(); // indicates that this match is to a known synonym (not necessarily in the graph, nor necessarily pointing to a node in the graph)
    public abstract double getScore(); // the score of this match
    public abstract HashMap<String,String> getOtherData(); // other data provided by the match source

}
