package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.opentree.properties.OTPropertyPredicate;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.tnrs.ContextResult;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSMatchSet;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;

public class TNRSResultsRepresentation extends MappingRepresentation {

	public TNRSResultsRepresentation(RepresentationType type) {
		super(type);
	}

	TNRSResultsRepresentation(String type) {
		super(type);
	}

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
	public static TNRSResultsRepresentation getContextRepresentation(final ContextResult result) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {

			@Override
			protected void serialize(final MappingSerializer serializer) {

				serializer.putString("context_name", result.context.getDescription().name);
				serializer.putNumber("context_rootnode_ott_id", (Long) result.context.getRootNode().getProperty(
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
	public static TNRSResultsRepresentation getResultsRepresentation(final TNRSResults results) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {

			@Override
			protected void serialize(final MappingSerializer serializer) {
				serializer.putString("governing_code", results.getGoverningCode());
				serializer.putList("unambiguous_name_ids",
						OTRepresentationConverter.getListRepresentation(results.getNameIdsWithDirectMatches()));
				serializer.putList("unmatched_name_ids", 
						OTRepresentationConverter.getListRepresentation(results.getUnmatchedNameIds()));
				serializer.putList("matched_name_ids",
						OTRepresentationConverter.getListRepresentation(results.getMatchedNameIds()));
				serializer.putString("context", results.getContextName());
				serializer.putBoolean("includes_deprecated_ids", results.getIncludesDeprecated());
				serializer.putBoolean("includes_dubious_names", results.getIncludesDubious());
				serializer.putBoolean("includes_approximate_matches", results.getIncludesApproximate());
				serializer.putMapping("taxonomy",
						OTRepresentationConverter.getMapRepresentation(results.getTaxonomyMetdata()));
				serializer.putList("results", getResultsListRepresentation(results));
			}
		};
	}

	public static MappingRepresentation getNameResultRepresentation(TNRSNameResult r) {

		final HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
		nameResultMap.put("id", r.getId());
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
						serializer.putList(key, getMatchSetRepresentation((TNRSMatchSet) value));
					} else if (value instanceof TNRSMatch) {
						serializer.putMapping(key, getMatchRepresentation((TNRSMatch) value));
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

	public static ListRepresentation getMatchSetRepresentation(final TNRSMatchSet matchSet) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
				new IteratorWrapper<Representation, Object>((Iterator) matchSet.iterator()) {
					@Override
					protected Representation underlyingObjectToObject(Object value) {
						return getMatchRepresentation((TNRSMatch) value);
					}
				});
		return new ListRepresentation(RepresentationType.PROPERTIES, results);
	}

	public static MappingRepresentation getMatchRepresentation(final TNRSMatch match) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

				Node matchedNode = match.getMatchedNode();

				// add these properties for all taxa
//				serializer.putBoolean("is_perfect_match", match.getIsPerfectMatch());
				serializer.putBoolean("is_approximate_match", match.getIsApproximate());
				serializer.putNumber("matched_node_id", matchedNode.getId());
//				serializer.putString("matched_name", (String) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
//				serializer.putNumber("matched_ott_id", (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
				serializer.putString(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(),
						(String) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
				serializer.putNumber(OTVocabularyPredicate.OT_OTT_ID.propertyName(),
						(Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
				serializer.putString("search_string", match.getSearchString());
				serializer.putNumber("score", match.getScore());

				// check if taxon is deprecated
				boolean isDeprecated = match.getIsDeprecated();
				serializer.putBoolean("is_deprecated", isDeprecated);

				if (isDeprecated) {
					serializer.putList("flags", OTRepresentationConverter.getListRepresentation(Arrays.asList(new String[] {"DEPRECATED",})));

				} else {
					// only add these properties for non-deprecated taxa
					serializer.putString("unique_name", match.getUniqueName());
					serializer.putString("rank", match.getRank());
					serializer.putString("nomenclature_code", match.getNomenCode());
					Node pNode = match.getParentNode();
					if (pNode != null) {
						serializer.putString("parent_name", (String) match.getParentNode().getProperty(
								OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()));
					}
					
					// check dubiousness
					boolean isDubious = false;
					if (matchedNode.hasProperty(TaxonomyProperty.DUBIOUS.propertyName())) {
						isDubious = (Boolean) matchedNode.getProperty(TaxonomyProperty.DUBIOUS.propertyName());
					}
					serializer.putBoolean("dubious_name", isDubious);
	
					// get all flags
					List<OTTFlag> flags = new LinkedList<OTTFlag>();
					for (OTTFlag flag : OTTFlag.values()) {
						if (matchedNode.hasProperty(flag.label)) {
							flags.add(flag);
						}
					}
					serializer.putList("flags", OTRepresentationConverter.getListRepresentation(flags));
					
					if (match.getNameStatusIsKnown()) {
						serializer.putString("synonym_or_homonym_status", "known");
						serializer.putBoolean("is_synonym", match.getIsSynonym());
						serializer.putBoolean("is_homonym", match.getIsHomonym());
					} else {
						serializer.putString("synonym_or_homonym_status", "uncertain");
					}
				}
			}
		};
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// these are for the autocomplete box query, and omit unnecessary information
	//
	// ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static ListRepresentation getMatchSetRepresentationForAutocompleteBox(final Iterator<TNRSMatch> matchIter) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
				new IteratorWrapper<Representation, Object>((Iterator) matchIter) {
					@Override
					protected Representation underlyingObjectToObject(Object value) {
						return getMatchRepresentationForAutocompleteBox((TNRSMatch) value);
					}
				});
		return new ListRepresentation(RepresentationType.PROPERTIES, results);
	}

	public static MappingRepresentation getMatchRepresentationForAutocompleteBox(final TNRSMatch match) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

				// TODO: THESE ARE DEPRECATED, REMOVE AT NEXT VERSION OF API
				serializer.putNumber("nodeId", match.getMatchedNode().getId()); // matched node id
				serializer.putNumber("ottId", (Long) match.getMatchedNode().getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())); // matched ott id

