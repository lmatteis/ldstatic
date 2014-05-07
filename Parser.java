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

import java.io.*;
import java.util.*;
import java.net.*;

public class Parser
{
    public static void main(String...argv)
    {
        String filename = "data.ttl" ;

        // This is the heart of N-triples printing ... outoput is heavily buffered
        // so the FilterSinkRDF called flush at the end of parsing.
        //Sink<Triple> output = new SinkTripleOutput(System.out, null, SyntaxLabels.createNodeToLabel()) ;
        StreamRDF filtered = new FilterSinkRDF(System.out) ;
        
        // Call the parsing process. 
        RDFDataMgr.parse(filtered, filename) ;

        // after all files are created add metadata and controls to each file 
        // (split into different files if necessary)
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
                savePage("s" + subject + "po", triple);
                // subject and predicate
                savePage("s" + subject + "p" + predicate + "o", triple);
                // subject and object
                savePage("s" + subject + "po" + object, triple);
                // subject, predicate and object
                savePage("s" + subject + "p" + predicate + "o" + object, triple);

                // only predicate
                savePage("sp" + predicate + "o", triple);
                // predicate and subject
                // predicate and object
                savePage("sp" + predicate + "o" + object, triple);
                // predicate, subject and object

                // only object
                savePage("spo" + object, triple);
                // object and subject
                // object and predicate
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
                // append
                FileOutputStream fileOutStream = new FileOutputStream("ldf/" + encode(filename), true); 

                RDFDataMgr.writeTriples(fileOutStream, Collections.singleton(triple).iterator());

            } catch (FileNotFoundException e) {
                out.println(e);
            }


        }

        private String encode(String s) throws UnsupportedEncodingException {
            return URLEncoder.encode(s, "UTF-8");
        }
        
        @Override
        public void finish()
        {
            // Output may be buffered.
            out.flush() ;
        }
    }
}
 
