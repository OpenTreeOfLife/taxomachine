package org.neo4j.server.rest.repr;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.helpers.collection.FirstItemIterable;
import org.neo4j.helpers.collection.IteratorWrapper;

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

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting specific data types below here
	//
	// ///////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Return a Representation object capable of serializing `result` into a
	 * map
	 * 
	 * @param result
	 * @return
	 */
	public static TNRSResultsRepresentation getContextRepresentation(
			final ContextResult result) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {

			@Override
			protected void serialize(final MappingSerializer serializer) {

				serializer.putString("context_name", result.context.getDescription().name);
				serializer.putString("content_rootnode_ottol_id", result.context.getRootNode().getProperty("uid").toString());
				serializer.putList("ambiguous_names",
						OpentreeRepresentationConverter
								.getListRepresentation(result.namesNotMatched));

			}
		};
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// methods for converting TNRSResults and nested types below here
	//
	// ///////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Return an Representation object capable of serializing `results` into a
	 * complex nested map structure containing all the information returned by the TNRS
	 * 
	 * @param results
	 * @return
	 */
	public static TNRSResultsRepresentation getResultsRepresentation(
			final TNRSResults results) {
		return new TNRSResultsRepresentation(RepresentationType.MAP.toString()) {

			@Override
			protected void serialize(final MappingSerializer serializer) {

				HashMap<String, Object> tnrsResultsMap = new HashMap<String, Object>();

				tnrsResultsMap
						.put("governing_code", results.getGoverningCode());
				tnrsResultsMap.put("unambiguous_names",
						results.getNamesWithDirectMatches());
				tnrsResultsMap.put("unmatched_names",
						results.getUnmatchedNames());
				tnrsResultsMap.put("matched_names", results.getMatchedNames());
				tnrsResultsMap.put("context", results.getContextName());

				for (Map.Entry<String, Object> pair : tnrsResultsMap.entrySet()) {
					String key = pair.getKey();
					Object value = pair.getValue();

					if (value instanceof String) {
						serializer.putString(key, (String) value);

					} else if (value instanceof Set) {
						serializer.putList(key, OpentreeRepresentationConverter
								.getListRepresentation((Set) value));
					}
				}

				serializer.putList("results", OpentreeRepresentationConverter
						.getListRepresentation(results));
			}
		};
	}

	public static MappingRepresentation getNameResultRepresentation(
			TNRSNameResult r) {

		final HashMap<String, Object> nameResultMap = new HashMap<String, Object>();
		nameResultMap.put("queried_name", r.getQueriedName());
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
						serializer
								.putList(
										key,
										getMatchSetRepresentation((TNRSMatchSet) value));
					} else if (value instanceof TNRSMatch) {
						serializer.putMapping(key,
								getMatchRepresentation((TNRSMatch) value));
					}
				}
			}
		};
	}

	public static ListRepresentation getMatchSetRepresentation(
			final TNRSMatchSet matchSet) {

		FirstItemIterable<Representation> results = new FirstItemIterable<Representation>(
				new IteratorWrapper<Representation, Object>(
						(Iterator) matchSet.iterator()) {
					@Override
					protected Representation underlyingObjectToObject(
							Object value) {
						return getMatchRepresentation((TNRSMatch) value);
					}
				});
		return new ListRepresentation(RepresentationType.PROPERTIES, results);
	}

	public static MappingRepresentation getMatchRepresentation(
			final TNRSMatch match) {

		return new MappingRepresentation(RepresentationType.MAP.toString()) {
			@Override
			protected void serialize(final MappingSerializer serializer) {

				// should also have matchedNodeUniqueId, but this is not yet
				// available
				serializer.putNumber("matchedNodeId", match.getMatchedNode()
						.getId());
				serializer.putString("matchedName", match.getMatchedNode()
						.getProperty("name").toString());
				serializer.putString("matchedOttolID", match.getMatchedNode()
						.getProperty("uid").toString());
				serializer.putString("parentName", match.getParentNode()
						.getProperty("name").toString());
				serializer.putString("sourceName", match.getSource());
				serializer.putString("nomenCode", match.getNomenCode());
				serializer.putBoolean("isPerfectMatch",
						match.getIsPerfectMatch());
				serializer.putBoolean("isApprox", match.getIsApproximate());
				serializer.putString("searchString", match.getSearchString());
				serializer.putNumber("score", match.getScore());

				if (match.getNameStatusIsKnown()) {
					serializer.putString("matchedNameStatus", "known");
					serializer.putBoolean("isSynonym", match.getIsSynonym());
					serializer.putBoolean("isHomonym", match.getIsHomonym());
				} else {
					serializer.putString("matchedNameStatus", "uncertain");
				}
			}
		};
	}

	public static Representation getAutocompleteBoxResultsRepresentation(TNRSResults results) {

		
		// FILL THIS IN. Might want to put this in a different class.
		
		return null;
		
    }

	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// general serialization methods below here, mostly just copied from Neo4j
	// RepresentationConverter classes
	//
	// ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	String serialize(RepresentationFormat format, URI baseUri,
			ExtensionInjector extensions) {
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
