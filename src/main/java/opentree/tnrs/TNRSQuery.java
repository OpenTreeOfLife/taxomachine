package opentree.tnrs;

import java.util.HashSet;
import java.util.Set;

import opentree.Taxon;
import opentree.TaxonSet;
import opentree.Taxonomy;
import opentree.NodeIndexDescription;
import opentree.RelType;
import opentree.TaxonomyContext;
import opentree.utils.Levenshtein;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Provides methods and various options for performing TNRS queries.
 * @author cody hinchliff
 * 
 */
public class TNRSQuery {

    
    private static final double DEFAULT_MIN_SCORE = 0.01;
    private static final double PERFECT_SCORE = 1;
    private static final int SHORT_NAME_LENGTH = 9;
    private static final int MEDIUM_NAME_LENGTH = 14;
    private static final int LONG_NAME_LENGTH = 19;
    private static final String DEFAULT_TAXONOMY_NAME = "ottol";

    private Taxonomy taxonomy;
    private TNRSResults results;
    private HashSet<String> queriedNames;
    private double minScore;
    
    private TaxonomyContext context;
    private Taxon bestGuessLICAForNames;

    // To store taxa/names for which we can/cannot find direct (exact, n=1) matches
    private HashSet<Taxon> taxaWithDirectMatches;

    private Index<Node> prefTaxNodesByName;
    
    public TNRSQuery(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
        prefTaxNodesByName = taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
        context = null;
        minScore = DEFAULT_MIN_SCORE; // TODO: make it possible for client to set this
        clearResults();
    }
    
    private void clearResults() {
        queriedNames = new HashSet<String>();
        results = new TNRSResults();
        
        context = null;
        bestGuessLICAForNames = null;
        
        taxaWithDirectMatches = new HashSet<Taxon>();
    }

    /**
     * Initialize the query object with a set of names. The passed `context` may be set to null, in which case an attempt to infer the context
     * based on the names will be made ((if necessary) by the appropriate functions, or the context can be explicitly inferred via a call to
     * `inferContext()`.
     * @param searchStrings
     * @param context
     */
    public TNRSQuery initialize(Set<String> searchStrings, TaxonomyContext context) {
        clearResults();
        this.context = context;
        if (context != null) {
            System.out.println(context.getDescription().name);
            results.setContextName(this.context.getDescription().name);
        }
        queriedNames = (HashSet<String>) searchStrings;
        return this;
    }

    /**
     * Perform a full TNRS query, i.e. using supplied or inferred context, matching all names to exact taxon names, exact synonyms, and
     * approximate taxon names and synonyms, and return the results as a TNRSResults object.
     * @return
     */
    public TNRSResults doFullTNRS() {
        
        HashSet<String> namesWithoutDirectTaxnameMatches = new HashSet<String>();
        HashSet<String> namesWithoutDirectSynonymMatches = new HashSet<String>();
        HashSet<String> namesWithoutApproxTaxnameOrSynonymMatches = new HashSet<String>();
        HashSet<String> unmatchableNames = new HashSet<String>();

        // infer context if we need to, and determine names to be matched against it
        HashSet<String> namesToMatchAgainstContext = new HashSet<String>();
        if (context == null) {
            inferContext(namesToMatchAgainstContext);
        } else {
            results.setGoverningCode(context.getDescription().nomenclature.code);
            namesToMatchAgainstContext = queriedNames;
        }
        
        // direct match unmatched names within context
        getExactTaxonMatches(namesToMatchAgainstContext, namesWithoutDirectTaxnameMatches);
        
        // direct match unmatched names against synonyms
        getExactSynonymMatches(namesWithoutDirectTaxnameMatches, namesWithoutDirectSynonymMatches);
        
        // TODO: external concept resolution for still-unmatched names? (direct match returned concepts against context)
        // this will need an external concept-resolution service, which as yet does not seem to exist...

        // do fuzzy matching for any names we couldn't match
        getApproxTaxnameOrSynonymMatches(namesWithoutDirectSynonymMatches, namesWithoutApproxTaxnameOrSynonymMatches);
        
        // last-ditch effort to match yet-unmatched names: try truncating names in case there are accession-id modifiers
        getApproxTaxnameOrSynonymMatches(namesWithoutApproxTaxnameOrSynonymMatches, unmatchableNames);

        // record unmatchable names to results
        for (String name : unmatchableNames)
            results.addUnmatchedName(name);
                    
        return results;
    }
    
