package org.opentree.taxonomy.contexts;

import org.opentree.taxonomy.contexts.ContextGroup;

/**
 * Definitions for the various taxonomic contexts. These are used to access the node indexes in order to locate nodes by name, synonym name, etc.
 * This enum defines the taxonomic scope of the various contexts. It is referred to by the TaxonomySynthesizer.makeContexts    () method, which will
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

    /* *** NOTE: names must be unique!  TODO: use ott ids instead of names, to avoid this... */

    // Enum name         Name ***           Group                   Index suffix        Node name string    ott id        Nomenclature
    ALLTAXA             ("All life",        ContextGroup.LIFE,      "",                 "life",             805080L,    Nomenclature.Undefined),

    // MICROBES group
    BACTERIA            ("Bacteria",        ContextGroup.MICROBES,  "Bacteria",         "Bacteria",         844192L,    Nomenclature.ICNP),
    SAR                 ("SAR group",       ContextGroup.MICROBES,  "SAR",              "SAR",              5246039L,   Nomenclature.Undefined),
    ARCHAEA             ("Archaea",         ContextGroup.MICROBES,  "Archaea",          "Archaea",          996421L,    Nomenclature.ICNP),
    EXCAVATA            ("Excavata",        ContextGroup.MICROBES,  "Excavata",         "Excavata",         2927065L,   Nomenclature.Undefined),
    AMOEBAE             ("Amoebozoa",       ContextGroup.MICROBES,  "Amoebae",          "Amoebozoa",        1064655L,   Nomenclature.ICZN),
    CENTROHELIDA        ("Centrohelida",    ContextGroup.MICROBES,  "Centrohelida",     "Centrohelida",     755852L,    Nomenclature.ICZN),
    HAPTOPHYTA          ("Haptophyta",      ContextGroup.MICROBES,  "Haptophyta",       "Haptophyta",       151014L,    Nomenclature.Undefined),
    APUSOZOA            ("Apusozoa",        ContextGroup.MICROBES,  "Apusozoa",         "Apusozoa",         671092L,    Nomenclature.ICZN),
    DIATOMS             ("Diatoms",         ContextGroup.MICROBES,  "Diatoms",          "Bacillariophyta",  5342311L,   Nomenclature.ICN),
    CILIATES            ("Ciliates",        ContextGroup.MICROBES,  "Ciliates",         "Ciliophora",       302424L,    Nomenclature.Undefined),
    FORAMS              ("Forams",          ContextGroup.MICROBES,  "Forams",           "Foraminifera",     936399L,    Nomenclature.ICZN),

    // ANIMALS group
    METAZOA             ("Animals",         ContextGroup.ANIMALS,   "Animals",          "Metazoa",          691846L,    Nomenclature.ICZN),
    BIRDS               ("Birds",           ContextGroup.ANIMALS,   "Birds",            "Aves",             81461L,     Nomenclature.ICZN),
    TETRAPODS           ("Tetrapods",       ContextGroup.ANIMALS,   "Tetrapods",        "Tetrapoda",        229562L,    Nomenclature.ICZN),
    MAMMALS             ("Mammals",         ContextGroup.ANIMALS,   "Mammals",          "Mammalia",         244265L,    Nomenclature.ICZN),
    AMPHIBIANS          ("Amphibians",      ContextGroup.ANIMALS,   "Amphibians",       "Amphibia",         544595L,    Nomenclature.ICZN),
    VERTEBRATES         ("Vertebrates",     ContextGroup.ANIMALS,   "Vertebrates",      "Vertebrata",       801601L,    Nomenclature.ICZN),
    ARTHROPODS          ("Arthropods",      ContextGroup.ANIMALS,   "Arthopods",        "Arthropoda",       632179L,    Nomenclature.ICZN),
    MOLLUSCS            ("Molluscs",        ContextGroup.ANIMALS,   "Molluscs",         "Mollusca",         802117L,    Nomenclature.ICZN),
    NEMATODES           ("Nematodes",       ContextGroup.ANIMALS,   "Nematodes",        "Nematoda",         395057L,    Nomenclature.ICZN),
    PLATYHELMINTHES     ("Platyhelminthes", ContextGroup.ANIMALS,   "Platyhelminthes",  "Platyhelminthes",  555379L,    Nomenclature.ICZN),
    ANNELIDS            ("Annelids",        ContextGroup.ANIMALS,   "Annelids",         "Annelida",         941620L,    Nomenclature.ICZN),
    CNIDARIA            ("Cnidarians",      ContextGroup.ANIMALS,   "Cnidarians",       "Cnidaria",         641033L,    Nomenclature.ICZN),
    ARACHNIDES          ("Arachnids",       ContextGroup.ANIMALS,   "Arachnids",        "Arachnida",        511967L,    Nomenclature.ICZN),
    INSECTS             ("Insects",         ContextGroup.ANIMALS,   "Insects",          "Insecta",          1062253L,   Nomenclature.ICZN),

    // FUNGI group
    FUNGI               ("Fungi",           ContextGroup.FUNGI,     "Fungi",            "Fungi",            352914L,    Nomenclature.ICN),
    BASIDIOMYCOTA       ("Basidiomycetes",  ContextGroup.FUNGI,     "Basidiomycetes",   "Basidiomycota",    634628L,    Nomenclature.ICN),
    ASCOMYCOTA          ("Ascomycetes",     ContextGroup.FUNGI,     "Ascomycota",       "Ascomycota",       439373L,    Nomenclature.ICN),
    
    // PLANTS group
    LAND_PLANTS         ("Land plants",     ContextGroup.PLANTS,    "Plants",           "Embryophyta",      5342313L,   Nomenclature.ICN),
    HORNWORTS           ("Hornworts",       ContextGroup.PLANTS,    "Anthocerotophyta", "Anthocerotophyta", 738980L,    Nomenclature.ICN),
    MOSSES              ("Mosses",          ContextGroup.PLANTS,    "Bryophyta",        "Bryophyta",        246594L,    Nomenclature.ICN),
    LIVERWORTS          ("Liverworts",      ContextGroup.PLANTS,    "Marchantiophyta",  "Marchantiophyta",  56601L,     Nomenclature.ICN),
    VASCULAR_PLANTS     ("Vascular plants", ContextGroup.PLANTS,    "Tracheophyta",     "Tracheophyta",     10210L,     Nomenclature.ICN),
    // In OTT 3.1 we lost Lycopodiophyta, so we can't really deal with extinct Lycopodiophyta that aren't Lycopodiopsida ...
    LYCOPHYTES          ("Club mosses",     ContextGroup.PLANTS,    "Lycopodiopsida",   "Lycopodiopsida",   144795L,    Nomenclature.ICN),
    FERNS               ("Ferns",           ContextGroup.PLANTS,    "Moniliformopses",  "Moniliformopses",  166292L,    Nomenclature.ICN),
    SEED_PLANTS         ("Seed plants",     ContextGroup.PLANTS,    "Spermatophyta",    "Spermatophyta",    10218L,     Nomenclature.ICN),
    FLOWERING_PLANTS    ("Flowering plants",ContextGroup.PLANTS,    "Magnoliophyta",    "Magnoliophyta",    99252L,     Nomenclature.ICN),
    MONOCOTS            ("Monocots",        ContextGroup.PLANTS,    "Monocots",         "Liliopsida",       1058517L,   Nomenclature.ICN),
    EUDICOTS            ("Eudicots",        ContextGroup.PLANTS,    "Eudicots",         "eudicotyledons",   431495L,    Nomenclature.ICN),
    ROSIDS              ("Rosids",          ContextGroup.PLANTS,    "Rosids",           "rosids",           1008296L,   Nomenclature.ICN),
    ASTERIDS            ("Asterids",        ContextGroup.PLANTS,    "Asterids",         "asterids",         1008294L,   Nomenclature.ICN),
    ASTERALES           ("Asterales",       ContextGroup.PLANTS,    "Asterales",        "Asterales",        1042120L,   Nomenclature.ICN),
    ASTERACEAE          ("Asteraceae",      ContextGroup.PLANTS,    "Asteraceae",       "Asteraceae",       46248L,   	Nomenclature.ICN),    
    ASTER          		("Aster",      		ContextGroup.PLANTS,    "Aster",       		"Aster",       		409712L,   	Nomenclature.ICN),    
    SYMPHYOTRICHUM		("Symphyotrichum",  ContextGroup.PLANTS,    "Symphyotrichum",   "Symphyotrichum",   1058735L,   Nomenclature.ICN),
    CAMPANULACEAE		("Campanulaceae",  	ContextGroup.PLANTS,    "Campanulaceae",   	"Campanulaceae",   	1086303L,   Nomenclature.ICN),
    LOBELIA				("Lobelia",  		ContextGroup.PLANTS,    "Lobelia",   		"Lobelia",   		1086294L,   Nomenclature.ICN),

    ;
    
    public final String name;
    public final ContextGroup group;
    public final String nameSuffix;
    public final String licaNodeName;
    public final Long ottId;
    public final Nomenclature nomenclature;

    ContextDescription     (String label, ContextGroup group, String nameSuffix, String nodeName, Long ottId, Nomenclature nomenclature) {
        this.name = label;
        this.group = group;
        this.nameSuffix = nameSuffix;
        this.licaNodeName = nodeName;
        this.ottId = ottId;
        this.nomenclature = nomenclature;
    }
}
