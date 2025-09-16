package org.nikolasparaskakis.core;//import org.semanticweb.HermiT.datatypes.MalformedLiteralException;



import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import uk.ac.manchester.cs.owl.owlapi.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;



@SuppressWarnings({"unused", "DuplicatedCode", "rawtypes", "MismatchedQueryAndUpdateOfCollection", "HttpUrlsUsage", "CommentedOutCode"})
public class Fixer {


    /**
     * Fix selection strategy
     * 1: trivial-fix
     * 2: greedy-fix
     * 3: rank-fix
     * 4: random-fix
     */
    private int fixSelection;

    /**
     * Timeout for the fixing process (in ms)
     */
    private long fixingTimeoutMillis;

    /**
     * Reasoner selection
     */
    private int reasonerSelection;

    /**
     * Timeout for the reasoner (in ms)
     */
    private long reasonerTimeoutMillis;

    /**
     * Limit on the number of explanations to be computed for each entailment
     */
    private int explanationsLimit;


    private long perOpTimeoutMillis;

    /**
     * Whether to use the Maximally Contained Diagnosis (MCD) approach
     */
    private boolean mcd;

    /**
     * Whether the current ontology is repairable
     */
    private boolean pRepairable = false;

    /**
     * Whether to skip the pRepairability check by using the pRepairable value
     */
    private boolean pRepairabilitySkipp = false; // Whether skipp a pRepairability check by using the pRepairable value

    /**
     *
     */
    private ArrayList<OWLNamedIndividualImpl> usedIndividuals = null; // All the individuals(values) used so far in fixing

    /**
     *
     */
    private ArrayList<OWLLiteralImpl> usedLiterals = null; // All the Literals(values) used so far in fixing

    /**
     *
     */
    private OWLOntology ontology = null;

    /**
     * Position Tracker of the current ontology
     */
    private PositionTracker pt = null;

    /**
     * Time spent generating fixes (in ms)
     */
    private static long fixGenerationTime = 0;

    /**
     * Time spent filtering fixes (in ms)
     */
    private static long fixFilteringTime = 0;

    /**
     * Time spent checking repairability (in ms)
     */
    private static long repairabilityCheckTime = 0;

    /**
     * Time spent counting inconsistencies (in ms)
     */
    private static long inconsistencyCountingTime = 0;


    public static void clearTimes(){
        fixGenerationTime = 0;
        fixFilteringTime = 0;
        repairabilityCheckTime = 0;
        inconsistencyCountingTime = 0;
    }


    private final ArrayList<AxiomType> PropertyAssertionAxiomTypes = new ArrayList<>(
            Arrays.asList(
                    AxiomType.OBJECT_PROPERTY_ASSERTION,
                    AxiomType.DATA_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION
            )
    );

    private final ArrayList<AxiomType> DataPropertyAssertionAxiomTypes = new ArrayList<>(
            Arrays.asList(
                    AxiomType.DATA_PROPERTY_ASSERTION,
                    AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION
            )
    );


    public Fixer(Fixer f){
        // Create a copy of the ontology
        OWLOntologyManager man = createOWLOntologyManager();
        try {
            this.ontology = man.copyOntology(f.ontology, OntologyCopy.DEEP);
            this.pt = new PositionTracker(f.pt);
            this.reasonerSelection = f.reasonerSelection;
            this.reasonerTimeoutMillis = f.reasonerTimeoutMillis;
            this.perOpTimeoutMillis = f.perOpTimeoutMillis;
            this.fixSelection = f.fixSelection;
            this.fixingTimeoutMillis = f.fixingTimeoutMillis;
            this.explanationsLimit = f.explanationsLimit;
            this.mcd = f.mcd;
            this.pRepairable = f.pRepairable;
            this.pRepairabilitySkipp = f.pRepairabilitySkipp;
            this.usedIndividuals = new ArrayList<>();
            this.usedLiterals = new ArrayList<>();
            this.usedIndividuals.addAll(f.usedIndividuals);
            this.usedLiterals.addAll(f.usedLiterals);
        } catch (OWLOntologyCreationException e) {
            System.err.println(e.getMessage());
        }
    }

