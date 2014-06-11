package org.opentree.taxonomy.constants;

import org.opentree.properties.OTPropertyPredicate;

public enum TaxonomyProperty implements OTPropertyPredicate {

	/**
	 * Identifies taxa with flags indicating suppression.
	 */
	DUBIOUS ("dubious", Boolean.class),

	/*
	 * The taxonomic name of this node taxon.
	 * 
	 * This has been replaced with ot:ottTaxonName.
	 *
	NAME ("name", String.class), */
	
	/**
	 * The taxonomic rank of the taxon.
	 */
	RANK ("rank", String.class),

	/**
	 * The taxonomic source of this taxon.
	 */
	SOURCE ("source", String.class),

	/**
	 * A unique taxonomic name for this taxon, determined during taxonomy compilation.
	 */
	UNIQUE_NAME ("uniqname", String.class),

	/**
	 * The OTT id of the genus that contains this taxon. Used for indexing.
	 */
	PARENT_GENUS_OTT_ID ("parent_genus_ott_id", Long.class),
	
	/**
	 * For deprecated taxa; the reason for deprecation (or generally, could be used for any other purpose).
	 */
	REASON ("reason", String.class),

	/**
	 * The original source information for this taxon.
	 */
	SOURCE_INFO ("source_info", String.class),
	
	/**
	 * ??? This might be redundant/deprecated
	 */
	INPUT_SOURCES ("input_sources", String.class),
	
	/**
	 * Whether or not this node represents a deprecated taxon. Assumed to be false if absent.
	 */
	DEPRECATED ("deprecated", Boolean.class),

	;

	private String propertyName;
	private Class<?> type;
	
	private TaxonomyProperty(String propertyName, Class<?> type) {
		this.propertyName = propertyName;
		this.type = type;
	}
	
	@Override
	public String propertyName() {
		return propertyName;
	}

	@Override
	public Class<?> type() {
		return type;
	}
}
