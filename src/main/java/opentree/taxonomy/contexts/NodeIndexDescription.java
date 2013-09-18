package opentree.taxonomy.contexts;

/**
 * This enum defines all the types of indexes that are accessible from the TaxonomyContext objects. This includes ALL
 * node indexes, including those whose scope is the entire taxonomy, as well as those with more limited scope. Each one of
 * the name prefixes defined in this enum is combined with the a name suffix from the Taxonomy.ContextDescription enum to
 * yield a complete name for an index that will be created by the TaxonomySynthesizer.makeContexts() method.
 * 
 * For more information and for the procedures themselves for building the node indexes, refer to the
 * TaxonomySynthesizer.makeContexts() method and the various methods it calls.
 * 
 * For information on how to access the node indexes in order to query the taxonomy for nodes by name, etc., refer to the
 * documentation in the TaxonomyContext class file.
 * 
 * @author cody hinchliff
 *
 */
public enum NodeIndexDescription {
	
	// all taxon indexes
	
	/** Records all taxa. Field is "name" and key is taxon name. */
    TAXON_BY_NAME               ("taxNodesByName"),         // all taxon nodes by name

	/** Records all taxa. Field is "uid" and key is ott id. */
    TAXON_BY_OTT_ID 			("taxNodesByOTTId"),         // all taxon nodes by name

	/** Records all taxa. Field is "name" and key is synonymous name. */
    TAXON_BY_SYNONYM            ("taxNodesBySyn"),          // all taxon nodes by synonym
    
    /** Records all taxa. Field is "name", key is taxon name or synonymous name. */
    TAXON_BY_NAME_OR_SYNONYM            ("taxNodesByNameOrSyn"),          // all taxon nodes by name or synonym

    /** Records all taxa. Field is "rank" and key is taxon rank. */
    TAXON_BY_RANK					 ("taxNodesByRank"),

    /** Records all taxa. Field is "flag" and key is flag name. */
    TAXON_BY_FLAG					("taxNodesByFlag"),

    // preferred taxon indexes
    
    /** Records preferred taxa. Field is "rank" and key is taxon rank. */
    PREFERRED_TAXON_BY_RANK			 ("prexTaxNodesByRank"),
    
    /** Records species and infraspecific ranks. Field is "genus_uid" and key is the ott id of the enclosing genus. */
    PREFERRED_SPECIES_BY_GENUS		("prefSpeciesNodesByGenus"),
    
    // all ranks

    /** Records all preferred taxa. Field is "name", key is taxon name. */
    PREFERRED_TAXON_BY_NAME     ("prefTaxNodesByName"),

    /** Records all preferred taxa. Field is "name", key is synonymous name. */
    PREFERRED_TAXON_BY_SYNONYM  ("prefTaxNodesBySyn"),

    /** Records all preferred taxa. Field is "name", key is taxon name or synonymous name. */
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM  ("prefTaxNodesByNameOrSyn"),

    // species and subspecific ranks

    /** Records preferred species and infraspecific taxa. Field is "name", key is taxon name. */
    PREFERRED_TAXON_BY_NAME_SPECIES     ("prefTaxNodesByNameSpecies"),

//    /** Records preferred species and infraspecific taxa. Field is "name", key is synonymous name. */
//    PREFERRED_TAXON_BY_SYNONYM_SPECIES  ("prefTaxNodesBySynSpecies"),

//    /** Records preferred species and infraspecific taxa. Field is "name", key is taxon name or synonymous name. */
//    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_SPECIES  ("prefTaxNodesByNameOrSynSpecies"),

    // genera only

    /** Records preferred genera. Field is "name", key is taxon name. */
    PREFERRED_TAXON_BY_NAME_GENERA     ("prefTaxNodesByNameGenera"),

//    /** Records preferred genera. Field is "name", key is synonymous name. */
//    PREFERRED_TAXON_BY_SYNONYM_GENERA  ("prefTaxNodesBySynGenera"),

//    /** Records preferred genera. Field is "name", key is taxon name or synonymous name. */
//    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_GENERA  ("prefTaxNodesByNameOrSynGenera"),

    // higher taxa

    /** Records preferred higher taxa. Field is "name", key is taxon name. */
    PREFERRED_TAXON_BY_NAME_HIGHER     ("prefTaxNodesByNameHigher"),

//    /** Records preferred taxa. Field is "name", key is synonymous name. */
//    PREFERRED_TAXON_BY_SYNONYM_HIGHER  ("prefTaxNodesBySynHigher"), // currently unused, here in case it becomes useful

    /** Records preferred higher taxa. Field is "name", key is taxon name or synonymous name. */
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER  ("prefTaxNodesByNameOrSynHigher"), 

    /** Stores information about the source taxonomies used to build OTT. */
    TAXONOMY_SOURCES                 ("taxSources"),
    
    /** Not used. Was used for preottol. */
    @Deprecated
    TAX_STATUS                  ("taxStatus"),

    /** Not used. Was used for preottol. */
    @Deprecated
    TAX_RANK                    ("taxRank"),
    
    /** Not used. Not clear what this is for. If this becomes used, please document what it is for. */
    @Deprecated
    TAXON_UNIQID				("taxUniqId");

    public final String namePrefix;

    NodeIndexDescription(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}