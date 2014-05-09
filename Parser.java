/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.jena.atlas.lib.Sink ;
import org.apache.jena.riot.RDFDataMgr ;
import org.apache.jena.riot.out.SinkTripleOutput ;
import org.apache.jena.riot.system.StreamRDF ;
import org.apache.jena.riot.system.StreamRDFBase ;
import org.apache.jena.riot.system.SyntaxLabels ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.Triple ;
import org.apache.jena.atlas.io.AWriter ;
import org.apache.jena.atlas.io.IO ;
import org.apache.jena.riot.out.*;
import com.hp.hpl.jena.rdf.model.Property ;
import com.hp.hpl.jena.sparql.vocabulary.FOAF ;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.net.*;

public class Parser
{
    public static void main(String...argv)
    {
        String filename = argv[0];
        String uri = argv[1];

        // This is the heart of N-triples printing ... outoput is heavily buffered
        // so the FilterSinkRDF called flush at the end of parsing.
        //Sink<Triple> output = new SinkTripleOutput(System.out, null, SyntaxLabels.createNodeToLabel()) ;
        StreamRDF filtered = new FilterSinkRDF(System.out) ;
        
        // Call the parsing process. 
        RDFDataMgr.parse(filtered, filename) ;

        // after all files are created add metadata and controls to each file 
        // (split into different files if necessary)
        listFilesForFolder(new File("ldf/"), uri);
        /*
        File dir = new File("ldf/");
        File[] directoryListing = dir.listFiles();
        for(File child : directoryListing) {
            Writer writer = null;
            try {
                FileOutputStream fileOutStream = new FileOutputStream(child, true); 
                writer = new BufferedWriter(new OutputStreamWriter(fileOutStream, "utf-8"));
                writer.write(controls(child, uri));
            } catch (IOException ex) {
              // report
            } finally {
               try {writer.close();} catch (Exception ex) {}
            }
        }
        */



        // write start.ttl as a fragment to start from
        Writer writer = null;
        try {
            FileOutputStream fileOutStream = new FileOutputStream("ldf/start.ttl"); 
            writer = new BufferedWriter(new OutputStreamWriter(fileOutStream, "utf-8"));
            writer.write(controls(new File("start.ttl"), uri));
        } catch (IOException ex) {
          // report
        } finally {
           try {writer.close();} catch (Exception ex) {}
        }
    }
    public static void listFilesForFolder(final File folder, String uri) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, uri);
            } else {
                //System.out.println(fileEntry.getName());
                Writer writer = null;
                try {
                    FileOutputStream fileOutStream = new FileOutputStream(fileEntry, true); 
                    writer = new BufferedWriter(new OutputStreamWriter(fileOutStream, "utf-8"));
                    writer.write(controls(fileEntry, uri));
                } catch (IOException ex) {
                  // report
                } finally {
                   try {writer.close();} catch (Exception ex) {}
                }
            }
        }
    }
    public static String getRelativePath(File file, File folder) {
        String filePath = file.getAbsolutePath();
        String folderPath = folder.getAbsolutePath();
        if (filePath.startsWith(folderPath)) {
            return filePath.substring(folderPath.length() + 1);
        } else {
            return null;
        }
    }


    public static String controls(File file, String uri) {
        String fileName = file.getName();
        String filePath = getRelativePath(file, new File("ldf/"));
        int numOfLines = 0;
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            numOfLines = lnr.getLineNumber();
            lnr.close();
        } catch(IOException ex) {
        } finally {
            // Finally, the LineNumberReader object should be closed to prevent resource leak
        }