    /**
     * Attempt to infer a context for the current set of names by looking for direct matches to the names in `queriedNames` on
     * the entire graph, determining the LICA for these names (calls `updateLICA()`) and then determining the least inclusive
     * context for the inferred LICA. The resulting TaxonomyContext object is remembered internally and returned.
     * @param searchStrings
     * @return inferred TaxonomyContext for names
     */
    public TaxonomyContext inferContext(HashSet<String> unmatchableNames) {
        // No user-defined context, so we need to infer one. First we will look for unambiguous
        // matches on entire graph (remembering direct matches to valid homonyms separately)

        for (String thisName : queriedNames) {

            // Attempt to find exact matches against *ALL* preferred taxa
            IndexHits<Node> hits = prefTaxNodesByName.query("name", thisName);
            if (hits.size() == 1) { // not a homonym

                // WE (MUST) ASSUME that users have spelled names correctly, but havoc will ensure if this assumption
                // is violated, as mispelled names are likely to yield direct matches to distantly related taxa!

                // add this taxon to the list of unambigous matches
                Taxon matchedTaxon = taxonomy.getTaxon(hits.getSingle());
                taxaWithDirectMatches.add(matchedTaxon);

                // add the match to the TNRS results
                TNRSMatchSet matches = new TNRSMatchSet();
                matches.addMatch(new TNRSHit().
                        setMatchedTaxon(matchedTaxon).
                        setSearchString(thisName).
                        setIsPerfectMatch(true).
                        setIsApprox(false).
                        setIsHomonym(false).
                        setNomenCode(matchedTaxon.getNomenCode()).
                        setSourceName(DEFAULT_TAXONOMY_NAME).
                        setScore(PERFECT_SCORE));

                results.addNameResult(new TNRSNameResult(thisName, matches));
                results.addNameWithDirectMatch(thisName);

            } else { // is either a homonym match or a non-matched name
                unmatchableNames.add(thisName);
            }

            hits.close();
        }
        
        // find the lica of the unambiguous (non-homonym) hits
        updateLICA();

        // now we can determine a context to use from here on
        context = bestGuessLICAForNames.getLeastInclusiveContext();
        results.setGoverningCode(context.getDescription().nomenclature.code);
        results.setContextName(context.getDescription().name);

        return context;
    }
    
    /**
     * Search for exact taxon name matches to names in `searchStrings`, adding names that cannot be matched to `namesWithoutDirectTaxnameMatches`, and
     * Taxon objects for names that can be matched to `taxaWithDirectMatches`. Finally will call `updateLICA()` to reflect any newly matched taxa.
     * @param searchStrings
     */
    private void getExactTaxonMatches(HashSet<String> searchStrings, HashSet<String> unmatchableNames) {

        // exact match the names against the context; save all hits
        for (String thisName : searchStrings) {

            IndexHits<Node> hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME).query("name", thisName);

            if (hits.size() < 1) {
                // no direct matches, move on to next name
                unmatchableNames.add(thisName);
                continue;

            } else {
                // at least 1 hit; prepare to record matches
                TNRSMatchSet matches = new TNRSMatchSet();

                // determine within-context homonym status
                boolean isHomonym = false;
                if (hits.size() > 1)
                    isHomonym = true;

                for (Node hit : hits) {
                    // add this match to the match set
                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
                    matches.addMatch(new TNRSHit().
                            setMatchedTaxon(matchedTaxon).
                            setSearchString(thisName).
                            setIsPerfectMatch(!isHomonym). // here it's either a direct match to an in-context homonym or a perfect match
                            setIsApprox(false).
                            setIsHomonym(isHomonym).
                            setNomenCode(matchedTaxon.getNomenCode()).
                            setSourceName(DEFAULT_TAXONOMY_NAME).
                            setScore(PERFECT_SCORE));
                    
                    if (isHomonym == false) {
                        taxaWithDirectMatches.add(matchedTaxon);
                        results.addNameWithDirectMatch(thisName);
                    }
                }

                // add matches to the TNRS results
                results.addNameResult(new TNRSNameResult(thisName, matches));
            }
            hits.close();
        }
        