    public Fixer(OWLOntology my_ont, PositionTracker pt, int fixSelection, long fixingTimeoutMillis, int reasonerSelection, long reasonerTimeoutMillis, long perOpTimeoutMillis, int explanationsLimit, boolean mcd) {
        this.ontology = my_ont;
        this.pt = pt;
        this.fixSelection = fixSelection;
        this.fixingTimeoutMillis = fixingTimeoutMillis;
        this.reasonerSelection = reasonerSelection;
        this.reasonerTimeoutMillis = reasonerTimeoutMillis;
        this.perOpTimeoutMillis = perOpTimeoutMillis;
        this.explanationsLimit = explanationsLimit;
        this.mcd = mcd;
        this.usedIndividuals = new ArrayList<>();
        this.usedLiterals = new ArrayList<>();
    }



    public static String printFixes(HashSet<Fix> fixes) {

        HashSet<Fix> addFixes = new HashSet<>();
        HashSet<Fix> updateFixes = new HashSet<>();
        HashSet<Fix> deleteFixes = new HashSet<>();

        for (Fix f : fixes) {
            if (f.isAdd())
                addFixes.add(f);
            if (f.isUpdate())
                updateFixes.add(f);
            if (f.isDelete())
                deleteFixes.add(f);
        }

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("******* Number of fixes: ").append(fixes.size())
                .append(" [ ").append(addFixes.size()).append(" Add, ")
                .append(updateFixes.size()).append(" Update, ")
                .append(deleteFixes.size()).append(" Delete ]\n");

        // Log Add Fixes
        if (!addFixes.isEmpty()) {
            logBuilder.append("******* Add Fixes:\n");
            for (Fix f : addFixes) {
                logBuilder.append("    * New Axiom: ").append(f.getNewAxiom()).append("\n");
            }
        }

        // Log Update Fixes
        if (!updateFixes.isEmpty()) {
            logBuilder.append("******* Update Fixes:\n");
            for (Fix f : updateFixes) {
                logBuilder.append("    * Old Axiom: ").append(f.getOldAxiom())
                        .append(", New Axiom: ").append(f.getNewAxiom()).append("\n");
            }
        }

        // Log Delete Fixes
        if (!deleteFixes.isEmpty()) {
            logBuilder.append("******* Delete Fixes:\n");
            for (Fix f : deleteFixes) {
                logBuilder.append("    * Old Axiom: ").append(f.getOldAxiom()).append("\n");
            }
        }

        return logBuilder.toString();
    }


