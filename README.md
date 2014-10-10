ldstatic
========

To run simply do `sh build.sh foaf.rdf http://lmatteis.github.io/ldstatic/ldf/`. It will parse the file and create Turtle pages inside the `ldf/` directory.

# How to use

LDstatic requires Java 1.6.x or greater to run. All the other dependencies are contained within the downloaded package. Follow these steps to run LDstatic:

1. Download the source-code from: https://github.com/lmatteis/ldstatic
2. Run the bash script to generate the linked data. The first argument is the path to the RDF source file. The second argument is the namespace used
for the fragments metadata and controls:
   sh build.sh foaf.rdf http://lmatteis.github.io/ldstatic/ldf/
3. The ldf/ directory will now contain your static linked data resources that
can be published on GitHub Pages or other file hosting repositories.