        // update the LICA to reflect any new direct hits
        updateLICA();
    }

    /**
     * Search for exact synonym matches to names in `searchStrings`, adding names that cannot be matched to `namesWithoutDirectSynonymMatches`
     * @param searchStrings
     */
    private void getExactSynonymMatches(HashSet<String> searchStrings, HashSet<String> unmatchableNames) {
        
        // exact match unmatched names against context synonym index
        for (String thisName : searchStrings) {

            IndexHits<Node> hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM).query("name", thisName);

            if (hits.size() < 1) {
                // no direct matches, move on to next name
                unmatchableNames.add(thisName);
                continue;

            } else {
                // at least 1 hit; prepare to record matches
                TNRSMatchSet matches = new TNRSMatchSet();

                for (Node hit : hits) {
                    // add this match to the match set
                    Taxon matchedTaxon = taxonomy.getTaxon(hit);
                    matches.addMatch(new TNRSHit().
                            setMatchedTaxon(matchedTaxon).
                            setSearchString(thisName).
                            setIsPerfectMatch(false).
                            setIsApprox(false).
                            setIsHomonym(false).
                            setIsSynonym(true).
                            setNomenCode(matchedTaxon.getNomenCode()).
                            setSourceName(DEFAULT_TAXONOMY_NAME).
                            setScore(PERFECT_SCORE));
                }

                // add matches to the TNRS results
                results.addNameResult(new TNRSNameResult(thisName, matches));
            }
            
            hits.close();
        }
    }
    
    /**
     * Search for approximate taxon name or synonym matches to names in `searchStrings`, adding names that
     * cannot be matched to `namesWithoutApproxTaxnameOrSynonymMatches`.
     * @param searchStrings
     */
    private void getApproxTaxnameOrSynonymMatches(HashSet<String> searchStrings, HashSet<String> unmatchableNames) {

        for (String thisName : searchStrings) {
            
            // fuzzy match names against ALL within-context taxa and synonyms
            float minIdentity = getMinIdentity(thisName);
            IndexHits<Node> hits = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM).
                    query(new FuzzyQuery(new Term("name", thisName), minIdentity));

            if (hits.size() < 1) {
                // no direct matches, move on to next name
                unmatchableNames.add(thisName);
                continue;

            } else {
                // at least 1 hit; prepare to record matches
                TNRSMatchSet matches = new TNRSMatchSet();
                
                for (Node hit : hits) {
                    
                    Taxon matchedTaxon = taxonomy.getTaxon(hit);

                    // use edit distance to calculate base score for fuzzy matches
                    //          System.out.println("comparing " + queriedName + " to " + matchedTaxon.getName());
                    double l = Levenshtein.distance(thisName, matchedTaxon.getName());
                    //          System.out.println("l = " + String.valueOf(l));
                    double s = Math.min(matchedTaxon.getName().length(), thisName.length());
                    //          System.out.println("s = " + String.valueOf(s));
                    double baseScore = (s - l) / s;
                    //          System.out.println("baseScore = " + String.valueOf(baseScore));
                    
                    // weight scores by distance outside of inferred lica (this may need to go away if it is a speed bottleneck)
                    double scoreModifier = 1;
                    if (matchedTaxon.isPreferredTaxChildOf(bestGuessLICAForNames) == false) {
                        int d = taxonomy.getInternodalDistThroughMRCA(hit, bestGuessLICAForNames.getNode(), RelType.PREFTAXCHILDOF);
                        scoreModifier *= (1/Math.log(d)); // down-weight fuzzy matches outside of mrca scope by abs distance to mrca
                        System.out.println("scoreModifier = " + String.valueOf(scoreModifier));
                    }
                    
                    // add the match if it scores high enough
                    double score = baseScore * scoreModifier;
                    if (score >= minScore) {
                        matches.addMatch(new TNRSHit().
                                setMatchedTaxon(matchedTaxon).
                                setSearchString(thisName).
                                setIsPerfectMatch(false).
                                setIsApprox(true).
                                setNameStatusIsKnown(false).
                                setNomenCode(matchedTaxon.getNomenCode()).
                                setSourceName(DEFAULT_TAXONOMY_NAME).
                                setScore(score));
                    }
                }

                // add the matches (if any) to the TNRS results
                if (matches.size() > 0)
                    results.addNameResult(new TNRSNameResult(thisName, matches));
                else
                    unmatchableNames.add(thisName);
            }
            hits.close();
        }
    }

    /**
     * Update the inferred LICA of the known direct matches.
     */
    private void updateLICA() {
        // update the lica to reflect all direct hits
        TaxonSet ts = new TaxonSet(taxaWithDirectMatches);
        if (ts.size() > 0)
            bestGuessLICAForNames = ts.getLICA();
        else
            bestGuessLICAForNames = taxonomy.getTaxon(taxonomy.ALLTAXA.getRootNode());
    }
    
    /**
     * Returns a minimum identity score used for fuzzy matching that limits the number
     * of edit differences based on the length of the string. 
     * 
     * @param name
     * @return minIdentity
     */
    public float getMinIdentity(String name) {
        
        float ql = name.length();

        int maxEdits = 4;

        if (ql < SHORT_NAME_LENGTH)
            maxEdits = 1;
        else if (ql < MEDIUM_NAME_LENGTH)
            maxEdits = 2;
        else if (ql < LONG_NAME_LENGTH)
            maxEdits = 3;
            
        return (ql - (maxEdits + 1)) / ql;
    }
    
    /**
     * A function for converting string arrays to HashSets of strings, provided as a convenience for preparing string arrays for TNRS.
     * @param input strings
     * @return a set of Strings containing the input
     */
    public HashSet<String> stringArrayToHashset(String[] strings) {
        HashSet<String> stringSet = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            stringSet.add(strings[i]);
        }
        return stringSet;
    }
    
    // TODO: create methods that leverage external adapters to match names

    // methods providing access to different simplified query options follow
    /**
     * Returns *ONLY* exact matches to a single string `searchString`; the context is determined automatically.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults matchExact(String searchString) {
        HashSet<String> name = new HashSet<String>();
        return matchExact(name, null);
    }
    
    /**
     * Returns *ONLY* exact matches to `searchStrings` within `context`.
     *         
     * @param searchStrings
     * @param context
     * @return results
     * 
     */
    public TNRSResults matchExact(Set<String> searchStrings, TaxonomyContext context) {
        
        initialize(searchStrings, context);

        // match names against context
        HashSet<String> namesWithoutDirectTaxnameMatches = new HashSet<String>();
        getExactTaxonMatches(queriedNames, namesWithoutDirectTaxnameMatches);

        // record the names that couldn't be matched
        for (String name : namesWithoutDirectTaxnameMatches) {
            results.addUnmatchedName(name);
        }
        
        return results;
    }

    /**
     * Returns *ONLY* exact matches to `searchStrings`; the taxonomic context is determined automatically.
     *         
     * @param searchStrings
     * @return results
     * 
     */
    public TNRSResults matchExact(Set<String> searchStrings) {
        initialize(searchStrings, null);

        HashSet<String> namesToMatchAgainstContext = new HashSet<String>();
        HashSet<String> namesWithoutDirectTaxnameMatches = new HashSet<String>();

        // infer the context, re-match unmatched names against inferred context
        inferContext(namesToMatchAgainstContext);
        getExactTaxonMatches(namesToMatchAgainstContext, namesWithoutDirectTaxnameMatches);

        // record names that still couldn't be matched against inferred context
        for (String name : namesWithoutDirectTaxnameMatches) {
            results.addUnmatchedName(name);
        }
        
        return results;
    }
}