    // Getters and Setters for static time tracking variables
    public static long getFixGenerationTime() {
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
    public HashSet<Fix> KeepSoundFixes(HashSet<Fix> fixes) {
        long startTime = System.nanoTime();
        HashSet<Fix> soundFixes = new HashSet<>();
        int bestScore = Integer.MAX_VALUE;
        int soundFixesChecked = 0;
        boolean stop = false;
        Iterator<Fix> it = fixes.iterator();
        while(!stop & it.hasNext()){
            Fix f = it.next();
//            logger.info("      - BestScore : " + bestScore);
            // Create copy of ontology and PositionTracker to check repairability
            Fixer tmpFixer = new Fixer(this);
            // Apply the fix
            boolean successfullyApplied = tmpFixer.applyFix(f);
            if(tmpFixer.CheckRepairability().getRepairable()){
                soundFixesChecked++;
                if(fixSelection==3 & !f.getTrivial()){// trivial fixes are (almost?) always "the best", ignore them in rank-fix!
                    int currentScore = tmpFixer.countInconsistencies();
//                    logger.info("      - BestScore : " + bestScore);
//                    logger.info("      - currScore : " + currentScore);
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
            }
            long timeSoFar = (System.nanoTime() - startTime) / 1000000;
            if(timeSoFar >= fixingTimeoutMillis){
                stop = true; // due to overtime!
            }
        }
        long endTime = System.nanoTime();
        setFixFilteringTime(getFixFilteringTime() + ((endTime - startTime) / 1000000));
        return soundFixes;
    }

    /**
     * Compute some sound fixes for the given explanation
     * @param explanation   A set of sets of OWLAxioms representing an explanation for an inconsistency
     * @return              A set of sound fixes for the given explanation
     */
    public HashSet<Fix> ComputeSomeSoundFixes(Set<Set<OWLAxiom>> explanation) {
//        Collections.shuffle(explanation); // Shuffle outer list
//        for (List<OWLAxiom> group : explanation) {
//            Collections.shuffle(group); // Shuffle each inner list
//        }
        long startTime = System.nanoTime();
        HashSet<Fix> fixes = new HashSet<>(); // Set of alternative fixes for this inconsistency
        OWLNamedIndividual newInd = getFreshNullIndividual(ontology);
        /* Get all individuals in the original ontology */
        List<OWLNamedIndividual> individuals = ontology.individualsInSignature(Imports.INCLUDED).collect(Collectors.toList());
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
                        if(fixSelection!=1){
                        // Add fixes by replacing the individual with other individuals of the KG
                            Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                            while(!soundFixFound && itIndividuals.hasNext()){
                                OWLNamedIndividual indTotal = itIndividuals.next();
                                if (!indTotal.equals(ind)) {
                                    altAxiom = new OWLClassAssertionAxiomImpl(indTotal, c, ax.getAnnotations());
                                    HashSet<Fix> currentFixes = new HashSet<>();
                                    currentFixes.add(new Fix(ax, altAxiom, p));
                                    if(fixSelection==2){//Check soundness prior to adding this fix
                                        //check soundness
                                        currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldSubject)) {
                                altAxiom = new OWLObjectPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the OBJECT individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldObject)) {
                                altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indTotal, oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldSubject)) {
                                altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the OBJECT individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldObject)) {
                                altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indTotal, oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    // For literals, that include a type encoded in them, non-joint positions should also be considered for fixing the type
                    // Check the TBox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.containsKey(oldAx.getProperty())) {
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldSubject)) {
                                altAxiom = new OWLDataPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                    // For literals, that include a type encoded in them, non-joint positions should also be considered for fixing the type
                    // Check the TBox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.containsKey(oldAx.getProperty())) {
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
                    if(fixSelection!=1) {
                        // Add fixes by replacing the individual with other individuals of the KG
                        Iterator<OWLNamedIndividual> itIndividuals = individuals.iterator();
                        while(!soundFixFound && itIndividuals.hasNext()){
                            OWLNamedIndividual indTotal = itIndividuals.next();
                            if (!indTotal.equals(oldSubject)) {
                                altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                                HashSet<Fix> currentFixes = new HashSet<>();
                                currentFixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                                if(fixSelection==2){//Check soundness prior to adding this fix
                                    //check soundness
                                    currentFixes = new HashSet<>(this.KeepSoundFixes(currentFixes));
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
                System.out.println("\n   **** Axiom: " + ax +
                        "\n    *** Axiom type not supported yet: " + ax.getAxiomType());
            } else {
                System.out.println("\n   **** Axiom: " + ax +
                        "\n    *** Axiom type not supported: " + ax.getAxiomType());
            }
        }
        long endTime = System.nanoTime();
        setFixGenerationTime(getFixGenerationTime() + ((endTime - startTime) / 1000000));
        return fixes;
    }


    public HashSet<Fix> ComputeIARFixes(Set<Set<OWLAxiom>> explanation) {

        HashSet<Fix> iar_fixes = new HashSet<>();

        Set<OWLAxiom> distinctAxioms = explanation.stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Set<OWLAxiom> tBoxAxioms = this.ontology.tboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());
        Set<OWLAxiom> rBoxAxioms = this.ontology.rboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());

        for (OWLAxiom old_axiom : distinctAxioms) {
            if (!tBoxAxioms.contains(old_axiom) && !rBoxAxioms.contains(old_axiom)) {
                Fix fix = new Fix(old_axiom, null, null);
                iar_fixes.add(fix);
            }
        }

        return  iar_fixes;
    }

    /**
     * Find all joint positions in all axioms in each explanation (plus Object positions in data property assertions, as they can be used despite not joint)
     *  And calculate the number of explanations each position is contained in (for maximality ranking)
     * @param explanations A set of explanations (set of axioms) causing inconsistency
     * @return  A map X -> Y, mapping each joint AxiomPosition X to the number of explanations it is contained in Y
     */
    public HashMap<AxiomPosition,Integer> getPositionMaximality(Set<Set<OWLAxiom>> explanations){
        OWLNamedIndividual newInd = getFreshNullIndividual(ontology);
        //Rank OWLAxioms to identify the Maximally contained in different Explanations
        HashMap<AxiomPosition,Integer> positionsContained = new  HashMap<>();
        for (Set<OWLAxiom> expAxioms : explanations) {
            // A: expAxioms gathers all axioms of this explanation
            HashSet<OWLAxiom> expABoxAxioms = new HashSet<>();
            HashSet<OWLNamedIndividual> allInds = new HashSet<>();
            HashMap<AxiomPosition, OWLNamedIndividual> allPositions = new HashMap<>();
            // B: Find joint positions in all axioms of this explanation
            HashSet<OWLNamedIndividual> jointPositionInds = new HashSet<>();
            for(OWLAxiom ax: expAxioms){ // For each axiom
                AxiomType axType = ax.getAxiomType();
                if(AxiomType.ABoxAxiomTypes.contains(axType)){
                    //This is an Abox Axiom
                    expABoxAxioms.add(ax);

                    ArrayList<OWLNamedIndividual> ax_in = new ArrayList<>();

                    // Keep all possible positions
                    if(PropertyAssertionAxiomTypes.contains(axType)){ // the axiom has subject and object
                        OWLPropertyAssertionAxiom pax = (OWLPropertyAssertionAxiom) ax;
                        OWLNamedIndividual sub = (OWLNamedIndividual) pax.getSubject();
                        AxiomPosition subPos = new AxiomPosition(ax, Position.SUBJECT);
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
                        // This explanation may also include some DataPropertyRange or other parts of the TBox that are relevant
                        // We could gather such information from the explanation at this point to be used later for fixing.
                        allPositions.put(obPos, obj);
                    } else { // the axiom has a single position
                        ax_in.addAll(ax.getIndividualsInSignature());
                        AxiomPosition sinPos = new AxiomPosition(ax, Position.SINGLE);
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
                        // In the original work non-joint positions are not considered for fixing, but we need it for covering DATA restrictions
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
    public HashSet<AxiomPosition> choosePositions(HashMap<AxiomPosition,Integer> positionsContained){
        HashSet<AxiomPosition> apChosen = new HashSet<>(); // All the selected positions to be considered for fix generation.
        // C: Generate fixes for the selected positions
        if(mcd) { // Generate fixes based on the maximally contained joint position(s) in Abox axioms
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
//        Collections.shuffle(explanation); // Shuffle outer list
//        for (List<OWLAxiom> group : explanation) {
//            Collections.shuffle(group); // Shuffle each inner list
//        }
        long startTime = System.nanoTime();
        HashSet<Fix> fixes = new HashSet<>(); // Set of alternative fixes for this inconsistency
        OWLNamedIndividual newInd = getFreshNullIndividual(ontology);
        /* Get all individuals in the original ontology */
        List<OWLNamedIndividual> individuals = ontology.individualsInSignature(Imports.INCLUDED).collect(Collectors.toList());
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
                        for (OWLNamedIndividual indTotal : individuals) {
                            if (!indTotal.equals(ind)) {
                                altAxiom = new OWLClassAssertionAxiomImpl(indTotal, c, ax.getAnnotations());
                                fixes.add(new Fix(ax, altAxiom, p)); // p should be Position.SINGLE
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
                    // Add fixes by replacing the individual with other individuals of the KG
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldSubject)) {
                            altAxiom = new OWLObjectPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                } else { //the mutable position is Position.OBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldObject)) {
                            altAxiom = new OWLObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indTotal, oldAx.getAnnotations());
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
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldSubject)) {
                            altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                } else { //the mutable position is Position.OBJECT
                    OWLIndividual oldObject = oldAx.getObject();
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    OWLAxiom altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), newInd, oldAx.getAnnotations());
                    fixes.add(new Fix(ax, altAxiom, Position.OBJECT));
                    // Add fixes by replacing the OBJECT individual with other individuals of the KG
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldObject)) {
                            altAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(oldAx.getSubject(), oldAx.getProperty(), indTotal, oldAx.getAnnotations());
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
                    // For literals, that include a type encoded in them, non-joint positions should also be considered for fixing the type
                    // Check the TBox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.containsKey(oldAx.getProperty())) {
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
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldSubject)) {
                            altAxiom = new OWLDataPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
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
                    // For literals, that include a type encoded in them, non-joint positions should also be considered for fixing the type
                    // Check the TBox to produce targeted fixes by literal types indicated in DataPropertyRange Axioms
                    HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = this.getDataPropertyRanges();
                    if (dataPropertyToRangeType.containsKey(oldAx.getProperty())) {
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
                    for (OWLNamedIndividual indTotal : individuals) {
                        if (!indTotal.equals(oldSubject)) {
                            altAxiom = new OWLNegativeDataPropertyAssertionAxiomImpl(indTotal, oldAx.getProperty(), oldAx.getObject(), oldAx.getAnnotations());
                            fixes.add(new Fix(ax, altAxiom, Position.SUBJECT));
                        }
                    }
                }
            }
            // TODO: cover other types
            else if ( at.equals(AxiomType.SAME_INDIVIDUAL) || at.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                System.out.println("\n   **** Axiom: " + ax +
                        "\n    *** Axiom type not supported yet: " + ax.getAxiomType());
            } else {
                System.out.println("\n   **** Axiom: " + ax +
                        "\n    *** Axiom type not supported: " + ax.getAxiomType());
            }
        }
        long endTime = System.nanoTime();
        setFixGenerationTime(getFixGenerationTime() + (endTime - startTime) / 1000000);
        return fixes;
    }

