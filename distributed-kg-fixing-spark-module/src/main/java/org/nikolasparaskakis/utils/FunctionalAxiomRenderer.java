package org.nikolasparaskakis.utils;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;

import java.io.UncheckedIOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Renders a single OWLAxiom as OWL Functional Syntax string,
 * with correct escaping of quotes etc.
 */
public final class FunctionalAxiomRenderer {

    // One manager is fine; we isolate ontology creation per call.
    // Guard saving with a lock (OWLAPI Manager is threadsafe enough for read,
    // but we’re defensively serializing .saveOntology for Spark threads).
    private static final OWLOntologyManager MAN = OWLManager.createOWLOntologyManager();
    private static final ReentrantLock SAVE_LOCK = new ReentrantLock();

    private FunctionalAxiomRenderer() {}

    public static String render(OWLAxiom axiom) {
        try {
            // Create an ephemeral ontology containing only this axiom
            OWLOntology o = MAN.createOntology();
            MAN.addAxiom(o, axiom);

            StringDocumentTarget target = new StringDocumentTarget();

            // Save as Functional Syntax
            SAVE_LOCK.lock();
            try {
                MAN.saveOntology(o, new FunctionalSyntaxDocumentFormat(), target);
            } finally {
                SAVE_LOCK.unlock();
            }

            // Example output:
            // Ontology(
            //   DataPropertyAssertion( ... )
            // )
            String doc = target.toString();
            return extractBody(doc).trim();
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Failed to create ephemeral ontology for rendering", e);
        } catch (OWLOntologyStorageException e) {
            throw new UncheckedIOException(new java.io.IOException("Failed to save ontology in Functional Syntax", e));
        }
    }

    /**
     * Extracts everything inside Ontology( ... ) and removes trivial header/footer lines.
     * If multiple lines exist (e.g., pretty printer wraps), we keep them all.
     */
    private static String extractBody(String functionalDoc) {
        // Fast-path: strip "Ontology(" header and final ')'
        int start = functionalDoc.indexOf("Ontology(");
        if (start >= 0) {
            start += "Ontology(".length();
            int end = functionalDoc.lastIndexOf(')');
            if (end > start) {
                String inner = functionalDoc.substring(start, end);

                // Remove common non-axiom lines (empty, whitespace-only)
                String[] lines = inner.split("\\R");
                StringBuilder sb = new StringBuilder(inner.length());
                for (String line : lines) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    // If OWLAPI ever adds Prefix(...) or Declaration(...), you can keep them,
                    // but since we only add the axiom, we usually only get the axiom line(s).
                    // Keep all non-empty lines to be safe:
                    sb.append(t).append('\n');
                }
                return sb.toString().trim();
            }
        }
        // Fallback: return the whole doc if we couldn't parse structure for some reason
        return functionalDoc;
    }
}
