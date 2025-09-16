package org.nikolasparaskakis.utils;



import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;



/**
 * A custom OWL reasoner factory that wraps another reasoner factory and uses a specific configuration
 * when creating reasoners.
 * This is useful for creating reasoners with custom configurations without having to specify the
 * configuration every time.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class CustomOwlReasonerFactory implements OWLReasonerFactory {

    /**
     * The base reasoner factory that will be used to create reasoners
     */
    OWLReasonerFactory baseReasonerFactory;

    /**
     * The configuration to be used when creating reasoners
     */
    OWLReasonerConfiguration reasonerConfiguration;

    /**
     * Constructor
     * @param baseReasonerFactory The base reasoner factory that will be used to create reasoners
     * @param reasonerConfiguration The configuration to be used when creating reasoners
     */
    public CustomOwlReasonerFactory(OWLReasonerFactory baseReasonerFactory, OWLReasonerConfiguration reasonerConfiguration) {
        this.baseReasonerFactory = baseReasonerFactory;
        this.reasonerConfiguration = reasonerConfiguration;
    }

    /**
     * Gets the name of the reasoner
     * @return The name of the reasoner
     */
    @Override
    public String getReasonerName() {
        return baseReasonerFactory.getReasonerName();
    }

    /**
     * Gets the reasoner version
     * @return The reasoner version
     */
    @Override
    public OWLReasoner createNonBufferingReasoner(@NotNull OWLOntology ontology) {
        return baseReasonerFactory.createNonBufferingReasoner(ontology, reasonerConfiguration);
    }

    /**
     * Creates a reasoner for the given ontology using the provided configuration
     * @param ontology The ontology to create the reasoner for
     * @return The created reasoner
     */
    @Override
    public OWLReasoner createReasoner(@NotNull OWLOntology ontology) {
        return baseReasonerFactory.createReasoner(ontology, reasonerConfiguration);
    }

    /**
     * Creates a non-buffering reasoner for the given ontology using the provided configuration
     * @param ontology The ontology to create the reasoner for
     * @param config The configuration to use when creating the reasoner
     * @return The created reasoner
     */
    @Override
    public OWLReasoner createNonBufferingReasoner(@NotNull OWLOntology ontology, @NotNull OWLReasonerConfiguration config) {
        return baseReasonerFactory.createNonBufferingReasoner(ontology, config);
    }

    /**
     * Creates a reasoner for the given ontology using the provided configuration
     * @param ontology The ontology to create the reasoner for
     * @param config The configuration to use when creating the reasoner
     * @return The created reasoner
     */
    @Override
    public OWLReasoner createReasoner(@NotNull OWLOntology ontology, @NotNull OWLReasonerConfiguration config) {
        return baseReasonerFactory.createReasoner(ontology, config);
    }
}