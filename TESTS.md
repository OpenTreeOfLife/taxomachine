Currently, there are no automated tests for taxomachine or its services. You may test the services by querying them using cURL or other clients. Here are some cURL calls and their expected return values (for the ott 2.8 taxonomy):

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/about
```

```json
{
  "author" : "open tree of life project",
  "weburl" : "https://github.com/OpenTreeOfLife/opentree/wiki/Open-Tree-Taxonomy",
  "source" : "ott2.8"
}
```

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/taxon -H "content-type:application/json" -d '{"ott_id":766177}'
```

```json
{
  "ot:ottId" : 766177,
  "rank" : "species",
  "flags" : [ ],
  "synonyms" : [ "Garcinia mangostana L.", "mangosteen", "Garcinia mangostana" ],
  "ot:ottTaxonName" : "Garcinia mangostana",
  "unique_name" : "",
  "node_id" : 4110374
}
```

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/subtree -H "content-type:application/json" -d '{"ott_id":372706}'
```

```json
{
  "subtree" : "(Canis_sp._Russia/33_500,Canis_sp._Belgium/36_000,Canis_lepophagus,Canis_armbrusteri,Canis_ferox,Canis_dirus,Canis_alopex,Canis_edwardii,Canis_apolloniensis,Canis_cedazoensis,Canis_primigenius,Canis_hyaena,Canis_spelaeus,Canis_lycaon,Canis_simensis,(Canis_mesomelas_elongae)Canis_mesomelas,Canis_aureus,Canis_adustus,Canis_sp._CANInt1,Canis_indica,Canis_himalayensis,(Canis_lupus_arctos,Canis_lupus_x_Canis_lupus_familiaris,Canis_lupus_campestris,Canis_lupus_lupaster,Canis_lupus_lupus,Canis_lupus_signatus,Canis_lupus_labradorius,Canis_lupus_dingo,Canis_lupus_hodophilax,Canis_lupus_mogollonensis,Canis_lupus_familiaris,Canis_lupus_desertorum,Canis_lupus_hattai,Canis_lupus_laniger,Canis_lupus_baileyi,Canis_lupus_chanco,Canis_lupus_pallipes)Canis_lupus,Canis_latrans,Canis_rufus)Canis"
}
```

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/lica -H "content-type:application/json" -d '{"ott_ids":[5551856,821970,770319]}'
```

```json
{
  "lica" : {
    "ot:ottId" : 770319,
    "rank" : "family",
    "flags" : [ "EDITED" ],
    "ot:ottTaxonName" : "Canidae",
    "synonyms" : [ "Canidae", "dog, coyote, wolf, fox" ],
    "unique_name" : "",
    "node_id" : 3568697
  },
  "ott_ids_not_found" : [ ]
}
```

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/deprecated_taxa
```

```json
[ {
  "ot:ottId" : 4085853,
  "reason" : "same-parent/direct",
  "ot:ottTaxonName" : "Octomyces",
  "source_info" : "gbif:7245488"
}, # snipped
{
  "ot:ottId" : 3702879,
  "reason" : "any-source-id-2",
  "ot:ottTaxonName" : "Illosporium album",
  "source_info" : "if:195216,if:195456,gbif:2562032"
} ]
```

```bash
curl -X POST http://devapi.opentreeoflife.org/taxomachine/ext/taxonomy/graphdb/flagged_taxa -H "content-type:application/json" -d '{"flag":"not_otu"}'
```

```json
{
  "not_otu" : 39123
}
```