String dataToWrite = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n"
+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.\n"
+ "@prefix hydra: <http://www.w3.org/ns/hydra/core#>.\n"
+ "@prefix void: <http://rdfs.org/ns/void#>.\n"
+ "@prefix dcterms: <http://purl.org/dc/terms/>.\n"
+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.\n"
+ "@prefix : <http://foo.org/>.\n"
+ "\n"
+ ":a a void:Dataset, hydra:Collection;\n"
+ "    void:subset <"+uri+filePath+">;\n"
+ "    void:uriLookupEndpoint \""+uri+"s{+s}p{+p}o{+o}.ttl\";\n"
+ "    hydra:search _:triplePattern.\n"
+ "_:triplePattern hydra:template \""+uri+"s{+s}p{+p}o{+o}.ttl\";\n"
+ "    hydra:mapping _:subject, _:predicate, _:object.\n"
+ "_:subject hydra:variable \"s\";\n"
+ "    hydra:property rdf:subject.\n"
+ "_:predicate hydra:variable \"p\";\n"
+ "    hydra:property rdf:predicate.\n"
+ "_:object hydra:variable \"o\";\n"
+ "    hydra:property rdf:object\n"
+ "    .\n"
+ "\n"
+ "<"+uri + filePath+"> \n"
+ "    a hydra:Collection, hydra:PagedCollection;\n"
+ "    dcterms:source :a;\n"
+ "    hydra:totalItems \""+numOfLines+"\"^^xsd:integer;\n"
+ "    void:triples \""+numOfLines+"\"^^xsd:integer;\n"
+ "    hydra:itemsPerPage \""+numOfLines+"\"^^xsd:integer\n"
+ "    .\n";
        return dataToWrite;
    }
    
    static class FilterSinkRDF extends StreamRDFBase
    {
        // Where to send the filtered triples.
        //private final Sink<Triple> dest ;
        private final PrintStream out;
        private NodeFormatter nodeFmt = new NodeFormatterNT() ;

        FilterSinkRDF(PrintStream out)
        {
            this.out = out ;
        }

        @Override
        public void triple(Triple triple)
        {
            Node s = triple.getSubject();
            Node p = triple.getPredicate();
            Node o = triple.getObject();

            String subject = s.toString();
            String predicate = p.toString();
            String object = o.toString();

            // print all combinations
            try {
                // only subject
                savePage("s" + transformPattern(subject) + "po", triple);
                // subject and predicate
                savePage("s" + transformPattern(subject) + "p" + transformPattern(predicate) + "o", triple);
                // subject and object
                savePage("s" + transformPattern(subject) + "po" + transformPattern(object), triple);
                // subject, predicate and object
                savePage("s" + transformPattern(subject) + "p" + transformPattern(predicate) + "o" + transformPattern(object), triple);

                // only predicate
                savePage("sp" + transformPattern(predicate) + "o", triple);
                // predicate and subject
                // predicate and object
                savePage("sp" + transformPattern(predicate) + "o" + transformPattern(object), triple);
                // predicate, subject and object

                // only object
                savePage("spo" + transformPattern(object), triple);
                // object and subject
                // object and predicate

                // all
                savePage("spo", triple);
            } catch(UnsupportedEncodingException e) {
            }
        }

        private void savePage(String filename, Triple triple) throws UnsupportedEncodingException {
            /*
            Node s = triple.getSubject();
            Node p = triple.getPredicate();
            Node o = triple.getObject();
            */

            // N-triples.
            try {
                File file = new File("ldf/" + filename + ".ttl");
                file.getParentFile().mkdirs();
                // append
                FileOutputStream fileOutStream = new FileOutputStream(file, true); 

                RDFDataMgr.writeTriples(fileOutStream, Collections.singleton(triple).iterator());

            } catch (IOException e) {
                // this may run if file is already open, retry!
                out.println(e);
            //    savePage(filename, triple);
            }


        }

        private String encode(String s) throws UnsupportedEncodingException {
            return URLEncoder.encode(s, "UTF-8");
        }
        private String transformPattern(String s) {
            // split the HASH into strings, 10 character each
            // it's .match(/.{1,10}/g) in JavaScript
            String hash = DigestUtils.shaHex(s);
            String path = StringUtils.join(hash.split("(?<=\\G.{10})"), "/");
            return path;
        }
        
        @Override
        public void finish()
        {
            // Output may be buffered.
            out.flush() ;
        }
    }
}
 
