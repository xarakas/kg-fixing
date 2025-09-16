package org.nikolasparaskakis.utils;



import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;



/** * Utility class for sanitizing OWL ontologies by replacing unsupported
 * literal datatypes in DataPropertyAssertion axioms and unsupported
 * range datatypes in DataPropertyRange axioms.
 * <p>
 * Supported datatypes are those defined in OWL 2 (see {@link OWL2Datatype}).
 * Unsupported datatypes can be replaced using two strategies:
 * <ul>
 *     <li>TO_STRING: convert all unsupported literals to xsd:string</li>
 *     <li>NUMERIC_GUESS: try to convert literals that look numeric to xsd:decimal,
 *         otherwise convert to xsd:string</li>
 * </ul>
 * Additionally, a custom mapping from unsupported datatype IRIs to supported
 * OWL2Datatypes can be provided for DataPropertyRange axioms.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class DataAxiomSanitizer {

    /**
     * Sanitization strategies for literals.
     */
    public enum Strategy { TO_STRING, NUMERIC_GUESS }

    /**
     * Sanitize the given ontology in place, replacing unsupported literal datatypes
     * in DataPropertyAssertion axioms and unsupported range datatypes in
     * DataPropertyRange axioms.
     * Uses the TO_STRING strategy and no custom range mapping.
     * @param ontology the ontology to sanitize
     * @param strategy the sanitization strategy to use for literals
     *                 (TO_STRING or NUMERIC_GUESS)
     *                 TO_STRING: convert all unsupported literals to xsd:string
     *                 NUMERIC_GUESS: try to convert literals that look numeric to xsd:decimal,
     *                 otherwise convert to xsd:string
     */
    public static void sanitize(OWLOntology ontology, Strategy strategy) {
        sanitize(ontology, strategy, Collections.emptyMap());
    }

    /**
     * Sanitize the given ontology in place, replacing unsupported literal datatypes
     * in DataPropertyAssertion axioms and unsupported range datatypes in
     * DataPropertyRange axioms.
     * @param ontology the ontology to sanitize
     * @param strategy the sanitization strategy to use for literals
     *                 (TO_STRING or NUMERIC_GUESS)
     *                 TO_STRING: convert all unsupported literals to xsd:string
     *                 NUMERIC_GUESS: try to convert literals that look numeric to xsd:decimal,
     *                 otherwise convert to xsd:string
     * @param customRangeMap a custom mapping from unsupported datatype IRIs to
     *                       supported OWL2Datatypes for DataPropertyRange axioms
     *                       (e.g., map xsd:float to xsd:decimal)
     *                       If a datatype IRI is not in the map, it will be replaced
     *                       according to the strategy (TO_STRING or NUMERIC_GUESS).
     */
    public static void sanitize(OWLOntology ontology, Strategy strategy, Map<IRI, OWL2Datatype> customRangeMap) {
        OWLOntologyManager man = ontology.getOWLOntologyManager();
        OWLDataFactory df = man.getOWLDataFactory();

        // Allowed set = OWL 2 datatype map
        Set<IRI> allowed = Arrays.stream(OWL2Datatype.values())
                .map(OWL2Datatype::getIRI)
                .collect(Collectors.toSet());

        List<OWLOntologyChange> changes = new ArrayList<>();

        // --- 1) DataPropertyAssertion: rewrite unsupported literal datatypes ---
        ontology.axioms(AxiomType.DATA_PROPERTY_ASSERTION).forEach(ax -> {
            OWLLiteral lit = ax.getObject();
            if (!isAllowed(lit, allowed)) {
                OWLLiteral safe = toSafeLiteral(df, lit, strategy);
                changes.add(new RemoveAxiom(ontology, ax));
                changes.add(new AddAxiom(ontology,
                        df.getOWLDataPropertyAssertionAxiom(ax.getProperty(), ax.getSubject(), safe)));
            }
        });

        // --- 2) DataPropertyRange: rewrite unsupported range datatypes ---
        ontology.axioms(AxiomType.DATA_PROPERTY_RANGE).forEach(ax -> {
            OWLDataRange range = ax.getRange();

            // Guard with type check, then use asOWLDatatype()
            if (range.getDataRangeType() == DataRangeType.DATATYPE) {
                OWLDatatype dt = range.asOWLDatatype();  // safe now
                IRI dtIri = dt.getIRI();

                if (!allowed.contains(dtIri)) {
                    OWLDatatype target = customRangeMap.containsKey(dtIri)
                            ? df.getOWLDatatype(customRangeMap.get(dtIri).getIRI())
                            : df.getOWLDatatype(
                            (strategy == Strategy.TO_STRING
                                    ? OWL2Datatype.XSD_STRING
                                    : OWL2Datatype.XSD_DECIMAL).getIRI());

                    changes.add(new RemoveAxiom(ontology, ax));
                    changes.add(new AddAxiom(ontology,
                            df.getOWLDataPropertyRangeAxiom(ax.getProperty(), target)));
                }
            }
        });

        man.applyChanges(changes);
    }

    /**
     * Check if a literal is "allowed" (safe).
     * A literal is allowed if it has a language tag (→ rdf:langString),
     * or if its datatype IRI is in the allowed set.
     * @param lit the literal to check
     * @param allowed the set of allowed datatype IRIs
     * @return true if the literal is allowed, false otherwise
     */
    private static boolean isAllowed(OWLLiteral lit, Set<IRI> allowed) {
        if (lit.hasLang()) return true;
        return allowed.contains(lit.getDatatype().getIRI());
    }

    /**
     * Convert a literal to a "safe" literal according to the strategy.
     * If the literal has a language tag, it is returned as is (rdf:langString).
     * Otherwise, if the strategy is NUMERIC_GUESS and the literal looks numeric,
     * it is converted to xsd:decimal (if valid). Otherwise, it is converted to xsd:string.
     * @param df the OWLDataFactory
     * @param lit the original literal
     * @param strategy the sanitization strategy
     * @return a safe OWLLiteral
     */
    private static OWLLiteral toSafeLiteral(OWLDataFactory df, OWLLiteral lit, Strategy strategy) {
        String s = lit.getLiteral();
        if (lit.hasLang()) return lit; // already rdf:langString (safe)

        switch (strategy) {
            case NUMERIC_GUESS:
                if (looksNumeric(s)) {
                    try {
                        new BigDecimal(s); // validate lexical form
                        return df.getOWLLiteral(s, OWL2Datatype.XSD_DECIMAL);
                    } catch (Exception ignore) { /* fall through */ }
                }
                return df.getOWLLiteral(s, OWL2Datatype.XSD_STRING);
            case TO_STRING:
            default:
                return df.getOWLLiteral(s, OWL2Datatype.XSD_STRING);
        }
    }

    /**
     * A simple regex-based check to see if a string looks like a number.
     * It matches integers, decimals, and scientific notation.
     * @param s the string to check
     * @return true if the string looks like a number, false otherwise
     */
    private static boolean looksNumeric(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.matches("[+-]?([0-9]+(\\.[0-9]+)?|\\.[0-9]+)([eE][+-]?[0-9]+)?");
    }
}