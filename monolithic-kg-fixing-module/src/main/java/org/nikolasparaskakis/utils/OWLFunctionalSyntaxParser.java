package org.nikolasparaskakis.utils;



import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import java.util.Set;



/**
 * A utility class for parsing OWL axioms written in Functional Syntax into OWLAxiom objects.
 */
@SuppressWarnings({"unused", "DuplicatedCode", "HttpUrlsUsage"})
public class OWLFunctionalSyntaxParser {

    /**
     * Parses a string containing an OWL axiom in Functional Syntax and returns the corresponding OWLAxiom object.
     *
     * @param functionalAxiom The string representation of the OWL axiom in Functional Syntax.
     * @return The parsed OWLAxiom object.
     * @throws RuntimeException If there is an error during parsing.
     * @throws IllegalArgumentException If no axioms are found in the input string.
     */
    public OWLAxiom parse(String functionalAxiom) {
        functionalAxiom = functionalAxiom.replace("\\\"", "\"");
        // 1) Wrap in a minimal ontology
        String wrapped =
                "Ontology(<http://example.org/temp>\n"
                        + functionalAxiom
                        + "\n)";

        // 2) Create a document source with explicit Functional syntax format
        StringDocumentSource source = new StringDocumentSource(
                wrapped,
                IRI.create("http://example.org/temp"),
                new FunctionalSyntaxDocumentFormat(),
                null
        );

        // 3) Load it
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(source);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Failed to parse OWL Functional Syntax: "+ functionalAxiom + " ", e);
        }

        // 4) Extract the first axiom (there should be exactly one)
        Set<OWLAxiom> axioms = ontology.getAxioms();
        if (axioms.isEmpty()) {
            throw new IllegalArgumentException("No axioms found in input:\n" + functionalAxiom);
        }
        return axioms.iterator().next();
    }
}