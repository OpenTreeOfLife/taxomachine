package opentree;

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
    TAXON_BY_NAME               ("taxNodesByName"),         // all taxon nodes by name
    TAXON_BY_SYNONYM            ("taxNodesBySyn"),          // all taxon nodes by synonym

    // preferred taxon nodes
    
    // all ranks
    PREFERRED_TAXON_BY_NAME     ("prefTaxNodesByName"),
    PREFERRED_TAXON_BY_SYNONYM  ("prefTaxNodesBySyn"),
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM  ("prefTaxNodesByNameOrSyn"),

    // species and subspecific ranks
    PREFERRED_TAXON_BY_NAME_SPECIES     ("prefTaxNodesByNameSpecies"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_SYNONYM_SPECIES  ("prefTaxNodesBySynSpecies"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_SPECIES  ("prefTaxNodesByNameOrSynSpecies"),

    // genera only
    PREFERRED_TAXON_BY_NAME_GENERA     ("prefTaxNodesByNameGenera"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_SYNONYM_GENERA  ("prefTaxNodesBySynGenera"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_GENERA  ("prefTaxNodesByNameOrSynGenera"),

    // higher taxa
    PREFERRED_TAXON_BY_NAME_HIGHER     ("prefTaxNodesByNameHigher"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_SYNONYM_HIGHER  ("prefTaxNodesBySynHigher"), // currently unused, here in case it becomes useful
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER  ("prefTaxNodesByNameOrSynHigher"),
    
    TAX_SOURCES                 ("taxSources"),             // ?
    TAX_STATUS                  ("taxStatus"),
    TAX_RANK                    ("taxRank"),
    
    TAXON_UNIQID				("taxUniqId");

    public final String namePrefix;

    NodeIndexDescription(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}