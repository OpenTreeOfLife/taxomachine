package opentree.tnrs;

import java.util.HashMap;
import java.util.HashSet;

import opentree.taxonomy.TaxonomyExplorer;

import org.forester.io.parsers.FastaParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

/**
 * @author Cody Hinchliff
 * 
 *         Provides methods for performing TNRS queries given a search string according to various options.
 */

public class TNRSQuery {

    private static final double TEMP_SCORE = 0.5;

    private String _graphName;
    private TaxonomyExplorer _taxonomy;
    private TNRSMatchSet _results;
    private HashSet<String> _unmatchedNames;
    private HashMap<String, Boolean> _matchedNames;

    public TNRSQuery(String graphName) {
        _graphName = graphName;
        _taxonomy = new TaxonomyExplorer(_graphName);
        _matchedNames = new HashMap<String, Boolean>();
    }

    /**
     * @param searchString
     * @param adapters
     * @return results
     * 
     *         Performs a TNRS query on 'searchString', and loads the results of the query into a TNRSMatchSet object called 'results', which is returned.
     *         'adapters' is either one or an array of TNRSAdapter objects for the sources to be queried. If no adapters are passed, then the query is only
     *         performed against the local taxonomy graph.
     */
    public TNRSMatchSet getMatches(String[] searchStrings, TNRSAdapter... adapters) {
        _results = new TNRSMatchSet(_graphName);
        _unmatchedNames = new HashSet<String>();

        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];

            if (_matchedNames.containsKey(thisName)) {
                continue;
            }

            // first: check the graph index. if we find any hits, we're done
            IndexHits<Node> matchedNodes = _taxonomy.findTaxNodeByName(thisName);
            if (matchedNodes.size() > 0) {
                for (Node n : matchedNodes) {

                    // check to see if the matched node is a homonym
                    IndexHits<Node> hitsForName = _taxonomy.findTaxNodeByName((String) n.getProperty("name"));
                    boolean isHomonym = false;
                    if (hitsForName.size() > 1)
                        isHomonym = true;

                    // add the match
                    _results.addMatch(new TNRSHit().
                            setMatchedNode(n).
                            setSearchString(thisName).
                            setIsExactNode(true).
                            setIsHomonym(isHomonym)
                            .setSourceName("ottol"));
                }
                _matchedNames.put(thisName, true);
                continue; // no need to look at other options
            }

            // TODO: second: try direct matches to synonyms. if we find a hit, we're done
            // make sure we don't try to match names twice
            // (need to interface with stephen about synonyms)

            // no direct matches, so we look at all other options
            boolean wasMatched = false;

            // third: check for fuzzy matches to recognized taxa
            IndexHits<Node> fuzzyMatches = _taxonomy.findTaxNodeByNameFuzzy(thisName);
            if (fuzzyMatches.size() > 0) {
                for (Node n : fuzzyMatches) {

                    // check to see if the fuzzily matched node is a homonym
                    IndexHits<Node> directMatches = _taxonomy.findTaxNodeByName(n.getProperty("name").toString());
                    boolean isHomonym = false;
                    if (directMatches.size() > 1)
                        isHomonym = true;

                    // add the match
                    _results.addMatch(new TNRSHit().
                            setIsApprox(true).
                            setIsHomonym(isHomonym).
                            setMatchedNode(n).
                            setScore(TEMP_SCORE). // TODO: record scores with fuzzy matches
                            setSearchString(thisName).
                            setSourceName("ottol"));
                }
                wasMatched = true;
            }

            // TODO: fourth: check for fuzzy matches to synonyms
            // make sure we don't try to match names twice

            // remember names we can't match within the graph
            if (wasMatched)
                _matchedNames.put(thisName, true);
            else
                _unmatchedNames.add(thisName);
        }

        // fourth: call passed adapters for help with names we couldn't match
        if (_unmatchedNames.size() > 0) {
            for (int i = 0; i < adapters.length; i++) {
                adapters[i].doQuery(_unmatchedNames, _taxonomy, _results);
            }
        }
        return _results;
    }

    public HashSet<String> getUnmatchedNames() {
        return _unmatchedNames;
    }
}
