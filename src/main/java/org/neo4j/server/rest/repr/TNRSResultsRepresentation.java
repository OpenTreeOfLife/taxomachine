package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.opentree.properties.OTPropertyPredicate;
import org.opentree.properties.OTVocabularyPredicate;

import opentree.taxonomy.OTTFlag;
import opentree.tnrs.ContextResult;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;

public class TNRSResultsRepresentation extends MappingRepresentation {

	public TNRSResultsRepresentation(RepresentationType type) {
		super(type);
	}

	TNRSResultsRepresentation(String type) {
		super(type);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting specific data types below here
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////

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
				serializer.putNumber("content_rootnode_ottol_id", (Long) result.context.getRootNode().getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
				serializer.putList("ambiguous_name_ids", OpentreeRepresentationConverter.getListRepresentation(result.nameIdsNotMatched));

			}
		};
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting TNRSResults and nested types below here
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Return an Representation object capable of serializing `results` into a complex nested map structure containing all the information returned by the TNRS
	 * 
	 * @param results
	 * @return
	 */
	public static TNRSResultsRepresentation getResultsRepresentation(final TNRSResults results) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {

			@Override
			protected void serialize(final MappingSerializer serializer) {

				HashMap<String, Object> tnrsResultsMap = new HashMap<String, Object>();

				tnrsResultsMap.put("governing_code", results.getGoverningCode());
				tnrsResultsMap.put("unambiguous_name_ids", results.getNameIdsWithDirectMatches()); // was "unambiguous_names"
				tnrsResultsMap.put("unmatched_name_ids", results.getUnmatchedNameIds()); // was "unmatched_names"
				tnrsResultsMap.put("matched_name_ids", results.getMatchedNameIds()); // was "matched_names"
				tnrsResultsMap.put("context", results.getContextName());

				for (Map.Entry<String, Object> pair : tnrsResultsMap.entrySet()) {
					String key = pair.getKey();
					Object value = pair.getValue();

					if (value instanceof String) {
						serializer.putString(key, (String) value);

					} else if (value instanceof Set) {
						serializer.putList(key, OpentreeRepresentationConverter.getListRepresentation((Set) value));
					}
				}

				serializer.putList("results", OpentreeRepresentationConverter.getListRepresentation(results));
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

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(new IteratorWrapper<Representation, Object>((Iterator) matchSet.iterator()) {
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
				
				serializer.putNumber("matched_node_id", matchedNode.getId());
				serializer.putString("matched_name", matchedNode.getProperty("name").toString());
				serializer.putString("unique_name", match.getUniqueName());
				serializer.putString("rank", match.getRank());
				serializer.putNumber("matched_ott_id", (Long) matchedNode.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
				serializer.putString("parent_name", match.getParentNode().getProperty("name").toString());
				serializer.putString("source_name", match.getSource());
				serializer.putString("nomenclature_code", match.getNomenCode());
				serializer.putBoolean("is_perfect_match", match.getIsPerfectMatch());
				serializer.putBoolean("is_approximate_match", match.getIsApproximate());
				serializer.putString("search_string", match.getSearchString());
				serializer.putNumber("score", match.getScore());

				// check dubiousness
				boolean isDubious = false;
				if (matchedNode.hasProperty("dubious")) {
					isDubious = (Boolean) matchedNode.getProperty("dubious");
				}
				serializer.putBoolean("dubious_name", isDubious);

				// get all flags
	        	List<OTTFlag> flags = new LinkedList<OTTFlag>();
	        	for (OTTFlag flag : OTTFlag.values()) {
	        		if (matchedNode.hasProperty(flag.label)) {
	        			flags.add(flag);
	        		}
	        	}
				serializer.putList("flags", OpentreeRepresentationConverter.getListRepresentation(flags));
				
				
				if (match.getNameStatusIsKnown()) {
					serializer.putString("synonym_or_homonym_status", "known");
					serializer.putBoolean("is_synonym", match.getIsSynonym());
					serializer.putBoolean("is_homonym", match.getIsHomonym());
				} else {
					serializer.putString("synonym_or_homonym_status", "uncertain");
				}
			}
		};
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// these are for the autocomplete box query, and omit unnecessary information
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static ListRepresentation getMatchSetRepresentationForAutocompleteBox(final Iterator<TNRSMatch> matchIter) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(new IteratorWrapper<Representation, Object>((Iterator) matchIter) {
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

				// TODO: transition these to lower case with underscores
				serializer.putNumber("nodeId", match.getMatchedNode().getId()); // matched node id
				serializer.putNumber("ottId", (Long) match.getMatchedNode().getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName())); // matched ottol id
				serializer.putString("name", match.getUniqueName()); // unique name
				serializer.putBoolean("exact", match.getIsPerfectMatch()); // is perfect match
				serializer.putBoolean("higher", match.getIsHigherTaxon()); // is higher taxon
			}
		};
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// general serialization methods below here, mostly just copied from Neo4j RepresentationConverter classes
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////

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

}
