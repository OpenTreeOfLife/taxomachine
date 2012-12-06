package opentree;

import opentree.ContextGroup;

/**
 * Definitions for the various taxonomic contexts. These descriptions can be passed to the Taxonomy.getContext() function to return a corresponding TaxonomyContext object that can be used for limited  (i.e. more efficient) access to individual parts of the taxonomy itself.
 * @author cody hinchliff
 *
 */
public enum ContextDescription {

    // Name              Label              Group                   Index suffix        Node name string    Nomenclature
    ALLTAXA             ("All life",        ContextGroup.LIFE,      "",                 "life",             Nomenclature.Undefined),

    // FUNGI
    BACTERIA            ("Bacteria",        ContextGroup.BACTERIA,  "Bacteria",         "Bacteria",          Nomenclature.ICNB),

    // ANIMALS
    METAZOA             ("Animals",         ContextGroup.ANIMALS,   "Animals",          "Metazoa",          Nomenclature.ICZN),

    // FUNGI
    FUNGI               ("Fungi",           ContextGroup.FUNGI,     "Fungi",            "Fungi",            Nomenclature.ICBN),
    
    // PLANTS
    LAND_PLANTS         ("Land plants",     ContextGroup.PLANTS,    "Plants",           "Embryophyta",      Nomenclature.ICBN),
    HORNWORTS           ("Hornworts",       ContextGroup.PLANTS,    "Anthocerotophyta", "Anthocerotophyta", Nomenclature.ICBN),
    MOSSES              ("Mosses",          ContextGroup.PLANTS,    "Bryophyta",        "Bryophyta",        Nomenclature.ICBN),
    LIVERWORTS          ("Liverworts",      ContextGroup.PLANTS,    "Marchantiophyta",  "Marchantiophyta",  Nomenclature.ICBN),
    VASCULAR_PLANTS     ("Vascular plants", ContextGroup.PLANTS,    "Tracheophyta",     "Tracheophyta",     Nomenclature.ICBN),
    LYCOPHYTES          ("Club mosses",     ContextGroup.PLANTS,    "Lycopodiophyta",   "Lycopodiophyta",   Nomenclature.ICBN),
    FERNS               ("Ferns",           ContextGroup.PLANTS,    "Moniliformopses",  "Moniliformopses",  Nomenclature.ICBN),
    SEED_PLANTS         ("Seed plants",     ContextGroup.PLANTS,    "Spermatophyta",    "Spermatophyta",    Nomenclature.ICBN),
    FLOWERING_PLANTS    ("Flowering plants", ContextGroup.PLANTS,   "Magnoliophyta",    "Magnoliophyta",    Nomenclature.ICBN),
    MAGNOLIIDS          ("Magnoliids",      ContextGroup.PLANTS,    "Magnoliids",       "magnoliids",       Nomenclature.ICBN),
    MONOCOTS            ("Monocots",        ContextGroup.PLANTS,    "Monocots",         "Liliopsida",       Nomenclature.ICBN),
    EUDICOTS            ("Eudicots",        ContextGroup.PLANTS,    "Eudicots",         "eudicotyledons",   Nomenclature.ICBN),
    ASTERIDS            ("Asterids",        ContextGroup.PLANTS,    "Asterids",         "asterids",         Nomenclature.ICBN),
    ROSIDS              ("Rosids",          ContextGroup.PLANTS,    "Rosids",           "rosids",           Nomenclature.ICBN);
    
    public final String label;
    public final ContextGroup group;
    public final String nameSuffix;
    public final String licaNodeName;
    public final Nomenclature nomenclature;

    ContextDescription (String label, ContextGroup group, String nameSuffix, String nodeName, Nomenclature nomenclature) {
        this.label = label;
        this.group = group;
        this.nameSuffix = nameSuffix;
        this.licaNodeName = nodeName;
        this.nomenclature = nomenclature;
    }
}