    /**
     * Generate a fresh null individual for the ontology provided
     * @param my_ont    The ontology for which the fresh null individual is generated
     * @return  A fresh null individual (OWLNamedIndividual)
     */
    public OWLNamedIndividual getFreshNullIndividual(OWLOntology my_ont){
        OWLNamedIndividual newInd;
        if(my_ont.isAnonymous()){
//            newInd = new OWLNamedIndividualImpl(IRI.create("NamedIndividual" + NamedNulls));
            String uuid = UUID.randomUUID().toString();
            newInd = new OWLNamedIndividualImpl(IRI.create("NamedIndividual" + uuid));
        } else {
            String uuid = UUID.randomUUID().toString();
//            newInd = new OWLNamedIndividualImpl(IRI.create(my_ont.getOntologyID().getOntologyIRI().get().getNamespace(), "NamedIndividual" + NamedNulls));
            Optional<IRI> optIri = my_ont.getOntologyID().getOntologyIRI();
            String namespace;
            if (optIri.isPresent()) {
                namespace = optIri.get().getNamespace();
            } else {
                // Fallback to a default namespace if not present
                namespace = "http://example.org/default#";
            }
            newInd = new OWLNamedIndividualImpl(IRI.create(namespace, "NamedIndividual" + uuid));
        }
//        NamedNulls++;
        return newInd;
    }


