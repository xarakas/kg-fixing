package org.nikolasparaskakis.utils;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.stream.Collectors;

public final class OWLFunctionalSyntaxParser {

    private static final OWLOntologyManager MAN = OWLManager.createOWLOntologyManager();

    public OWLAxiom parse(String input) {
        try {
            if (input == null) throw new IllegalArgumentException("null input");
            String s = input.trim();
            if (s.isEmpty()) throw new IllegalArgumentException("empty input");

            // 1) Remove any COMMENT lines (not valid Functional Syntax)
            //    Some renderers (or pre-processing) may insert Manchester-like comments.
            s = stripHashComments(s);

            // 2) Ensure we feed a complete Functional document: Ontology( ... )
            if (!containsOntologyWrapper(s)) {
                s = "Ontology(\n" + s + "\n)";
            }

            // 3) Load as Functional Syntax
            StringDocumentSource src = new StringDocumentSource(s, IRI.create("http://example.org/temp"),
                    new FunctionalSyntaxDocumentFormat(), null);
            OWLOntology o = MAN.loadOntologyFromOntologyDocument(src);

            // 4) Prefer a *non-declaration* axiom if present (your payload often begins with Declarations)
            List<OWLAxiom> axioms = o.axioms().collect(Collectors.toList());
            if (axioms.isEmpty()) {
                throw new IllegalArgumentException("no axioms found after parsing");
            }

            for (OWLAxiom ax : axioms) {
                if (!(ax instanceof OWLDeclarationAxiom)) {
                    return ax; // return the first real axiom (e.g., DataPropertyAssertion)
                }
            }

            // Fallback: if only declarations exist, return the first declaration
            return axioms.get(0);

        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Failed to parse OWL Functional Syntax: " + preview(input), e);
        }
    }

    private static boolean containsOntologyWrapper(String s) {
        // very light check—Functional docs start with Ontology(
        int i = indexOfNonWhitespace(s, 0);
        return i >= 0 && s.regionMatches(true, i, "Ontology(", 0, "Ontology(".length());
    }

    private static int indexOfNonWhitespace(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private static String stripHashComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (String line : s.split("\\R")) {
            String t = line.trim();
            if (t.startsWith("#")) continue;           // drop comment lines
            if (t.startsWith("################")) continue; // drop banner comments
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String preview(String x) {
        if (x == null) return "null";
        String s = x.replace("\r", " ");
        if (s.length() > 500) s = s.substring(0, 500) + "…";
        return s;
    }
}
