package org.opentree.tnrs;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.constants.TaxonomyRelType;

import java.util.ArrayList;
import java.util.Iterator;
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
    @Override
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
        private String matchedName;						// the name that was matched against during the search. may differ from the name of the taxon that was matched, e.g. if the matched name was a synonym
        private String nomenCode;                      // the nomenclatural code under which this match is defined
        private boolean isApprox;                     // whether this is a fuzzy match (presumably misspellings)
        private boolean isSynonym;                    // whether the match points to a known synonym
        private String rank;							// the taxonomic rank
        private double score;                         // the score of this match
        
        public Match(TNRSHit m) {
            matchedNode = m.getMatchedNode();
            matchedName = m.getMatchedName();
            nomenCode = m.getNomenCode();
            isApprox = m.getIsApprox();
            isSynonym = m.getIsSynonym();
            rank = m.getRank();
            searchString = m.getSearchString();
            score = m.getScore();
        }
        
        /**
         * @return the original search string which produced this match
         */
		@Override
		public String getSearchString() {
            return searchString;
        }

		/**
		 * @return the name that was matched against during the TNRS search process
		 */
		@Override
		public String getMatchedName() {
			return matchedName;
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
			if (matchedNode == null) {
				return null;
			}
            
            Relationship pRel = matchedNode.getSingleRelationship(TaxonomyRelType.TAXCHILDOF, Direction.OUTGOING);
            if (pRel == null) {
            	return null;
            }
            
            Node p = pRel.getStartNode();

            if (! p.equals(matchedNode)) {
                return p;
            } else {
                return null;
            }
        }

		@Override
		public boolean getIsDeprecated() {
			if (matchedNode.hasProperty(TaxonomyProperty.DEPRECATED.propertyName())) {
				return (Boolean) matchedNode.getProperty(TaxonomyProperty.DEPRECATED.propertyName());
			} else {
				return false;
			}
        }
		
		@Override
		public boolean getIsDubious() {
			if (matchedNode.hasProperty(TaxonomyProperty.DUBIOUS.propertyName())) {
				return (Boolean) matchedNode.getProperty(TaxonomyProperty.DUBIOUS.propertyName());
			} else {
				return false;
			}
		}

		@Override
		public boolean getIsApproximate() {
            return isApprox;
        }

		@Override
		public String getNomenCode() {
            return nomenCode;
        }

		@Override
		public boolean getIsSynonym() {
            return isSynonym;
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
		public double getScore() {
            return score;
        }
        
		@Override
		public String getUniqueName() {
			String name = (String) getMatchedNode().getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName());
			if (matchedNode.hasProperty(TaxonomyProperty.UNIQUE_NAME.propertyName())) {
				String uniqueName = (String) getMatchedNode().getProperty(TaxonomyProperty.UNIQUE_NAME.propertyName());
				if (! uniqueName.equals("")) {
					name = uniqueName;
				}
			}
			return name;
		}
        
        @Override
        public String toString() {
        	
        	String nameType = "";
            if (isSynonym) {
            	nameType = "synonym name";
            } else {
                if (matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()) != getUniqueName()) {
                	nameType = "non-unique taxon name";
                } else {
                	nameType = "unique taxon name";
                }
            }

        	String matchType = isApprox ? "Approximate" : "Exact";
        	
          return matchType + " match from search string '" + searchString + "\' to " + nameType + " '" + matchedName + "' for ot:ottId=" +
                    String.valueOf(matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())) + "(score=" +
                    String.valueOf(score) + ")";
        }
    }
}