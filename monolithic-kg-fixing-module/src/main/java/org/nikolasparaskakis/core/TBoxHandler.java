package org.nikolasparaskakis.core;



import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;



/**
 * Class for handling TBox axioms in an ontology.
 */
@SuppressWarnings({"unused", "DuplicatedCode", "rawtypes"})
public class TBoxHandler {

    /**
     * TBox axioms in the ontology.
     */
    public Set<OWLAxiom> tBox;

    /**
     * RBox axioms in the TBox.
     */
    public Set<OWLAxiom> rBox;

    /**
     * Declaration axioms in the TBox.
     */
    public Set<OWLAxiom> declarationsTBox;

    /**
     * Other TBox axioms that are not part of the main TBox, RBox, or declarations.
     */
    public Set<OWLAxiom> otherTBox;

    /**
     * Constructor that initializes the TBoxHandler by loading an ontology from the specified file path.
     * It extracts TBox axioms, RBox axioms, declaration axioms, and other axioms from the ontology.
     * @param tBoxFilePath The file path to the ontology file.
     */
    public TBoxHandler(String tBoxFilePath) {

        OWLOntologyManager man_t = createOWLOntologyManager();

        File tBoxFile = new File(tBoxFilePath);

        try {
            // Load the ontology from file.
            OWLOntology tBoxFromFile = man_t.loadOntologyFromOntologyDocument(tBoxFile);

            // Extract TBox axioms.
            this.tBox = tBoxFromFile.tboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());

            // Extract RBox axioms.
            this.rBox = tBoxFromFile.rboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());

            // Extract declaration axioms.
            this.declarationsTBox = getDeclarationAxioms(tBoxFromFile);

            // Compute the other axioms.
            this.otherTBox = getOtherAxioms(
                    tBoxFromFile.axioms().collect(Collectors.toSet()),
                    tBox, rBox, declarationsTBox
            );
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Error loading ontology from file: " + tBoxFilePath, e);
        }
    }

    /**
     * Extracts declaration axioms from the given ontology.
     * @param ontology The ontology from which to extract declaration axioms.
     * @return A set of declaration axioms.
     */
    public Set<OWLAxiom> getDeclarationAxioms(OWLOntology ontology) {
        return ontology.axioms()
                .filter(axiom -> axiom instanceof OWLDeclarationAxiom)
                .map(axiom -> (OWLDeclarationAxiom) axiom)
                .collect(Collectors.toSet());
    }

    /**
     * Identifies and returns axioms that are not part of the main TBox, RBox, or declaration axioms.
     * @param allAxioms The complete set of axioms in the ontology.
     * @param tBox The set of TBox axioms.
     * @param rBox The set of RBox axioms.
     * @param declarationsTBox The set of declaration axioms.
     * @return A set of other axioms not included in the main TBox, RBox, or declarations.
     */
    public Set<OWLAxiom> getOtherAxioms(Set<OWLAxiom> allAxioms, Set<OWLAxiom> tBox, Set<OWLAxiom> rBox, Set<OWLAxiom> declarationsTBox) {

        Set<OWLAxiom> otherTBox = new HashSet<>();

        HashSet<AxiomType> mainAxiomTypes = new HashSet<>();
        mainAxiomTypes.addAll(extractAxiomTypes(tBox));
        mainAxiomTypes.addAll(extractAxiomTypes(rBox));
        mainAxiomTypes.addAll(extractAxiomTypes(declarationsTBox));

        HashSet<AxiomType> otherAxiomTypes = new HashSet<>(extractAxiomTypes(allAxioms));
        otherAxiomTypes.removeAll(mainAxiomTypes);

        for (AxiomType<?> at : otherAxiomTypes) {
            Set<OWLAxiom> tmp = allAxioms.stream()
                    .filter(ax -> ax.getAxiomType().equals(at))
                    .collect(Collectors.toSet());
            if (!tmp.isEmpty()) {
                otherTBox.addAll(tmp);
            }
        }
        return otherTBox;
    }

    /**
     * Extracts the set of unique axiom types from a given set of OWLAxioms.
     * @param axioms The set of OWLAxioms from which to extract axiom types.
     * @return A set of unique AxiomType instances present in the input axioms.
     */
    public static Set<AxiomType> extractAxiomTypes(Set<OWLAxiom> axioms) {
        Set<AxiomType> axiomTypes = new HashSet<>();
        for (OWLAxiom axiom : axioms) {
            axiomTypes.add(axiom.getAxiomType());
        }
        return axiomTypes;
    }

    /**
     * Combines all TBox-related axioms (TBox, RBox, declarations, and other TBox axioms)
     * into a single set, excluding annotation assertions.
     * @return A set of all TBox-related axioms without annotation assertions.
     */
    public Set<OWLAxiom> getOntologyTBox() {
        Set<OWLAxiom> ontology = new HashSet<>();
        ontology.addAll(declarationsTBox);
        ontology.addAll(tBox);
        ontology.addAll(rBox);
        ontology.addAll(otherTBox);
        ontology.removeIf(ax -> ax.getAxiomType().equals(AxiomType.ANNOTATION_ASSERTION));
        return ontology;
    }
}