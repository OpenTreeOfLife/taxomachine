package opentree.tnrs;

import java.util.Set;

import opentree.taxomachine.TaxonomyExplorer;

/**
 * This abstract class just defines the basic API for TNRS adapters, so that they will work with the TNRSQuery
 * objects that call them. At this time, they simply must accept a set of search strings, a connection to a
 * taxonomy explorer object for the graph to be queried, and a set of results to which new matches will be
 * added. Note: search strings that result in positive matches should be removed from the passed HashSet.
 * 
 * @author Cody Hinchliff
 *
 */
public abstract class TNRSAdapter {
    
    public abstract void doQuery(Set<String> searchStrings, TaxonomyExplorer taxonomy, TNRSMatchSet results);

}