				serializer.putNumber("node_id", match.getMatchedNode().getId()); // matched node id
				serializer.putNumber("ott_id", (Long) match.getMatchedNode().getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())); // matched ott id
				serializer.putString("name", match.getUniqueName()); // unique name
				serializer.putBoolean("exact", match.getIsPerfectMatch()); // is perfect match
				serializer.putBoolean("higher", match.getIsHigherTaxon()); // is higher taxon
				serializer.putBoolean("dubious", match.getIsDubious());  // is hidden by virtue of having some suppressed flag
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
	protected void serialize(MappingSerializer serializer) {
	}

	// ========= private methods

	/**
	 * Return a serialization of a general Iterable type
	 * 
	 * @param data
	 * @return
	 */
	public static ListRepresentation getResultsListRepresentation(TNRSResults data) {
		final FirstItemIterable<Representation> results = // convertValuesToRepresentations(data);

		new FirstItemIterable<Representation>(new IterableWrapper<Representation, Object>((Iterable) data) {

			@Override
			protected Representation underlyingObjectToObject(Object value) {

				// if (value instanceof TNRSNameResult) {
				return getNameResultRepresentation((TNRSNameResult) value);

				/*
				 * } else if (value instanceof Iterable) { final FirstItemIterable<Representation> nested =
				 * convertValuesToRepresentations((Iterable) value); return new ListRepresentation(getType(nested),
				 * nested);
				 * 
				 * } else if (value instanceof Map<?, ?>) { return
				 * GeneralizedMappingRepresentation.getMapRepresentation((Map<String, Object>) value);
				 */
				// } else {
				// return null;
				// }
			}
		});
		return new ListRepresentation(RepresentationType.MAP.toString(), results);
	}

	/*
	 * static FirstItemIterable<Representation> convertValuesToRepresentations(Iterable data) {
	 * 
	 * return new FirstItemIterable<Representation>( new IterableWrapper<Representation, Object>(data) {
	 * 
	 * @Override protected Representation underlyingObjectToObject(Object value) {
	 * 
	 * if (value instanceof TNRSNameResult) { return getNameResultRepresentation((TNRSNameResult)value);
	 * 
	 * /* } else if (value instanceof Iterable) { final FirstItemIterable<Representation> nested =
	 * convertValuesToRepresentations((Iterable) value); return new ListRepresentation(getType(nested), nested);
	 * 
	 * } else if (value instanceof Map<?, ?>) { return
	 * GeneralizedMappingRepresentation.getMapRepresentation((Map<String, Object>) value);
	 * 
	 * 
	 * } else { return null; } } }); }
	 */

}