    private OWLLiteralImpl getFreshLiteralImpl(String lit, String lang, OWLDatatype newDatatype) {
        //TODO: Check that the generated literal is not "malformed" (e.g. "Q2342"<Integer>) leading to an exception

        return new OWLLiteralImpl(lit, lang, newDatatype);
    }


    public ConsistencyCheckOutcome CheckConsistency(long timeoutMillis) {
        ConsistencyCheckOutcome consistencyCheckOutcome;
        try (MyHandler myHandler = new MyHandler(ontology.getAxioms(), reasonerSelection)) {
            consistencyCheckOutcome = myHandler.isConsistent(timeoutMillis);
        }
        return consistencyCheckOutcome;
    }

    public ConsistencyCheckOutcome CheckConsistency() {
        ConsistencyCheckOutcome result;
        try (MyHandler myHandler = new MyHandler(ontology.getAxioms(), reasonerSelection)) {
            result = myHandler.isConsistent();
        }
        return result;
    }

    public int countInconsistencies() {
        long startTime = System.nanoTime();
        int result = 0;
        try (MyHandler myHandler = new MyHandler(ontology.getAxioms(), reasonerSelection)) {
            if (!myHandler.isConsistent().getConsistent()) {
                // Some inconsistency should be found TODO: CHECK THIS and REFACTOR
                ExplanationOutcome j = myHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                result = j.getExplanations().size();
            }
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
        // Get all Data Property Range Axioms from TBox
        Set<OWLDataPropertyRangeAxiom> dataPropertyRanges = ontology.getAxioms(AxiomType.DATA_PROPERTY_RANGE,Imports.INCLUDED);
        // In OWLDataPropertyRangeAxioms, a Data Property Expression can have only a single Data Range: "A data property range axiom DataPropertyRange( DPE DR ) states that the range of the data property expression DPE is the data range DR — that is, if some individual is connected by DPE with a literal x, then x is in DR. The arity of DR must be one." https://www.w3.org/TR/owl2-syntax/#Data_Property_Range
        HashMap<OWLDataPropertyExpression,OWLDatatype> dataPropertyToRangeType = new HashMap<>();
        for(OWLDataPropertyRangeAxiom t: dataPropertyRanges){
            dataPropertyToRangeType.put(t.getProperty(), t.getRange().asOWLDatatype());
        }
        return dataPropertyToRangeType;
    }

    public RepairabilityCheckOutcome CheckRepairability() {
        long startTime = System.nanoTime();
        RepairabilityCheckOutcome result;
        // TODO: Add an argument for optimized pRepairability check
        if(this.pRepairabilitySkipp){
            // Skip CheckRepairability in certain cases: i.e. when the repair value is not "not in" "seen repair values"
            // This is a case where the CheckRepairability can be skipped
            result = new RepairabilityCheckOutcome(this.pRepairable, null, null);
        }
        else { // Normal calculation of pRepairability
            Fixer tmpFixer = new Fixer(this);
            OWLOntology newOntology = tmpFixer.ontology;
            // Get all aBox Axioms
            Set<OWLAxiom> aBoxAxioms = ontology.getABoxAxioms(Imports.INCLUDED);
            // Generate muted version of the ontology/KG with "fresh nulls" where possible
            for (OWLAxiom axiom : aBoxAxioms) {
                if (!pt.isImmutable(axiom)) { // This axiom is not fully immutable, check positions
                    AxiomType at = axiom.getAxiomType();
                    OWLAxiom newAxiom = null;
                    // ClassAssertion Axioms can;t be partly immutable. Hence, the SUBJECT Position is mutable
                    if (at.equals(AxiomType.CLASS_ASSERTION)) {
                        //                        logger.info("\n   **** Current axiom : " + axiom  );
                        OWLClassAssertionAxiomImpl oldAxiom = (OWLClassAssertionAxiomImpl) axiom;
                        newAxiom = new OWLClassAssertionAxiomImpl(getFreshNullIndividual(ontology), oldAxiom.getClassExpression(), axiom.getAnnotations());
                        //                        logger.info("\n   **** New class assertion  : " + newAxiom  );
                    } else if (at.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                        // ObjectPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLObjectPropertyAssertionAxiomImpl oldAxiom = (OWLObjectPropertyAssertionAxiomImpl) axiom;
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLNamedIndividual newObject = (OWLNamedIndividual) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(ontology);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            newObject = getFreshNullIndividual(ontology);
                        }
                        newAxiom = new OWLObjectPropertyAssertionAxiomImpl(newSubject, oldAxiom.getProperty(), newObject, axiom.getAnnotations());
                    } else if (at.equals(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION)) {
                        // NegativeDataPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLNegativeObjectPropertyAssertionAxiomImpl oldAxiom = (OWLNegativeObjectPropertyAssertionAxiomImpl) axiom;
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLNamedIndividual newObject = (OWLNamedIndividual) oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(ontology);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            newObject = getFreshNullIndividual(ontology);
                        }
                        newAxiom = new OWLNegativeObjectPropertyAssertionAxiomImpl(newSubject, oldAxiom.getProperty(), newObject, axiom.getAnnotations());
                    } else if (at.equals(AxiomType.DATA_PROPERTY_ASSERTION)) {
                        // DataPropertyAssertion
                        // This axiom is not fully immutable, check immutability of subject/object positions
                        OWLDataPropertyAssertionAxiomImpl oldAxiom = (OWLDataPropertyAssertionAxiomImpl) axiom;
                        OWLDataPropertyExpression property = oldAxiom.getProperty();
                        OWLNamedIndividual newSubject = (OWLNamedIndividual) oldAxiom.getSubject();
                        OWLLiteral newObject = oldAxiom.getObject();
                        if (!pt.hasImmutableSubject(axiom)) {
                            newSubject = getFreshNullIndividual(ontology);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            // Produce an alternative literal value based on Data Property Ranges if any.
                            // Get all Data Property Range Axioms from TBox
                            HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = tmpFixer.getDataPropertyRanges();
                            if (dataPropertyToRangeType.containsKey(property)) {
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
                            newSubject = getFreshNullIndividual(ontology);
                        }
                        if (!pt.hasImmutableObject(axiom)) {
                            // Produce an alternative literal value based on Data Property Ranges if any.
                            // Get all Data Property Range Axioms from TBox
                            HashMap<OWLDataPropertyExpression, OWLDatatype> dataPropertyToRangeType = tmpFixer.getDataPropertyRanges();
                            if (dataPropertyToRangeType.containsKey(property)) {
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
            ConsistencyCheckOutcome consistencyCheckOutcome = tmpFixer.CheckConsistency();
            result = new RepairabilityCheckOutcome(consistencyCheckOutcome.getConsistent(), consistencyCheckOutcome.getStatus().toRepairabilityCheckOutcomeStatus(), consistencyCheckOutcome.getError());
            this.pRepairable = result.getRepairable(); // Update this value for future use
            this.pRepairabilitySkipp = false; // Now, CheckRepairability can be skipped until some new fix is applied TODO: Change this back to true
            //            catch(MalformedLiteralException e) {
//                logger.info("MalformedLiteralException: The ontology contains at least one DATA_PROPERTY_ASSERTION or NEGATIVE_DATA_PROPERTY_ASSERTION with a literal value that is not compatible with some applicable DataPropertyRange.");
//                result = false;
//                this.pRepairable = result; // Update this value for future use
//                this.pRepairabilitySkipp = false; // Now, CheckRepairability can be skipped until some new fix is applied TODO: Change this back to true
//            }
        }
        long endTime = System.nanoTime();
        setRepairabilityCheckTime(getRepairabilityCheckTime() + (endTime - startTime) / 1000000);
        return result;
    }

    /**
     * Apply a fix to the current ontology/KG
     * @param f The fix to be applied
     * @return  true if the fix was successfully applied, false otherwise
     */
    public boolean applyFix(Fix f){
        // Apply the fix
        boolean done = false;
        if(f.isUpdate()) {
            ChangeApplied change = ontology.removeAxiom(f.getOldAxiom());
            if(change.equals(ChangeApplied.SUCCESSFULLY)){
                done = true;
            } else {
                System.out.println("Warning (" + change + ") removing old axiom:" + f.getOldAxiom());
            }

            if(done) {
                ChangeApplied change2 = ontology.addAxiom(f.getNewAxiom());
                if (f.getMutedPosition() == Position.SINGLE) {
                    pt.addImmutableAxiom(f.getNewAxiom());
                } else { // some position in this axiom is mutable
                    pt.addImmutablePosition(f.getNewAxiom(), f.getMutedPosition());
                }
                if(!change2.equals(ChangeApplied.SUCCESSFULLY)) {
                    System.out.println("Warning (" + change2 + ") adding new axiom:" + f.getNewAxiom());
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
                    } else {// else, This value has not been used before: Next pRepairability check can be skipped
                        // Now add this value to the used ones for future use
                        this.usedIndividuals.add(usedIndividual);
                    }
                } else if (usedLiteral != null){
                    if(this.usedLiterals.contains(usedLiteral)){
                        // This value has been used before: Next pRepairability check can't be skipped
                        this.pRepairabilitySkipp = false;
                    } else {// else, This value has not been used before: Next pRepairability check can be skipped
                        // Now add this value to the used ones for future use
                        this.usedLiterals.add(usedLiteral);
                    }
                }
            }
            // Add and Delete are not used by the current implementation
        } else if(f.isAdd()){
            ontology.addAxiom(f.getNewAxiom());
            pt.addImmutableAxiom(f.getNewAxiom());
            done = true;
        } else if(f.isDelete()){
            ontology.removeAxiom(f.getOldAxiom());
            done = true;
        } else {
            System.err.println("Error! Unsupported Fix type. Fixes are expected to be one of: Update, Delete, Add");
        }
        return done;
    }


    public OWLOntology getOntology() {
        return ontology;
    }
}
