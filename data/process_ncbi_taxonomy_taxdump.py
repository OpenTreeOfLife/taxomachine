import sys,os,sqlite3
import os.path

"""
this processes the ncbi taxonomy tables for the synonyms and the 
names that will be included in the upload to the taxomachine
"""

"""
skipping
- X 
-environmental
-unknown
-unidentified
-endophyte
-uncultured
-scgc
-libraries

connecting these to there parents
-unclassified
-incertae sedis
"""

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print "python process_ncbi_taxonomy_taxdump.py download[T|F] outfile"
        sys.exit(0)
    
    download = sys.argv[1]
    outfile = open(sys.argv[2],"w")
    outfilesy = open(sys.argv[2]+".synonyms","w")
    if download.upper() == "T":
        print("downloading taxonomy")
        os.system("wget ftp://ftp.ncbi.nih.gov/pub/taxonomy/taxdump.tar.gz")
        os.system("tar -xzvf taxdump.tar.gz")

    if os.path.isfile("nodes.dmp") == False:
        print "nodes.dmp is not present"
        os.exit(0)
    if os.path.isfile("names.dmp") == False:
        print "names.dmp is not present"
        os.exit(0)

    nodesf = open("nodes.dmp","r")
    namesf = open("names.dmp","r")

    count = 0
    pid = {} #key is the child id and the value is the parent
    cid = {} #key is the parent and value is the list of children
    nrank = {} #key is the node id and the value is the rank
    for i in nodesf:
        spls = i.split("\t|\t")
        tid = spls[0].strip()
        parentid = spls[1].strip()
        rank = spls[2].strip()
        pid[tid] = parentid
        nrank[tid] = rank
        if parentid not in cid: 
            cid[parentid] = []
        cid[parentid].append(tid)
        count += 1
        if count % 100000 == 0:
            print count
    nodesf.close()

    skip = ["x","environmental","unknown","unidentified","endophyte","uncultured","scgc","libraries","virus"]
    count = 0
    classes = []
    idstoexclude = []
    nm_storage = {}
    lines = {}
    synonyms = {}
    for i in namesf:
        spls = i.strip().split("\t|") #if you do \t|\t then you don't get the name class right because it is "\t|"
        gid = spls[0].strip()
        par = pid[gid]
        nm = spls[1].strip()
        nm_c = spls[3].strip()
        if nm_c not in classes:
            classes.append(nm_c)
        nm_keep = True
        nms = nm.split(" ")
        for j in nms:
            if j in skip:
                nm_keep = False
        if nm_keep == False:
            idstoexclude.append(gid)
            continue
        if nm_c != "scientific name":
            if gid not in synonyms:
                synonyms[gid] = []
            synonyms[gid].append(i.strip())
        else:
            lines[gid] = i.strip()
            nm_storage[gid] = nm
        count += 1
    print "number of lines: ",count
    namesf.close()

    #now making sure that the taxonomy is functional before printing to the file

    skipids = []
    stack = idstoexclude

    
    while len(stack) != 0:
        curid = stack.pop()
        if curid in skipids:
            continue
        skipids.append(curid)
        if curid in cid:
            ids = cid[curid]
            for i in ids:
                stack.append(i)

    for i in skipids:
        if i in lines:
            del lines[i]
        if i in synonyms:
            del synonyms[i]
        if i in nm_storage:
            del nm_storage[i]
    
    print "number of scientific names: ",len(lines)
    print "number of synonyms: ",len(synonyms)

    """
    in this section we change the names of the parent child identical names for
    1) if parent and child have the same name higher than genus, they are sunk
    2) if the parent and child have the same name at genus and subspecies (etc), the subname
    is called genusname rank subgenus name
    """

    for i in nm_storage:
        if nm_storage[i] != "root":
            if i in pid:
                if nm_storage[i] == nm_storage[pid[i]]:
                #do something for the genus 
                    if nrank[pid[i]] == "genus":
                        nm_storage[i] = nm_storage[pid[i]]+" "+nrank[i]+" "+nm_storage[i]
                    else:
                        idstoch = cid[i]
                        for j in idstoch:
                            pid[j] = pid[i]
                        if i in synonyms:
                            for j in synonyms[i]:
                                if pid[i] in synonyms:
                                    synonyms[pid[i]].append(j)
                                else:
                                    synonyms[pid[i]] = [j]
                            del synonyms[i]
                        del lines[i]
                #do something for everything else
                

    #need to print id, parent id, and name   
    for i in lines:
        spls = lines[i].split("\t|\t")
        id = spls[0].strip()
        prid = pid[spls[0]].strip()
        sname = spls[1].strip()
        #changed from sname to nm_storage to fix the dup name issue
        outfile.write(id+"\t|\t"+prid+"\t|\t"+nm_storage[i]+"\t|\t\n")
    outfile.close()

    for i in synonyms:
        if i in lines:
            for j in synonyms[i]:
                spls = j.split("\t|\t")
                id = spls[0].strip()
                sname = spls[1].strip()
                nametp = spls[3].strip()
                outfilesy.write(id+"\t|\t"+sname+"\t|\t"+nametp+"\t|\t\n")
    outfilesy.close()