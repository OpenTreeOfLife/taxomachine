package org.opentree.taxonomy.constants;

import org.opentree.properties.OTPropertyPredicate;

public enum TaxonomyProperty implements OTPropertyPredicate {

	/**
	 * Identifies taxa with flags indicating suppression.
	 */
	DUBIOUS ("dubious", Boolean.class),

	/**
	 * The taxonomic name of this node taxon.
	 * 
	 * TODO::: this should be replaced with ot:ottTaxonName.
	 */
	NAME ("name", String.class),
	
	/**
	 * The taxonomic rank of the taxon.
	 */
	RANK ("rank", String.class),
	
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
