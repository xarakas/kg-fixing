import org.semanticweb.HermiT.Configuration;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owl.explanation.api.NullExplanationProgressMonitor;
import org.semanticweb.owl.explanation.impl.blackbox.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.Set;
import java.util.function.Supplier;

import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.add;

import java.util.HashSet;



/*   This is a modified version of the following file:
 *
 * Copyright (C) 2009, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Information Management Group<br>
 * Date: 14-May-2009
 */
public class CustomConfInconsistentOntologyExplanationGeneratorFactory implements ExplanationGeneratorFactory<OWLAxiom> {


    private InconsistentOntologyContractionStrategy<OWLAxiom> contractionStrategy;

    private ExpansionStrategy<OWLAxiom> expansionStrategy;

    private CustomConfConsistencyEntailmentCheckerFactory consistencyEntailmentCheckerFactory;

    private Supplier<OWLOntologyManager> m;

    private Configuration conf;

    public CustomConfInconsistentOntologyExplanationGeneratorFactory(OWLReasonerFactory reasonerFactory, OWLDataFactory df, Supplier<OWLOntologyManager> m, Configuration conf) {
        expansionStrategy = new InconsistentOntologyExpansionStrategy<>();
        contractionStrategy = new InconsistentOntologyContractionStrategy<>();
        this.m = m;
        this.conf = conf;
        consistencyEntailmentCheckerFactory = new CustomConfConsistencyEntailmentCheckerFactory(reasonerFactory, m, df, conf);
    }


    @Override
    public ExplanationGenerator<OWLAxiom> createExplanationGenerator(OWLOntology ontology) {
        return createExplanationGenerator(ontology, new NullExplanationProgressMonitor<OWLAxiom>());
    }

    @Override
    public ExplanationGenerator<OWLAxiom> createExplanationGenerator(OWLOntology ontology, ExplanationProgressMonitor<OWLAxiom> progressMonitor) {
        Set<OWLAxiom> axioms = new HashSet<>(ontology.getLogicalAxiomCount(Imports.INCLUDED));
        ontology.importsClosure().forEach(ont -> add(axioms, ont.logicalAxioms()));
        return new BlackBoxExplanationGenerator2<>(
                axioms,
                consistencyEntailmentCheckerFactory,
                expansionStrategy,
                contractionStrategy,
                progressMonitor,
                m);
    }

    @Override
    public ExplanationGenerator<OWLAxiom> createExplanationGenerator(Set<? extends OWLAxiom> axioms) {
        return createExplanationGenerator(axioms, new NullExplanationProgressMonitor<OWLAxiom>());
    }

    @Override
    public ExplanationGenerator<OWLAxiom> createExplanationGenerator(Set<? extends OWLAxiom> axioms, ExplanationProgressMonitor<OWLAxiom> progressMonitor) {
        return new BlackBoxExplanationGenerator2<>(
                axioms,
                consistencyEntailmentCheckerFactory,
                expansionStrategy,
                contractionStrategy,
                progressMonitor,
                m);
    }
}
