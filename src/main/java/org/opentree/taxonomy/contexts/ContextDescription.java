package org.opentree.taxonomy.contexts;

import org.opentree.taxonomy.contexts.ContextGroup;

/**
 * Definitions for the various taxonomic contexts. These are used to access the node indexes in order to locate nodes by name, synonym name, etc.
 * This enum defines the taxonomic scope of the various contexts. It is referred to by the TaxonomySynthesizer.makeContexts() method, which will
 * make context-specific node indexes for all the contexts defined here, and is also used when initializing an instance of the TaxonomyContext class,
 * through which access to all the context-specific node indexes themselves is provided.
 * 
 * Definitions for new contexts should be created in this enum.
 * 
 * For more information on how to use the context-specific indexes, refer to the documentation in the TaxonomyContext class file.
 * 
 * @author cody hinchliff
 *
 */
public enum ContextDescription {

    /* *** NOTE: names must be unique! */

    // Enum name         Name ***           Group                   Index suffix        Node name string    Nomenclature
    ALLTAXA             ("All life",        ContextGroup.LIFE,      "",                 "life",             Nomenclature.Undefined),

    // MICROBES group
    BACTERIA            ("Bacteria",        ContextGroup.MICROBES,  "Bacteria",         "Bacteria",         Nomenclature.ICNB),
    SAR					("SAR group"
    Archaea				()
    Excavata			()
    Amoebozoa			("Amoebozoans")
    Centrohelida		()
    Haptophyta			()
    Apusozoa			()
    Bacillariophyta		("Diatoms")
    Ciliophora			("Ciliates",		ContextGroup.MICROBES,	"Ciliates",			"Cilioophora",			Nomenclature.Undefined),
    Foraminifera		("Foraminifera",	ContextGroup.MICROBES,	"Forams",			"Foraminifera")

    // ANIMALS group
    METAZOA             ("Animals",         ContextGroup.ANIMALS,   "Animals",          "Metazoa",          Nomenclature.ICZN),
    BIRDS               ("Birds",           ContextGroup.ANIMALS,   "Birds",            "Aves",             Nomenclature.ICZN),
    TETRAPODS           ("Tetrapods",       ContextGroup.ANIMALS,   "Tetrapods",        "Tetrapoda",        Nomenclature.ICZN),
    MAMMALS             ("Mammals",         ContextGroup.ANIMALS,   "Mammals",          "Mammalia",         Nomenclature.ICZN),
    AMPHIBIANS          ("Amphibians",      ContextGroup.ANIMALS,   "Amphibians",       "Amphibia",         Nomenclature.ICZN),
    VERTEBRATES         ("Vertebrates",     ContextGroup.ANIMALS,   "Vertebrates",      "Vertebrata",       Nomenclature.ICZN),
    ARTHROPODS          ("Arthropods",      ContextGroup.ANIMALS,   "Arthopods",        "Arthropoda",       Nomenclature.ICZN),
    MOLLUSCS            ("Molluscs",        ContextGroup.ANIMALS,   "Molluscs",         "Mollusca",         Nomenclature.ICZN),
// name Nematodes may not be unique?
//  NEMATODES           ("Nematodes",       ContextGroup.ANIMALS,   "Nematodes",        "Nematoda",         Nomenclature.ICZN),
    PLATYHELMINTHES     ("Platyhelminthes", ContextGroup.ANIMALS,   "Platyhelminthes",  "Platyhelminthes",  Nomenclature.ICZN),
    ANNELIDS            ("Annelids",        ContextGroup.ANIMALS,   "Annelids",         "Annelida",         Nomenclature.ICZN),
    CNIDARIA            ("Cnidarians",      ContextGroup.ANIMALS,   "Cnidarians",       "Cnidaria",         Nomenclature.ICZN),
    ARACHNIDES          ("Arachnides",      ContextGroup.ANIMALS,   "Arachnids",		"Arachnida",        Nomenclature.ICZN),
    INSECTS             ("Insects",         ContextGroup.ANIMALS,   "Insects",          "Insecta",          Nomenclature.ICZN),

    // FUNGI group
    FUNGI               ("Fungi",           ContextGroup.FUNGI,     "Fungi",            "Fungi",            Nomenclature.ICBN),
    
    // PLANTS group
    LAND_PLANTS         ("Land plants",     ContextGroup.PLANTS,    "Plants",           "Embryophyta",      Nomenclature.ICBN),
    HORNWORTS           ("Hornworts",       ContextGroup.PLANTS,    "Anthocerotophyta", "Anthocerotophyta", Nomenclature.ICBN),
    MOSSES              ("Mosses",          ContextGroup.PLANTS,    "Bryophyta",        "Bryophyta",        Nomenclature.ICBN),
    LIVERWORTS          ("Liverworts",      ContextGroup.PLANTS,    "Marchantiophyta",  "Marchantiophyta",  Nomenclature.ICBN),
    VASCULAR_PLANTS     ("Vascular plants", ContextGroup.PLANTS,    "Tracheophyta",     "Tracheophyta",     Nomenclature.ICBN),
    LYCOPHYTES          ("Club mosses",     ContextGroup.PLANTS,    "Lycopodiophyta",   "Lycopodiophyta",   Nomenclature.ICBN),
    FERNS               ("Ferns",           ContextGroup.PLANTS,    "Moniliformopses",  "Moniliformopses",  Nomenclature.ICBN),
    SEED_PLANTS         ("Seed plants",     ContextGroup.PLANTS,    "Spermatophyta",    "Spermatophyta",    Nomenclature.ICBN),
    FLOWERING_PLANTS    ("Flowering plants",ContextGroup.PLANTS,    "Magnoliophyta",    "Magnoliophyta",    Nomenclature.ICBN),
//    MAGNOLIIDS          ("Magnoliids",      ContextGroup.PLANTS,    "Magnoliids",       "magnoliids",       Nomenclature.ICBN), // apparently the name 'magnoliids' no longer exists
    MONOCOTS            ("Monocots",        ContextGroup.PLANTS,    "Monocots",         "Liliopsida",       Nomenclature.ICBN),
    EUDICOTS            ("Eudicots",        ContextGroup.PLANTS,    "Eudicots",         "eudicotyledons",   Nomenclature.ICBN),
    ASTERIDS            ("Asterids",        ContextGroup.PLANTS,    "Asterids",         "asterids",         Nomenclature.ICBN),
    ROSIDS              ("Rosids",          ContextGroup.PLANTS,    "Rosids",           "rosids",           Nomenclature.ICBN);
    
    public final String name;
    public final ContextGroup group;
    public final String nameSuffix;
    public final String licaNodeName;
    public final Nomenclature nomenclature;

    ContextDescription (String label, ContextGroup group, String nameSuffix, String nodeName, Nomenclature nomenclature) {
        this.name = label;
        this.group = group;
        this.nameSuffix = nameSuffix;
        this.licaNodeName = nodeName;
        this.nomenclature = nomenclature;
    }
}