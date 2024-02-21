import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.datetime.DateTime;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class Fixer {

    static Logger logger = Logger.getLogger(ExtractModules.class.getName());

    private static int NamedNulls = 0;
    private static int Literals = 0;
    private static ReasonExplainArgs c_args = null;
    private boolean pRepairable = false; // Whether the current instance of my_ont is pRepairable
    private boolean pRepairabilitySkipp = false; // Whether skipp a pRepairability check by using the pRepairable value
    private ArrayList<OWLNamedIndividualImpl> usedIndividuals = null; // All the individuals(values) used so far in fixing
    private ArrayList<OWLLiteralImpl> usedLiterals = null; // All the Literals(values) used so far in fixing
    private OWLOntology my_ont = null;
    private PositionTracker pt = null;
    // reporting times in experiments
    private static long fixGenerationTime = 0;
    private static long fixFilteringTime = 0;
    private static long repairabilityCheckTime = 0;
    private static long inconsistencyCountingTime = 0;

    public static void clearTimes(){
        fixGenerationTime = 0;
        fixFilteringTime = 0;
        repairabilityCheckTime = 0;
        inconsistencyCountingTime = 0;
    }

    // AxiomTypes corresponding to (object or data) property assertions
    private ArrayList<AxiomType> PropertyAssertionAxiomTypes = new ArrayList<>(
            Arrays.asList(
                    AxiomType.OBJECT_PROPERTY_ASSERTION,
                    AxiomType.DATA_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION
            )
    );
    // AxiomTypes corresponding to data property assertions
    private ArrayList<AxiomType> DataPropertyAssertionAxiomTypes = new ArrayList<>(
            Arrays.asList(
                    AxiomType.DATA_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION
            )
    );

    public Fixer(Fixer f){
        // Create a copy of the ontology
        OWLOntologyManager man = createOWLOntologyManager();
        try {
            this.my_ont = man.copyOntology(f.my_ont, OntologyCopy.DEEP);
            this.pt = new PositionTracker(f.pt);
            this.c_args = f.c_args;
            this.pRepairable = f.pRepairable;
            this.pRepairabilitySkipp = f.pRepairabilitySkipp;
            this.usedIndividuals = new ArrayList<OWLNamedIndividualImpl>();
            this.usedLiterals = new ArrayList<OWLLiteralImpl>();
            this.usedIndividuals.addAll(f.usedIndividuals);
            this.usedLiterals.addAll(f.usedLiterals);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    public Fixer(OWLOntology my_ont, PositionTracker pt, ReasonExplainArgs c_args){
        this.my_ont = my_ont;
        this.pt = pt;
        this.c_args = c_args;
        this.usedIndividuals = new ArrayList<OWLNamedIndividualImpl>();
        this.usedLiterals = new ArrayList<OWLLiteralImpl>();
    }

    public static void printFixes(HashSet<Fix> fixes){
        HashSet<OWLAxiom> oldAxioms = new HashSet<OWLAxiom>();
        for(Fix f : fixes){
            if(!f.isAdd()){
                oldAxioms.add(f.getOldAxiom());
            }
        }
//        logger.info("******* Number of fixes: " + fixes.size() +
//                        "\n     * Number of old axioms: " + oldAxioms.size() +
//                "\n     * Old axioms: " + oldAxioms
//        );
//        for(OWLAxiom newAxiom : fixes.keySet()){
//            logger.info("      ** Old axiom: " + fixes.get(newAxiom) +
//                    "\n       * New axiom: " + newAxiom
//            );
//        }
    }

    public static long getFixGenerationTime() {
//        if(c_args.fixSelection==2){
//            // When choosing first fix, filtering takes place during generation
//            return fixGenerationTime - fixFilteringTime;
//        }
            return fixGenerationTime;
    }

    public static void setFixGenerationTime(long fixGenerationTime) {
        Fixer.fixGenerationTime = fixGenerationTime;
    }

    public static long getFixFilteringTime() {
        return fixFilteringTime;
    }

    public static void setFixFilteringTime(long fixFilteringTime) {
        Fixer.fixFilteringTime = fixFilteringTime;
    }

    public static long getRepairabilityCheckTime() {
        return repairabilityCheckTime;
    }

    public static void setRepairabilityCheckTime(long repairabilityCheckTime) {
        Fixer.repairabilityCheckTime = repairabilityCheckTime;
    }

    public static long getInconsistencyCountingTime() {
        return inconsistencyCountingTime;
    }

    public static void setInconsistencyCountingTime(long inconsistencyCountingTime) {
        Fixer.inconsistencyCountingTime = inconsistencyCountingTime;
    }

    /**
     * Check each fix for soundness (i.e. whether repairability is retained after fix application)
     * Used in Rank-fix or Random-fix (default)
     * @param fixes     A set of fixes to be filtered for soundness
     * @return          A set of sound fixes
     */
    public HashSet<Fix> KeepSoundFixes(HashSet<Fix> fixes){
        long startTime = System.nanoTime();
        HashSet<Fix> soundFixes = new HashSet<Fix>();
        int bestScore = Integer.MAX_VALUE;
        int soundFixesChecked = 0;
        boolean stop = false;
        Iterator<Fix> it = fixes.iterator();
        while(!stop & it.hasNext()){
            Fix f = it.next();
//            logger.info("      - Bestscore : " + bestScore);
            // Create copy of ontology and PositionTracker to check repairability
            Fixer tmpFixer = new Fixer(this);
            // Apply the fix
            tmpFixer.applyFix(f);
            if(tmpFixer.CheckRepairability()){
                soundFixesChecked++;
                if(c_args.fixSelection==3 & !f.getTrivial()){// trivial fixes are (almost?) always "the best", ignore them in rank-fix!
                    int currentScore = tmpFixer.countInconsistencies();
//                    logger.info("      - Bestscore : " + bestScore);
//                    logger.info("      - currcore : " + currentScore);
                    if(currentScore < bestScore){
                        bestScore = currentScore;
                        soundFixes.clear();
                        soundFixes.add(f);
//                        logger.info("      - NewBestFix : " + f);
                        if(bestScore == 0){
                            // Found an ideal fix!
                            stop = true;
                        } else if(soundFixesChecked >= 50){
                            // enough searching
                            stop = true;
                        }
                    } else if (currentScore == bestScore){
                        soundFixes.add(f);
                    }
                } else {
                    // this is a ''good'' fix
                    soundFixes.add(f);
                }
            } else {
//                logger.info("      - Non-sound fix: " + f);
            }
            long timeSoFar = (System.nanoTime() - startTime) / 1000000;
            if(timeSoFar >= c_args.fixingTimeout){
                stop = true; // due to overtime!
            }
        }
        long endTime = System.nanoTime();
        setFixFilteringTime(getFixFilteringTime() + ((endTime - startTime) / 1000000));
        return soundFixes;
    }

    /**
     * Compute only some fixes skipping the timely part of computation/generation of all potential fixes!
     * Used in trivial-fix and greedy-fix
     * @param explanation
     * @return
     */
    public HashSet<Fix> ComputeSomeSoundFixes(Set<Set<OWLAxiom>> explanation){
        long startTime = System.nanoTime();
        HashSet<Fix> fixes = new HashSet<>(); // Set of alternative fixes for this inconsistency
        OWLNamedIndividual newInd = getFreshNullIndividual(my_ont);
        /* Get all individuals in the original ontology */
        List<OWLNamedIndividual> individuals = my_ont.individualsInSignature(Imports.INCLUDED).collect(Collectors.toList());
        // A: Find all joint positions in all axioms in each explanation (plus Object positions in data property assertions, as they can be used despite not joint)
        // Also calculate the number of explanations each position is contained in (for maximality ranking)
        HashMap<AxiomPosition,Integer> positionsContained = getPositionMaximality(explanation);
        // B: Select for which joint positions to generate fixes
        // (e.g. the Maximally contained across explanations?)
        HashSet<AxiomPosition> apChosen = choosePositions(positionsContained);
        // C: Generate fixes for the selected position(s)
        // Get the iterator
        Iterator<AxiomPosition> itApMCD = apChosen.iterator();
        // soundFixFound is a flag indicating whether we found what we are looking for: i.e. a sound fix!
        // This flag is only used when greedy-fix is used
        boolean soundFixFound = false;
        while(!soundFixFound && itApMCD.hasNext()){
            AxiomPosition apMCD = itApMCD.next();
            OWLAxiom ax = apMCD.getAxiom();
            Position p = apMCD.getPosition();
            AxiomType at = ax.getAxiomType();

            // The chosen position is mutable and joint: It has already been checked for these two conditions
            if (at.equals(AxiomType.CLASS_ASSERTION)) {
                // Mutable CLASS_ASSERTION axioms have a single mutable position
                // Get the single individual in this axiom
                Set<OWLNamedIndividual> inds = ax.getIndividualsInSignature();
                // This is a joint mutable position, Generate fixes
                // Get the single "class" of the CLASS_ASSERTION which we do not change
                Set<OWLClass> cs = ax.getClassesInSignature();
                for (OWLNamedIndividual ind : inds) { //This for should only run for one repeat (the single individual)!
                    for (OWLClass c : cs) { //This for should only run for one repeat (the single class)!
                        // Add fixes by replacing the individual with "named null"
                        OWLAxiom altAxiom = new OWLClassAssertionAxiomImpl(newInd, c, ax.getAnnotations());
                        fixes.add(new Fix(ax, altAxiom, p)); // p should be Position.SINGLE
                        if(c_args.fixSelection!=1){
                        // Add fixes by replacing the individual with other individuals of the KG
                            Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                            while(!soundFixFound && itIndividuals.hasNext()){
                                OWLNamedIndividual indtotal = itIndividuals.next();
                                if (!indtotal.equals(ind)) {
                                    altAxiom = new OWLClassAssertionAxiomImpl(indtotal, c, ax.getAnnotations());
                                    HashSet<Fix> currentFixes = new HashSet<>();
                                    currentFixes.add(new Fix(ax, altAxiom, p));
                                    if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                        //check soundness
                                        currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                        // update flag
                                        if(!currentFixes.isEmpty()){
                                            // The first non-trivial sound fix has just been found.
                                            // No need for further search of fixes
                                            soundFixFound = true;
                                            // We want to prioritize the non-trivial fixes (if any)
                                            // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                            fixes.clear();
                                        }
                                    }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                    fixes.addAll(currentFixes); // p should be Position.SINGLE
                                }
                            }
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                OWLObjectPropertyAssertionAxiomImpl oldAx = (OWLObjectPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.SUBJECT)) { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLObjectPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldSubject)) {
                                altAxiom = new OWLObjectPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                } else { //the mutable position is Position.OBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the OBJECT individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldObject)) {
                                altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indtotal, oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)) {
                OWLNegativeObjectPropertyAssertionAxiomImpl oldAx = (OWLNegativeObjectPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.SUBJECT)) { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldSubject)) {
                                altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                } else { //the mutable position is Position.OBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the OBJECT individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldObject)) {
                                altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indtotal, oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.DATA_PROPERTY_ASSERTION)) {
                OWLDataPropertyAssertionAxiomImpl oldAx = (OWLDataPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.OBJECT)) { //the mutable position is Position.OBJECT
                    OWLLiteral oldObject = oldAx.getObject();
                    // For literals, that include a type encoded in them, non joint positions should also be considered for fixing the type
                    // Check the Tbox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.keySet().contains(oldAx.getProperty())) {
                        // A DataPropertyRange Axiom does exist for this property
                        OWLDatatype newDatatype = dataPropertyToRangeType.get(oldAx.getProperty());
                        OWLDatatype oldDatatype = oldAx.getObject().getDatatype();
                        if (!newDatatype.equals(oldDatatype)) {
                            String lit = oldObject.getLiteral();
                            String lang = oldObject.getLang();
                            //A type mismatch does exist, propose a fix by casting the literal in the expected type.
                            OWLAxiom altAxiom = new OWLDataPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), this.getFreshLiteralImpl(lit, lang, newDatatype), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                            // TODO: Add fixes by a random Literal
                            // TODO: Add fixes by replacing the Literal with other Literals of the KG
                        } // Else, no type mismatch exists. Nothing to fix.
                    } // Else, no DataPropertyRange Axiom is available. Nothing to fix.
                    //                            if(oldObject.getDatatype().equals()
                } else { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLDataPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldSubject)) {
                                altAxiom = new OWLDataPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION)) {
                OWLNegativeDataPropertyAssertionAxiomImpl oldAx = (OWLNegativeDataPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.OBJECT)) { //the mutable position is Position.OBJECT
                    OWLLiteral oldObject = oldAx.getObject();
                    // For literals, that include a type encoded in them, non joint positions should also be considered for fixing the type
                    // Check the Tbox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.keySet().contains(oldAx.getProperty())) {
                        // A DataPropertyRange Axiom does exist for this property
                        OWLDatatype newDatatype = dataPropertyToRangeType.get(oldAx.getProperty());
                        OWLDatatype oldDatatype = oldAx.getObject().getDatatype();
                        if (!newDatatype.equals(oldDatatype)) {
                            String lit = oldObject.getLiteral();
                            String lang = oldObject.getLang();
                            //A type mismatch does exist, propose a fix by casting the literal in the expected type.
                            OWLAxiom altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), this.getFreshLiteralImpl(lit, lang, newDatatype), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                            // TODO: Add fixes by a random Literal
                            // TODO: Add fixes by replacing the Literal with other Literals of the KG
                        } // Else, no type mismatch exists. Nothing to fix.
                    } // Else, no DataPropertyRange Axiom is available. Nothing to fix.
                    //                            if(oldObject.getDatatype().equals()
                } else { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    if(c_args.fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indtotal = itIndividuals.next();
                            if (!indtotal.equals(oldSubject)) {
                                altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(c_args.fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes.addAll(this.KeepSoundFixes(currentFixes));
                                    // update flag
                                    if(!currentFixes.isEmpty()){
                                        // The first non-trivial sound fix has just been found.
                                        // No need for further search of fixes
                                        soundFixFound = true;
                                        // We want to prioritize the non-trivial fixes (if any)
                                        // Hence, now that we found the first sound non-trivial fix, we should ignore the trivial one.
                                        fixes.clear();
                                    }
                                }// Add the fix available in currentFixes in the final fixes list for checking soundness later
                                fixes.addAll(currentFixes);
                            }
                        }
                    }
                }
            }
            // TODO: cover other types
            else if ( at.equals(AxiomType.SAME_INDIVIDUAL) || at.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                logger.info("\n   **** Axiom: " + ax.toString() +
                        "\n    *** Axiom type not supported yet: " + ax.getAxiomType());
            } else {
                logger.info("\n   **** Axiom: " + ax.toString() +
                        "\n    *** Axiom type not supported: " + ax.getAxiomType());
            }
        }
        long endTime = System.nanoTime();
        setFixGenerationTime(getFixGenerationTime() + ((endTime - startTime) / 1000000));
        return fixes;
    }

    /**
     * Find all joint positions in all axioms in each explanation (plus Object positions in data property assertions, as they can be used despite not joint)
     *  And calculate the number of explanations each position is contained in (for maximality ranking)
     * @param explanation A set of explanations (set of axioms) causing inconsistency
     * @return  A map X -> Y, mapping each joint AxiomPosition X to the number of explanations it is contained in Y
     */
    public HashMap<AxiomPosition,Integer> getPositionMaximality( Set<Set<OWLAxiom>> explanation){
        OWLNamedIndividual newInd = getFreshNullIndividual(my_ont);
        //Rank OWLAxioms to identify the Maximally contained in different Explanations
        HashMap<AxiomPosition,Integer> positionsContained = new  HashMap<>();
        for (Set<OWLAxiom> expAxioms : explanation) {
            // A: expAxioms gathers all axioms of this explanation
            HashSet<OWLAxiom> expABoxAxioms = new HashSet<OWLAxiom>();
            HashSet<OWLNamedIndividual> allInds = new HashSet<OWLNamedIndividual>();
            HashMap<AxiomPosition, OWLNamedIndividual> allPositions = new HashMap<>();
            // B: Find joint positions in all axioms of this explanation
            HashSet<OWLNamedIndividual> jointPositionInds = new HashSet<OWLNamedIndividual>();
            for(OWLAxiom ax: expAxioms){ // For each axiom
                AxiomType axType = ax.getAxiomType();
                if(AxiomType.ABoxAxiomTypes.contains(axType)){
                    //This is an Abox Axiom
                    expABoxAxioms.add(ax);

                    ArrayList<OWLNamedIndividual> ax_in = new ArrayList<OWLNamedIndividual>();

                    // Keep all possible positions
                    if(PropertyAssertionAxiomTypes.contains(axType)){ // the axiom has subject and object
                        OWLPropertyAssertionAxiom pax = (OWLPropertyAssertionAxiom) ax;
                        OWLNamedIndividual sub = (OWLNamedIndividual) pax.getSubject();
                        AxiomPosition subPos = new AxiomPosition(ax,Position.SUBJECT);
                        allPositions.put(subPos, sub);
                        ax_in.add(sub);
                        AxiomPosition obPos = new AxiomPosition(ax, Position.OBJECT);
                        OWLNamedIndividual obj = newInd; // For DataPropertyAssertionAxiomTypes the newInd is not actually used!
                        if(!DataPropertyAssertionAxiomTypes.contains(axType)) {
                            // This is not a DataPropertyAssertionAxiom, the object has an individual as well.
                            obj = (OWLNamedIndividual) pax.getObject();
                            ax_in.add(obj);
                        } // else: this is a DataPropertyAssertionAxiom
                        // TODO: This DataPropertyAssertionAxiom is included in an inconsistency explanation
                        // This explanation may also include some DataPropertyRange or other parts of the Tbox that are relevant
                        // We could gather such information from the explanation at this point to be used later for fixing.
                        allPositions.put(obPos, obj);
                    } else { // the axiom has a single position
                        ax_in.addAll(ax.getIndividualsInSignature());
                        AxiomPosition sinPos = new AxiomPosition(ax,Position.SINGLE);
                        allPositions.put(sinPos, ax.getIndividualsInSignature().iterator().next());
                    }
                    // Update individuals of joint positions
                    for (OWLNamedIndividual ind : ax_in) { // For each individual in this Abox axiom
                        if (allInds.contains(ind)) {
                            // This has already been found in this explanation, hence it is a joint position
                            jointPositionInds.add(ind);
                        } else {
                            // This is found for a first time in this explanation, not a joint position
                            allInds.add(ind);
                        }
                    }
                } // No Abox axioms, hence no need to process them for individuals
            } // End "for each axiom"

            // Update the ranking for maximally contained joint positions
            for(AxiomPosition axiomPosition: allPositions.keySet()){ // for each position in this explanation
                if(!pt.isImmutable(axiomPosition)) {
                    // Consider a position only if mutable
                    OWLNamedIndividual axPosInd = allPositions.get(axiomPosition);
                    if (jointPositionInds.contains(axPosInd)) {//This is a joint position
                        // Object positions for axioms of DataPropertyAssertionAxiomTypes never fall in this case because newInd is not considered for joint positions
                        if (positionsContained.containsKey(axiomPosition)) {
                            // This joint position already been found in other explanation, hence increase the number
                            positionsContained.put(axiomPosition, positionsContained.get(axiomPosition) + 1);
                        } else {
                            // This is found for a first time
                            positionsContained.put(axiomPosition, 1);
                        }
                    } else {// This is a non-joint position.
                        // In the original work non-joint positions are not considered for fixing but we need it for covering DATA restrictions
                        // Object positions for axioms of DataPropertyAssertionAxiomTypes always fall in this case because newInd is not considered for joint positions
                        if(DataPropertyAssertionAxiomTypes.contains(axiomPosition.getAxiom().getAxiomType())){
                            //Add the DATA position (i.e. Position.OBJECT) in the positions to consider for fixing
                            if(axiomPosition.getPosition().equals(Position.OBJECT)){
                                if (positionsContained.containsKey(axiomPosition)) {
                                    // This joint position already been found in other explanation, hence increase the number
                                    positionsContained.put(axiomPosition, positionsContained.get(axiomPosition) + 1);
                                } else {
                                    // This is found for a first time
                                    positionsContained.put(axiomPosition, 1);
                                }
                            } // Else, it is a non-joint SUBJECT position (individual) of a DataPropertyAssertionAxiomType Axiom.
                        }
                    }
                } // Else, this is an immutable position, no fixes considered.
            }
        } // End "for each explanation"

        return positionsContained;
    }

    /**
     * Select for which joint positions to generate fixes
     * @param positionsContained A map X -> Y, mapping each joint AxiomPosition X to the number of explanations it is contained in Y
     * @return  A HashSet of AxiomPositions to be considered for fixing.
     */
    public HashSet<AxiomPosition> choosePositions( HashMap<AxiomPosition,Integer> positionsContained){
        HashSet<AxiomPosition> apChosen = new HashSet<>(); // All the selected positions to be considered for fix generation.
        // C: Generate fixes for the selected positions
        if(c_args.mcd) { // Generate fixes based on the maximally contained joint position(s) in Abox axioms
            // Find the maximally contained joint position(s)
            int maxContained = 0;
            for (AxiomPosition ap : positionsContained.keySet()) {
                if (positionsContained.get(ap) > maxContained) {
                    // ap is the new current MCD, clear all previous MCDs
                    maxContained = positionsContained.get(ap);
                    apChosen.clear();
                    apChosen.add(ap); // apChosen holds the MCD.
                } else if (positionsContained.get(ap) == maxContained) {
                    // ap is an additional MCD (tie), add in MCDs
                    apChosen.add(ap); // In case of tie more than one MCD elements will be stored here.
                } // else, this is a non-MCD position
            }
            //tmp code for printing debug info
//            System.out.println("MCD positions: " + apChosen);
        } else { // Generate fixes based on all joint position(s) in Abox axioms
            apChosen.addAll(positionsContained.keySet());
        }
        return apChosen;
    }

    /**
     * Calculate all potential fixes (newAxiom - oldAxiom) for the (Abox) axioms involved in the explanations provided
     * Used in rank-fix and random-fix (default)
     * @param explanation    A set of "alternative" explanations
     */
    public HashSet<Fix> ComputeFixes(Set<Set<OWLAxiom>> explanation){
        long startTime = System.nanoTime();
        HashSet<Fix> fixes = new HashSet<>(); // Set of alternative fixes for this inconsistency
        OWLNamedIndividual newInd = getFreshNullIndividual(my_ont);
        /* Get all individuals in the original ontology */
        List<OWLNamedIndividual> individuals = my_ont.individualsInSignature(Imports.INCLUDED).collect(Collectors.toList());
        // A: Find all joint positions in all axioms in each explanation (plus Object positions in data property assertions, as they can be used despite not joint)
        // Also calculate the number of explanations each position is contained in (for maximality ranking)
        HashMap<AxiomPosition,Integer> positionsContained = getPositionMaximality(explanation);
        // B: Select for which joint positions to generate fixes
        // (e.g. the Maximally contained across explanations?)
        HashSet<AxiomPosition> apChosen = choosePositions(positionsContained);
        // C: Generate fixes for the selected position(s)
        for(AxiomPosition apMCD : apChosen) {
            OWLAxiom ax = apMCD.getAxiom();
            Position p = apMCD.getPosition();
            AxiomType at = ax.getAxiomType();

            // The chosen position is mutable and joint: It has already been checked for these two conditions
            if (at.equals(AxiomType.CLASS_ASSERTION)) {
                // Mutable CLASS_ASSERTION axioms have a single mutable position
                // Get the single individual in this axiom
                Set<OWLNamedIndividual> inds = ax.getIndividualsInSignature();
                // This is a joint mutable position, Generate fixes
                // Get the single "class" of the CLASS_ASSERTION which we do not change
                Set<OWLClass> cs = ax.getClassesInSignature();
                for (OWLNamedIndividual ind : inds) { //This for should only run for one repeat (the single individual)!
                    for (OWLClass c : cs) { //This for should only run for one repeat (the single class)!
                        // Add fixes by replacing the individual with "named null"
                        OWLAxiom altAxiom = new OWLClassAssertionAxiomImpl(newInd, c, ax.getAnnotations());
                        fixes.add(new Fix(ax, altAxiom, p)); // p should be Position.SINGLE
                        // Add fixes by replacing the individual with other individuals of the KG
                        for (OWLNamedIndividual indtotal : individuals) {
                            if (!indtotal.equals(ind)) {
                                altAxiom = new OWLClassAssertionAxiomImpl(indtotal, c, ax.getAnnotations());
                                fixes.add(new Fix(ax, altAxiom, p)); // p should be Position.SINGLE
                            }
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                OWLObjectPropertyAssertionAxiomImpl oldAx = (OWLObjectPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.OBJECT)) { //the mutable position is Position.OBJECT

                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLObjectPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    // Add fixes by replacing the individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldSubject)) {
                            altAxiom = new OWLObjectPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                } else { //the mutable position is Position.SUBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldObject)) {
                            altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indtotal, oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)) {
                OWLNegativeObjectPropertyAssertionAxiomImpl oldAx = (OWLNegativeObjectPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.SUBJECT)) { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    // Add fixes by replacing the individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldSubject)) {
                            altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                } else { //the mutable position is Position.OBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldObject)) {
                            altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indtotal, oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.DATA_PROPERTY_ASSERTION)) {
                OWLDataPropertyAssertionAxiomImpl oldAx = (OWLDataPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.OBJECT)) { //the mutable position is Position.OBJECT
                    OWLLiteral oldObject = oldAx.getObject();
                    // For literals, that include a type encoded in them, non joint positions should also be considered for fixing the type
                    // Check the Tbox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.keySet().contains(oldAx.getProperty())) {
                        // A DataPropertyRange Axiom does exist for this property
                        OWLDatatype newDatatype = dataPropertyToRangeType.get(oldAx.getProperty());
                        OWLDatatype oldDatatype = oldAx.getObject().getDatatype();
                        if (!newDatatype.equals(oldDatatype)) {
                            String lit = oldObject.getLiteral();
                            String lang = oldObject.getLang();
                            //A type mismatch does exist, propose a fix by casting the literal in the expected type.
                            OWLAxiom altAxiom = new OWLDataPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), this.getFreshLiteralImpl(lit, lang, newDatatype), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                            // TODO: Add fixes by a random Literal
                            // TODO: Add fixes by replacing the Literal with other Literals of the KG
                        } // Else, no type mismatch exists. Nothing to fix.
                    } // Else, no DataPropertyRange Axiom is available. Nothing to fix.
                    //                            if(oldObject.getDatatype().equals()
                } else { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLDataPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    // Add fixes by replacing the individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldSubject)) {
                            altAxiom = new OWLDataPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                }
            }
            else if (at.equals(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION)) {
                OWLNegativeDataPropertyAssertionAxiomImpl oldAx = (OWLNegativeDataPropertyAssertionAxiomImpl) ax;
                // For mutable OBJECT_PROPERTY_ASSERTION axioms we have to find mutable position(s) first
                if (p.equals(Position.OBJECT)) { //the mutable position is Position.OBJECT
                    OWLLiteral oldObject = oldAx.getObject();
                    // For literals, that include a type encoded in them, non joint positions should also be considered for fixing the type
                    // Check the Tbox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.keySet().contains(oldAx.getProperty())) {
                        // A DataPropertyRange Axiom does exist for this property
                        OWLDatatype newDatatype = dataPropertyToRangeType.get(oldAx.getProperty());
                        OWLDatatype oldDatatype = oldAx.getObject().getDatatype();
                        if (!newDatatype.equals(oldDatatype)) {
                            String lit = oldObject.getLiteral();
                            String lang = oldObject.getLang();
                            //A type mismatch does exist, propose a fix by casting the literal in the expected type.
                            OWLAxiom altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), this.getFreshLiteralImpl(lit, lang, newDatatype), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                            // TODO: Add fixes by a random Literal
                            // TODO: Add fixes by replacing the Literal with other Literals of the KG
                        } // Else, no type mismatch exists. Nothing to fix.
                    } // Else, no DataPropertyRange Axiom is available. Nothing to fix.
                    //                            if(oldObject.getDatatype().equals()
                } else { //the mutable position is Position.SUBJECT
                    OWLIndividual oldSubject = oldAx.getSubject();
                    // This is a joint mutable position, Generate fixes
                    OWLAxiom altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(newInd, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                    // Add fixes by replacing the individual with other individuals of the KG
                    for (OWLNamedIndividual indtotal : individuals) {
                        if (!indtotal.equals(oldSubject)) {
                            altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(indtotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                }
            }
            // TODO: cover other types
            else if ( at.equals(AxiomType.SAME_INDIVIDUAL) || at.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                logger.info("\n   **** Axiom: " + ax.toString() +
                        "\n    *** Axiom type not supported yet: " + ax.getAxiomType());
            } else {
                logger.info("\n   **** Axiom: " + ax.toString() +
                        "\n    *** Axiom type not supported: " + ax.getAxiomType());
            }
        }
        long endTime = System.nanoTime();
        setFixGenerationTime(getFixGenerationTime() + (endTime - startTime) / 1000000);
        return fixes;
    }

    /**
     * Creates a "Fresh-Null" representing a new unknown individual
     * @param my_ont
     * @return
     */
    public OWLNamedIndividual getFreshNullIndividual(OWLOntology my_ont){
        OWLNamedIndividual newInd = null;
        if(my_ont.isAnonymous()){
            newInd = new OWLNamedIndividualImpl(IRI.create("NamedIndividual" + NamedNulls));
        } else {
            newInd = new OWLNamedIndividualImpl(IRI.create(my_ont.getOntologyID().getOntologyIRI().get().getNamespace(), "NamedIndividual" + NamedNulls));
        }
        NamedNulls++;
        return newInd;
    }

    /**
     * Return a fresh literal with specific literal value, language and datatype
     * @param lit
     * @param lang
     * @param newDatatype
     * @return
     */
    private OWLLiteralImpl getFreshLiteralImpl(String lit, String lang, OWLDatatype newDatatype) {
        //TODO: Check that the generated literal is not "malformed" (e.g. "Q2342"<Integer>) leading to an exception
        OWLLiteralImpl newInd = new OWLLiteralImpl(lit, lang, newDatatype);

        return newInd;
    }

    /**
     * Check whether the ontology provided is consistent
     * @return true if consistent, false otherwise
     */
    public boolean CheckConsistency() throws OWLOntologyCreationException {
        boolean result = true;
        OntBreakdown moduleBr = new OntBreakdown(my_ont, this.c_args.debugLog);
        result = moduleBr.isConsistent();
        if (!result){
            // Some inconsistency should be found
            Set<Set<OWLAxiom>> j = moduleBr.getExplanations(c_args.timeout, c_args.explanationsLimit);
//            logger.info(" Inconsistency found: |"+ j.toString() +"|");
            result = false;
        }
        return result;
    }

    public int countInconsistencies(){
        long startTime = System.nanoTime();
        int result = 0;
        OntBreakdown moduleBr = null;
        try {
            moduleBr = new OntBreakdown(my_ont, this.c_args.debugLog);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        if (!moduleBr.isConsistent()){
            // Some inconsistency should be found
            Set<Set<OWLAxiom>> j = moduleBr.getExplanations(c_args.timeout, c_args.explanationsLimit);
            result = j.size();
//            logger.info(" Inconsistency size: |"+ result +"|");
//            logger.info(" Inconsistency found: |"+ j.toString() +"|");
        }
        long endTime = System.nanoTime();
        setInconsistencyCountingTime(getInconsistencyCountingTime() + (endTime - startTime) / 1000000);
        return result;
    }

    /**
     * Get all DataPropertyRange Axioms for the given ontology my_ont
     * @return  a HashMap<OWLDataPropertyExpression,OWLDatatype> from axiom to respective OWLDatatype
     */
    public HashMap<OWLDataPropertyExpression,OWLDatatype> getDataPropertyRanges(){
        // Get all Data Property Range Axioms from Tbox
        Set<OWLDataPropertyRangeAxiom> dataPropertyRanges = my_ont.getAxioms(AxiomType.DATA_PROPERTY_RANGE,Imports.INCLUDED);
        // In OWLDataPropertyRangeAxioms, a Data Property Expression can have only a single Data Range: "A data property range axiom DataPropertyRange( DPE DR ) states that the range of the data property expression DPE is the data range DR — that is, if some individual is connected by DPE with a literal x, then x is in DR. The arity of DR must be one." https://www.w3.org/TR/owl2-syntax/#Data_Property_Range
        HashMap<OWLDataPropertyExpression,OWLDatatype> dataPropertyToRangeType = new HashMap<>();
        for(OWLDataPropertyRangeAxiom t: dataPropertyRanges){
            dataPropertyToRangeType.put(t.getProperty(), t.getRange().asOWLDatatype());
        }
        return dataPropertyToRangeType;
    }

    /**
     * Checks whether the ontology provided is repairable, conditioned to any immutable positions
     *      This is checked by replacing all individuals with "fresh null individuals" and checking the consistency.
     *      For DataPropertyAssertionAxiomTypes it also replaces literals with "a fresh random literal" of the expected type (taken from a DATA_PROPERTY_RANGE axiom).
     * @return
     */
    public boolean CheckRepairability(){
        long startTime = System.nanoTime();
        boolean result = false;
        // TODO: Add an argument for optimized pRepairability check
        if(this.pRepairabilitySkipp){
            // Skip CheckRepairability in certain cases: i.e. when the repair value is not "not in" "seen repair values"
            // This is a case where the CheckRepairability can be skipped
            result = this.pRepairable;
        } else { // Normal calculation of pRepairability
            Fixer tmpFixer = new Fixer(this);
            OWLOntology newOntology = tmpFixer.my_ont;
            // Get all aBox Axioms
            Set<OWLAxiom> aBoxAxioms = my_ont.getABoxAxioms(Imports.INCLUDED);
            // Generate muted version of the ontology/KG with "fresh nulls" where possible
            for (OWLAxiom axiom : aBoxAxioms) {
                if (!pt.isImmutable(axiom)) { // This axiom is not fully immutable, check positions
                    AxiomType at = axiom.getAxiomType();
                    OWLAxiom newAxiom = null;
                    // ClassAssertion Axioms can;t be partly immutable. Hence the SUBJECT Position is mutable
                    if (at.equals(AxiomType.CLASS_ASSERTION)) {
                        //                        logger.info("\n   **** Current axiom : " + axiom  );
                        OWLClassAssertionAxiomImpl oldAxiom = (OWLClassAssertionAxiomImpl) axiom;
                        newAxiom = new OWLClassAssertionAxiomImpl(getFreshNullIndividual(my_ont), oldAxiom.getClassExpression(), axiom.getAnnotations());
                        //                        logger.info("\n   **** New class assertion  : " + newAxiom  );
                    } else if (at.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                        // ObjectPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLObjectPropertyAssertionAxiomImpl oldAxiom = (OWLObjectPropertyAssertionAxiomImpl) axiom;
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLNamedIndividual newObject = (OWLNamedIndividual) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(my_ont);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            newObject = getFreshNullIndividual(my_ont);
                        }
                        newAxiom = new OWLObjectPropertyAssertionAxiomImpl(newSubject, oldAxiom.getProperty(), newObject, axiom.getAnnotations());
                    } else if (at.equals(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)) {
                        // NegativeDataPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLNegativeObjectPropertyAssertionAxiomImpl oldAxiom = (OWLNegativeObjectPropertyAssertionAxiomImpl) axiom;
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLNamedIndividual newObject = (OWLNamedIndividual) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(my_ont);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            newObject = getFreshNullIndividual(my_ont);
                        }
                        newAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(newSubject, oldAxiom.getProperty(), newObject, axiom.getAnnotations());
                    } else if (at.equals(AxiomType.DATA_PROPERTY_ASSERTION)) {
                        // DataPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLDataPropertyAssertionAxiomImpl oldAxiom = (OWLDataPropertyAssertionAxiomImpl) axiom;
                        OWLDataPropertyExpression property = oldAxiom.getProperty();
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLLiteral newObject = (OWLLiteral) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(my_ont);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            // Produce an alternative litteral value based on Data Property Ranges if any.
                            // Get all Data Property Range Axioms from Tbox
                            HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = tmpFixer.getDataPropertyRanges();
                            if (dataPropertyToRangeType.keySet().contains(property)) {
                                //There is a DataPropertyRange restriction to apply
                                OWLDatatype newDataType = dataPropertyToRangeType.get(property).asOWLDatatype();
                                if(!newDataType.equals(newObject.getDatatype())) {
                                    String lit = newObject.getLiteral();
                                    String lang = newObject.getLang();
                                    newObject = getFreshLiteralImpl(lit, lang, newDataType);
                                } // else, the literal is already in the correct DataType
                            } // else, no DataPropertyRange restriction for this property
                        }
                        newAxiom = new OWLDataPropertyAssertionAxiomImpl(newSubject, property, newObject, axiom.getAnnotations());
                    } else if (at.equals(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION)) {
                        // NegativeDataPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLNegativeDataPropertyAssertionAxiomImpl oldAxiom = (OWLNegativeDataPropertyAssertionAxiomImpl) axiom;
                        OWLDataPropertyExpression property = oldAxiom.getProperty();
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLLiteralImpl newObject = (OWLLiteralImpl) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(my_ont);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            // Produce an alternative litteral value based on Data Property Ranges if any.
                            // Get all Data Property Range Axioms from Tbox
                            HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = tmpFixer.getDataPropertyRanges();
                            if (dataPropertyToRangeType.keySet().contains(property)) {
                                //There is a DataPropertyRange restriction to apply
                                OWLDatatype newDataType = dataPropertyToRangeType.get(property).asOWLDatatype();
                                if(!newDataType.equals(newObject.getDatatype())) {
                                    String lit = newObject.getLiteral();
                                    String lang = newObject.getLang();
                                    newObject = getFreshLiteralImpl(lit, lang, newDataType);
                                } // else, the literal is already in the correct DataType
                            } // else, no DataPropertyRange restriction for this property
                        }
                        newAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(newSubject, property, newObject, axiom.getAnnotations());
                    }
                    // TODO: cover other types
                    //                else if (at.equals(AxiomType.SAME_INDIVIDUAL) || at.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                    //                            logger.info("\n   **** Axiom: " + axiom.toString() +
                    //                            "\n    *** Axiom type not supported yet: " + axiom.getAxiomType());
                    //                } else {
                    //                    logger.info("\n   **** Axiom: " + axiom.toString() +
                    //                            "\n    *** Axiom type not supported: " + axiom.getAxiomType());
                    //                }

                    // Update the tmp KG
                    if (newAxiom != null) { // An updated version of the axiom is available
                        newOntology.removeAxiom(axiom);
                        newOntology.addAxiom(newAxiom);
                    } // else: keep the axiom as is

                } // Else: for immutable axioms do nothing
            }
            //            logger.info("\n   **** New Tmp ontology Axioms after: " + newOntology.getABoxAxioms(Imports.EXCLUDED));

            // Check the muted version of the ontology/KG for consistency
            try {
                result = tmpFixer.CheckConsistency();
                this.pRepairable = result; // Update this value for future use
                this.pRepairabilitySkipp = true; // Now, CheckRepairability can be skipped until some new fix is applied
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }  catch(MalformedLiteralException e) {
                logger.info("MalformedLiteralException: The ontology contains at least one DATA_PROPERTY_ASSERTION or NEGATIVE_DATA_PROPERTY_ASSERTION with a literal value that is not compatible with some applicable DataPropertyRange.");
                result = false;
                this.pRepairable = result; // Update this value for future use
                this.pRepairabilitySkipp = true; // Now, CheckRepairability can be skipped until some new fix is applied
            }
        }
        long endTime = System.nanoTime();
        setRepairabilityCheckTime(getRepairabilityCheckTime() + (endTime - startTime) / 1000000);
        return result;
    }

    /**
     * Update an ontology by applying a fix (oldAxiom - newAxiom) in the ontology given.
     * @return
     */
    public boolean applyFix(Fix f){
        // Apply the fix
        boolean done = false;
        if(f.isUpdate()) {
            ChangeApplied change = my_ont.removeAxiom(f.getOldAxiom());
            if(change.equals(ChangeApplied.SUCCESSFULLY)){
                done = true;
            } else {
                logger.info("Warning (" + change + ") removing old axiom:" + f.getOldAxiom());
            }

            if(done) {
                ChangeApplied change2 = my_ont.addAxiom(f.getNewAxiom());
                if (f.getMutedPosition() == Position.SINGLE) {
                    pt.addImmutableAxiom(f.getNewAxiom());
                } else { // some position in this axiom is mutable
                    pt.addImmutablePosition(f.getNewAxiom(), f.getMutedPosition());
                }
                if(!change2.equals(ChangeApplied.SUCCESSFULLY)) {
                    logger.info("Warning (" + change2 + ") adding new axiom:" + f.getNewAxiom());
                }

                //Keep "seen repair values" for skipping CheckRepairability in certain cases!
                OWLNamedIndividualImpl usedIndividual = null;
                OWLLiteralImpl usedLiteral = null;
                AxiomType at = f.getNewAxiom().getAxiomType();
                if(at.equals(AxiomType.CLASS_ASSERTION)){
                    usedIndividual = (OWLNamedIndividualImpl)((OWLClassAssertionAxiomImpl)f.getNewAxiom()).getIndividual();
                } else if (at.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)){
                    OWLObjectPropertyAssertionAxiomImpl ax = (OWLObjectPropertyAssertionAxiomImpl)f.getNewAxiom();
                    if(f.getMutedPosition().equals(Position.SUBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getSubject();
                    } else if (f.getMutedPosition().equals(Position.OBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getObject();
                    }
                } else if (at.equals(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)){
                    OWLNegativeObjectPropertyAssertionAxiomImpl ax = (OWLNegativeObjectPropertyAssertionAxiomImpl)f.getNewAxiom();
                    if(f.getMutedPosition().equals(Position.SUBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getSubject();
                    } else if (f.getMutedPosition().equals(Position.OBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getObject();
                    }
                } else if (at.equals(AxiomType.DATA_PROPERTY_ASSERTION)){
                    OWLDataPropertyAssertionAxiomImpl ax = (OWLDataPropertyAssertionAxiomImpl)f.getNewAxiom();
                    if(f.getMutedPosition().equals(Position.SUBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getSubject();
                    } else if (f.getMutedPosition().equals(Position.OBJECT)){
                        usedLiteral = (OWLLiteralImpl)ax.getObject();
                    }
                } else if (at.equals(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION)){
                    OWLNegativeDataPropertyAssertionAxiomImpl ax = (OWLNegativeDataPropertyAssertionAxiomImpl)f.getNewAxiom();
                    if(f.getMutedPosition().equals(Position.SUBJECT)){
                        usedIndividual = (OWLNamedIndividualImpl)ax.getSubject();
                    } else if (f.getMutedPosition().equals(Position.OBJECT)){
                        usedLiteral = (OWLLiteralImpl)ax.getObject();
                    }
                }
                // TODO: cover other types
                //                else if (at.equals(AxiomType.SAME_INDIVIDUAL) || at.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                //                            logger.info("\n   **** Axiom: " + axiom.toString() +
                //                            "\n    *** Axiom type not supported yet: " + axiom.getAxiomType());
                //                } else {
                //                    logger.info("\n   **** Axiom: " + axiom.toString() +
                //                            "\n    *** Axiom type not supported: " + axiom.getAxiomType());
                //                }
                if(usedIndividual != null){
                    if(this.usedIndividuals.contains(usedIndividual)){
                        // This value has been used before: Next pRepairability check can't be skipped
                        this.pRepairabilitySkipp = false;
                    } // else, This value has not been used before: Next pRepairability check can be skipped
                } else if (usedLiteral != null){
                    if(this.usedLiterals.contains(usedLiteral)){
                        // This value has been used before: Next pRepairability check can't be skipped
                        this.pRepairabilitySkipp = false;
                    } // else, This value has not been used before: Next pRepairability check can be skipped
                }
            }
            // Add and Delete are not used by the current implementation
        } else if(f.isAdd()){
            my_ont.addAxiom(f.getNewAxiom());
            pt.addImmutableAxiom(f.getNewAxiom());
        } else if(f.isDelete()){
            my_ont.removeAxiom(f.getOldAxiom());
        } else {
            logger.info("Error! Unsupported Fix type. Fixes are expected to be one of: Update, Delete, Add");
        }
        return done;
    }
}
