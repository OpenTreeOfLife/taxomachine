package org.opentree.taxonomy;

import org.neo4j.graphdb.RelationshipType;

public enum TaxonomyRelType implements RelationshipType {
    TAXCHILDOF,     // standard rel for tax db, from node to parent
    SYNONYMOF,      // relationship pointing at a taxon node from a synonym node
    METADATAFOR,    // relationship connecting a metadata node to the root of a taxonomy
    PREFTAXCHILDOF  // relationship type for preferred taxonomic relationships
}
