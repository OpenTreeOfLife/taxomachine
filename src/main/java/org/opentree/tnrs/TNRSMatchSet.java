package org.opentree.tnrs;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.server.rest.repr.MappingRepresentation;
import org.neo4j.server.rest.repr.MappingSerializer;
import org.opentree.taxonomy.GraphDatabaseAgent;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.TaxonomyRelType;
import org.opentree.taxonomy.Taxon;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.opentree.taxonomy.contexts.TaxonomyContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Cody Hinchliff
 * 
 * A set of matches resulting from a single TNRS query (which may have contained many names). These are simple containers to hold results, which
 * they provide access to in the form of TNRSMatch objects representing individual hits for a given search string to some recognized name or synonym.
 *
 */
public class TNRSMatchSet implements Iterable<TNRSMatch> {
    
    private List<TNRSMatch> matches;
    private Taxonomy taxonomy;

    // TODO: add sort features

    public TNRSMatchSet(Taxonomy tax) {
        matches = new ArrayList<TNRSMatch>();
        taxonomy = tax;
    }
    
    /**
     * @return the number of matches in the set
     */
    public int size() {
        return matches.size();
    }

    /**
     * @return an iterator of TNRSMatch objects containing all the matches in this set
     */
    public Iterator<TNRSMatch> iterator() {
        return matches.iterator();
    }

    /**
     * Adds a match to the set, using the data within the passed TNRSHit object.
     * @param TNRSHit to be added
     */
    public void addMatch(TNRSHit m) {
        Match match = new Match(m);
        matches.add(match);
    }
    
    public List<TNRSMatch> getMatchList() {
    	return matches;
    }
        
    /**
     * An internal container compatible with the TNRSMatch specification.
     */
    private class Match extends TNRSMatch {

        // for all matches, information associating this match with a known node in the graph
        private String searchString;                   // the original search text queried
        private Node matchedNode;                     // the recognized taxon node we matched
        private String sourceName;                     // the name of the source where the match was found
        private String nomenCode;                      // the nomenclatural code under which this match is defined
        private boolean isPerfectMatch;                  // whether this is an exact match to known, recognized taxon
        private boolean isApprox;                     // whether this is a fuzzy match (presumably misspellings)
        private boolean isSynonym;                    // whether the match points to a known synonym (not necessarily in the graph, nor necessarily pointing to a node in the graph)
        private boolean isHomonym;                     // whether the match points to a known homonym
        private String rank;							// the taxonomic rank
        private boolean nameStatusIsKnown;              // whether we know if the match involves a synonym and/or homonym
        private double score;                         // the score of this match
        private HashMap<String,String> otherData;     // other data provided by the match source
        
        public Match(TNRSHit m) {
            matchedNode = m.getMatchedNode();
            isPerfectMatch = m.getIsPerfectMatch();
            sourceName = m.getSourceName();
            nomenCode = m.getNomenCode();
            isApprox = m.getIsApprox();
            isSynonym = m.getIsSynonym();
            isHomonym = m.getIsHomonym();
            rank = m.getRank();
            nameStatusIsKnown = m.getNameStatusIsKnown();
            searchString = m.getSearchString();
            score = m.getScore();
            otherData = (HashMap<String,String>)m.getOtherData();
        }
        
        /**
         * @return the original search string which produced this match
         */
		@Override
		public String getSearchString() {
            return searchString;
        }
        
        /**
         * @return the Neo4j Node object for the recognized name to which this match points
         */
		@Override
		public Node getMatchedNode() {
            return matchedNode;
        }
        
		@Override
		public Node getParentNode() {
            TraversalDescription prefTaxTD = Traversal.description().breadthFirst().
                    relationships(TaxonomyRelType.PREFTAXCHILDOF, Direction.OUTGOING).evaluator(Evaluators.toDepth(1));
            
            Node p = null;
            for (Node n : prefTaxTD.traverse(matchedNode).nodes()) {
                p = n;
            }

            if (p.equals(matchedNode) == false)
                return p;
            else
                throw new java.lang.IllegalStateException("Node " + matchedNode + " doesn't seem to have a preferred parent!");
        }

		@Override
		public boolean getIsPerfectMatch() {
            return isPerfectMatch;
        }

		@Override
		public boolean getIsApproximate() {
            return isApprox;
        }

        /**
         * @return the TNRS source that produced this match
         */
		@Override
		public String getSource() {
            return sourceName;
        }
        
		@Override
		public String getNomenCode() {
            return nomenCode;
        }

		@Override
		public boolean getIsSynonym() {
            return isSynonym;
        }
        
/*        public boolean getIsDubiousName() {
        	return (Boolean) matchedNode.getProperty("dubious");
        } */

		@Override
		public boolean getIsHomonym() {
            return isHomonym;
        }
        
		@Override
		public String getRank() {
        	return rank;
        }
        
		@Override
		public Boolean getIsHigherTaxon() {
			boolean isHigher = true;
			String rank = this.getRank();
			if (rank.equals("species") || rank.equals("subspecies") || rank.equals("variety") || rank.equals("forma")) {
				isHigher = false;
			}
			return isHigher;
		}
        
		@Override
		public boolean getNameStatusIsKnown() {
            return nameStatusIsKnown;
        }

		@Override
		public double getScore() {
            return score;
        }
        
		@Override
		public String getUniqueName() {
			String uniqueName = (String) getMatchedNode().getProperty("uniqname");
			if (uniqueName.length() > 0) {
				return uniqueName;
			} else {
				return (String) getMatchedNode().getProperty("name");
			}
			/*
        	String uniqueName = getMatchedNode().getProperty("name").toString();
        	if (isHomonym) {
	    		if (getMatchedNode().hasProperty("leastcontext")) {
	    			String leastContext = String.valueOf(getMatchedNode().getProperty("leastcontext"));
	    			for (ContextDescription cd : ContextDescription.values()) {
	    				if (cd.toString().equals(leastContext)) {
	    					uniqueName = uniqueName.concat(" (").concat(cd.name).concat(")");
	    				}
	    			}
	    		}
        	}

			return uniqueName; */
        }
        
        /**
         * @return a textual description of the type of match this is
         */
		@Override
		public String getMatchType() {
            String desc = "";
                        
            if (isPerfectMatch) {
                desc += "unambiguous match to known taxon";
            
            } else {
                
            	if (isApprox) {
                	desc += "approximate match";
                } else {
                    desc += "exact match";
                }

            	if (nameStatusIsKnown) {
                    if (isSynonym) {
                    	desc += "to known synonym";
    
                    } else {
                    	desc += "to known taxon";
    
                    }

		    if (isHomonym) {
    	                desc += "; also a homonym";
                    }
            	} else {
            	    desc += "; name status unknown";
            	}
            }

            return desc;
        }
        
        @Override
        public String toString() {
            return "Query '" + searchString + "' matched by " + sourceName + " to " + matchedNode.getProperty("name") + " (id=" +
                    matchedNode.getId() + "), score " + String.valueOf(score) + "; (" + getMatchType() + ")";
        }
    }
}