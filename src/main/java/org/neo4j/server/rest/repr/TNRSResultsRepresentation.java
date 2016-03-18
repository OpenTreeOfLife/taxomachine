package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.tnrs.ContextResult;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSMatchSet;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;

// MappingRepresentation is inherited from neo4j's
// org.neo4j.server.rest.repr package.

public class TNRSResultsRepresentation extends MappingRepresentation {

	public TNRSResultsRepresentation(RepresentationType type) {
		super(type);
	}

	TNRSResultsRepresentation(String type, final int apiVersion) {
		super(type);
        this.apiVersion = apiVersion;
	}

    // Really sorry about the fact that 'apiVersion' is passed
    // everywhere, but it seems the easiest way to keep both the v2
    // and the v3 code running at the same time.  A wholesale rewrite
    // could probably get rid of apiVersion.

    // Turns out this isn't used, because all methods are static
    int apiVersion;  // 2 or 3

	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting specific data types below here
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Return a Representation object capable of serializing `result` into a map
	 * 
	 * @param result
	 * @return
	 */
    // used by API
	public static TNRSResultsRepresentation getContextRepresentation(final ContextResult result, final int apiVersion) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString(), apiVersion) {

			@Override
			protected void serialize(final MappingSerializer serializer) {

				serializer.putString("context_name", result.context.getDescription().name);
				serializer.putNumber("context_ott_id", (Long) result.context.getRootNode().getProperty(
						OTVocabularyPredicate.OT_OTT_ID.propertyName()));
				serializer.putList("ambiguous_names", OTRepresentationConverter
						.getListRepresentation(result.namesNotMatched));

			}
		};
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting TNRSResults and nested types below here
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Return an Representation object capable of serializing `results` into a complex nested map structure containing
	 * all the information returned by the TNRS
	 * 
	 * @param results
	 * @return
	 */
    public static TNRSResultsRepresentation getResultsRepresentation(final TNRSResults results, final int apiVersion) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString(), apiVersion) {

			@Override
			protected void serialize(final MappingSerializer serializer) {
				serializer.putString("governing_code", results.getGoverningCode());
                boolean V3 = (apiVersion >= 3);
				serializer.putList((V3 ? "unambiguous_names" : "unambiguous_name_ids"),
						OTRepresentationConverter.getListRepresentation(results.getNameIdsWithDirectMatches()));
				serializer.putList((V3 ? "unmatched_names" : "unmatched_name_ids"), 
						OTRepresentationConverter.getListRepresentation(results.getUnmatchedNameIds()));
				serializer.putList((V3 ? "matched_names" : "matched_name_ids"),
						OTRepresentationConverter.getListRepresentation(results.getMatchedNameIds()));
				serializer.putString("context", results.getContextName());
				serializer.putBoolean("includes_deprecated_taxa", results.getIncludesDeprecated());
                if (apiVersion <= 2)
                    serializer.putBoolean("includes_dubious_names", results.getIncludesDubious());
                else
                    serializer.putBoolean("includes_suppressed_names", results.getIncludesDubious());
				serializer.putBoolean("includes_approximate_matches", results.getIncludesApproximate());
				serializer.putMapping("taxonomy",
						OTRepresentationConverter.getMapRepresentation(results.getTaxonomyMetdata()));
				serializer.putList("results", getResultsListRepresentation(results, apiVersion));
			}
		};
	}

    // Blob for matches for a single name

	private static MappingRepresentation getNameResultRepresentation(TNRSNameResult r, final int apiVersion) {

		final HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
        if (apiVersion <= 2)
            nameResultMap.put("id", r.getId());
        else
            nameResultMap.put("name", r.getId());
		nameResultMap.put("matches", r.getMatches());

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

				for (Map.Entry<String, Object> pair : nameResultMap.entrySet()) {
					String key = pair.getKey();
					Object value = pair.getValue();

					if (value instanceof String) {
						serializer.putString(key, (String) value);
					} else if (value instanceof TNRSMatchSet) {
						serializer.putList(key, getMatchSetRepresentation((TNRSMatchSet) value, apiVersion));
					} else if (value instanceof TNRSMatch) {
						serializer.putMapping(key, getMatchRepresentation((TNRSMatch) value, apiVersion));
					} else if (value instanceof Long) {
						serializer.putNumber(key, (Long) value);
					} else if (value instanceof Double) {
						serializer.putNumber(key, (Long) value);
					} else {
						throw new UnsupportedOperationException("unrecognized type for value: " + value + " of key " + key);
					}
				}
			}
		};
	}

	private static ListRepresentation getMatchSetRepresentation(final TNRSMatchSet matchSet, final int apiVersion) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
				new IteratorWrapper<Representation, Object>((Iterator) matchSet.iterator()) {
					@Override
					protected Representation underlyingObjectToObject(Object value) {
						return getMatchRepresentation((TNRSMatch) value, apiVersion);
					}
				});
		return new ListRepresentation(RepresentationType.PROPERTIES, results);
	}

	private static MappingRepresentation getMatchRepresentation(final TNRSMatch match, final int apiVersion) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

				Node matchedNode = match.getMatchedTaxon().getNode();

				// add these properties for all taxa
				serializer.putBoolean("is_approximate_match", match.getIsApproximate());
                if (apiVersion <= 2)
                    serializer.putNumber("matched_node_id", matchedNode.getId());
				serializer.putString("search_string", match.getSearchString());
				serializer.putString("matched_name", match.getMatchedName());
				serializer.putNumber("score", match.getScore());
                serializer.putBoolean("is_synonym", match.getIsSynonym());
                serializer.putString("nomenclature_code", match.getNomenCode());

                // Taxon - in v3 all this information has to go into a separate blob

                if (apiVersion <= 2)
                    addTaxonInfo(match, serializer, apiVersion);
                else
                    serializer.putMapping("taxon", getTaxonRepresentation(match, apiVersion));
            }
        };
    }

	private static MappingRepresentation getTaxonRepresentation(final TNRSMatch match, final int apiVersion) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

                // Taxon - in v3 all this information has to go into a separate blob

                addTaxonInfo(match, serializer, apiVersion);
            }
        };
    }

    // Compare addTaxonInfo in plugin taxonomy_v3

    private static void addTaxonInfo(final TNRSMatch match, final MappingSerializer serializer, final int apiVersion) {

				Node matchedNode = match.getMatchedTaxon().getNode();

                String name = (String) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName());
                if (apiVersion <= 2)
                    serializer.putString(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);
                else
                    serializer.putString("name", name);
                if (apiVersion <= 2)
                    serializer.putNumber(OTVocabularyPredicate.OT_OTT_ID.propertyName(),
                                         (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
                else
                    serializer.putNumber("ott_id",
                                         (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));

                // Need tax_sources here!  Damn - requires another class !?  !!!!!!

				// check if taxon is deprecated
				boolean isDeprecated = match.getIsDeprecated();
                if (apiVersion <= 2)
                    serializer.putBoolean("is_deprecated", isDeprecated);

				if (isDeprecated) {
					serializer.putList("flags", OTRepresentationConverter.getListRepresentation(Arrays.asList(new String[] {"DEPRECATED",})));

				} else {
					// only add these properties for non-deprecated taxa
                    String uname = match.getUniqueName();
                    if (apiVersion >= 3 && uname.length() == 0) uname = name;
					serializer.putString("unique_name", uname); // TODO; update this to use the TaxonomyProperty enum once the unique_name field is fixed in ott input
					serializer.putString("rank", match.getRank());

					/*
					Node pNode = match.getMatchedTaxon().getParentTaxon().getNode();
					if (pNode != null) {
						serializer.putString("parent_name", (String) match.getMatchedTaxon().getParentTaxon().getNode().getProperty(
								OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
					} */

					serializer.putList("synonyms", OTRepresentationConverter.getListRepresentation(match.getSynonyms()));
					
					// check dubiousness
					Boolean isSuppressed = Boolean.FALSE;
					if (matchedNode.hasProperty(TaxonomyProperty.DUBIOUS.propertyName())) {
						isSuppressed = (Boolean) matchedNode.getProperty(TaxonomyProperty.DUBIOUS.propertyName());
					}
                    if (apiVersion <= 2)
                        serializer.putBoolean("is_dubious", isSuppressed);
                    else
                        serializer.putBoolean("is_suppressed", isSuppressed);

					// get all flags
					List<OTTFlag> flags = new LinkedList<OTTFlag>();
					for (OTTFlag flag : OTTFlag.values()) {
						if (matchedNode.hasProperty(flag.label)) {
							flags.add(flag);
						}
					}
					serializer.putList("flags", OTRepresentationConverter.getListRepresentation(flags));
				}
    }

	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// these are for the autocomplete box query, and omit unnecessary information
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

    // used by API
	public static ListRepresentation getMatchSetRepresentationForAutocompleteBox(final Iterator<TNRSMatch> matchIter, final int apiVersion) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
				new IteratorWrapper<Representation, Object>((Iterator) matchIter) {
					@Override
					protected Representation underlyingObjectToObject(Object value) {
						return getMatchRepresentationForAutocompleteBox((TNRSMatch) value, apiVersion);
					}
				});
		return new ListRepresentation(RepresentationType.PROPERTIES, results);
	}

	private static MappingRepresentation getMatchRepresentationForAutocompleteBox(final TNRSMatch match, final int apiVersion) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {
				Node matchedNode = match.getMatchedTaxon().getNode();
                if (apiVersion <= 2)
                    serializer.putNumber("node_id", matchedNode.getId()); // matched node id
                if (apiVersion <= 2)
                    serializer.putNumber(OTVocabularyPredicate.OT_OTT_ID.propertyName(), (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())); // matched ott id
                else
                    serializer.putNumber("ott_id", (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())); // matched ott id
                String uname = match.getUniqueName();
                if (apiVersion >= 3 && uname.length() == 0)
                    uname = (String)matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName());
				serializer.putString("unique_name", uname); // unique name

				serializer.putBoolean("is_higher", match.getIsHigherTaxon()); // is higher taxon ... num_tips would be more consistent with synth
                if (apiVersion <= 2)
                    serializer.putBoolean("is_dubious", match.getIsDubious());  // is hidden by virtue of having some suppressed flag
                else
                    serializer.putBoolean("is_suppressed", match.getIsDubious());  // is hidden by virtue of having some suppressed flag
			}
		};
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// general serialization methods below here, mostly just copied from Neo4j RepresentationConverter classes
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	String serialize(RepresentationFormat format, URI baseUri, ExtensionInjector extensions) {
		MappingWriter writer = format.serializeMapping(type);
		Serializer.injectExtensions(writer, this, baseUri, extensions);
		serialize(new MappingSerializer(writer, baseUri, extensions));
		writer.done();
		return format.complete(writer);
	}

	@Override
	void addTo(ListSerializer serializer) {
		serializer.addMapping(this);
	}

	@Override
	void putTo(MappingSerializer serializer, String key) {
		serializer.putMapping(key, this);
	}

	@Override
	protected void serialize(MappingSerializer serializer) {}

	// ========= private methods

	/**
	 * Return a serialization of a general Iterable type
	 * 
	 * @param data
	 * @return
	 */
	private static ListRepresentation getResultsListRepresentation(TNRSResults data, final int apiVersion) {
		final FirstItemIterable<Representation> results = // convertValuesToRepresentations(data);

		new FirstItemIterable<Representation>(new IterableWrapper<Representation, Object>((Iterable) data) {

			@Override
			protected Representation underlyingObjectToObject(Object value) {

				// if (value instanceof TNRSNameResult) {
				return getNameResultRepresentation((TNRSNameResult) value, apiVersion);
			}
		});
		return new ListRepresentation(RepresentationType.MAP.toString(), results);
	}
}
