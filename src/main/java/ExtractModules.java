import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import org.javatuples.Pair;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;


import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class ExtractModules {

    static Logger logger = Logger.getLogger(ExtractModules.class.getName());

    /* TODO add here input arguments (ontology .ttl + class expression in manchester format) */
    public static void main(String[] args) throws OWLOntologyCreationException, IOException {
        /* Parse input parameters */
        ReasonExplainArgs c_args = new ReasonExplainArgs();
        JCommander jc = new JCommander(c_args);
        try {
            jc.parse(args);
        } catch (Exception e) {
            jc.usage();
            System.exit(0);
        }
        if (c_args.help) {
            jc.usage();
            System.exit(0);
        }

        /* Setup the logger */
        FileHandler fh;
        try {
            // This block configures the logger with handler and formatter
            fh = new FileHandler(c_args.logFile);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("\n++++++++++++++++ Execution started with input parameters: " + c_args.getAllParams() + "++++++++++++++++");// + args[0] + ", " + args[1] + ", " + args[2]);
        long totalExecTimeStart = System.nanoTime();

        /* MARITIME ontology */
//        OntBreakdown my_ont = new OntBreakdown("example-KG/MSA-HSN.owl");

        /* PIZZA ontology */
//        OntBreakdown my_ont = new OntBreakdown(IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl"));

        /* Comic book ontology NO inconsistencies found */
//        OntBreakdown my_ont = new OntBreakdown("comicbook.owl");

        /* Wine ontology NO inconsistencies found */
//        OntBreakdown my_ont = new OntBreakdown("wine.owl");

        /* SHACL ontology NO inconsistencies found */
//        OntBreakdown my_ont = new OntBreakdown("SHACL.owl");

        /* charis-muslim */
//        OntBreakdown my_ont = new OntBreakdown("example-KG/charis-inconsistent.ttl");

        /* D3FEND memory problems */
//        OntBreakdown my_ont = new OntBreakdown("d3fend.ttl");

        /* COSMO */
//        OntBreakdown my_ont = new OntBreakdown("COSMO.ttl");

        // TODO ? Handle JSON-LD formats
        OntBreakdown my_ont;
        PrintWriter pw = new PrintWriter("merged_ontology.ttl");
        BufferedReader br = new BufferedReader(new FileReader(c_args.tBox));
        String line = br.readLine();
        while (line != null)
        {
            pw.println(line);
            line = br.readLine();
        }
        if (!c_args.aBox.isEmpty()) {
            if (c_args.aBox.split(",").length > 1) {
//              /* Merge A-Boxes */
//              // TODO, manage urls, e.g. if (g.contains("http"))...
                ArrayList<String> aBoxes = new ArrayList<>(Arrays.asList(c_args.aBox.split(",")));
                for (String g : aBoxes){
                    br = new BufferedReader(new FileReader(g));
                    line = br.readLine();
                    while (line != null) {
                        pw.println(line);
                        line = br.readLine();
                    }
                }
            }
            else {
                br = new BufferedReader(new FileReader(c_args.aBox));
                line = br.readLine();
                while (line != null) {
                    pw.println(line);
                    line = br.readLine();
                }
            }
        }
        pw.flush();
        // closing resources
        br.close();
        pw.close();

        /* Read the T-Box and A-Box(es) */
        my_ont = new OntBreakdown("merged_ontology.ttl", c_args.debugLog);
//        try {
//            logger.info("Loaded ontology: " + my_ont.getOntology().getOntologyID().getOntologyIRI().get().toString());
//        }
//        catch (Exception e) {
//            logger.info("Loaded ontology: No name found.");
//        }

        /* Print the ontology summary */
//        logger.info("######## Summary ########");
//        logger.info(my_ont.logOntologySummary());
//        logger.info("######## Axioms ########");
//        logger.info(my_ont.logOntologyAxioms());
//        logger.info("######## A-Box ########");
//        my_ont.printOntologyABox();
//        logger.info("######   T-Box   ######.");
//        logger.info(my_ont.logOntologyTBox(","));
//        logger.info("######.   T-Box Ends   ######");
//        logger.info("######## Summary Ends ########");

        /* Save merged KG to file, if flag is enabled */
        if (c_args.save) {
            OWLOntologyManager man_temp = createOWLOntologyManager();
            OWLDocumentFormat owlForm = new OWLXMLDocumentFormat();
            File outfile = new File("merged-ontology.owl");
            try {
//                logger.info("Saving merged ontology:\n" + outfile.toString());
                man_temp.saveOntology(my_ont.getOntology(), owlForm, IRI.create(outfile.toURI()));
            } catch (OWLOntologyStorageException e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
//                logger.info("Error saving merged ontology:\n" + sw.toString());
            }
        }
//////////////////////////////////////////////////
//        my_ont.printOntologySummary();
//        my_ont.printOntologyClasses();
//        my_ont.printOntologyIndividuals();
//        Stream<OWLNamedIndividual> inds =  my_ont.getOntologyIndividuals();
//        logger.info("Individuals count: " + inds.count());
//        my_ont.printOntologyObjectProperties();
//        my_ont.printOntologyABox();
//        logger.info(my_ont.logOntologyAxioms());
//        my_ont.printOntologyTBox();
//        my_ont.printOntologyRBox();
//        my_ont.printOntologyLogicalAxioms();
/////////////////////////////////////////////////
//        logger.info("\n######## All Axioms ########");
//        my_ont.printOntologyABox();
//        my_ont.printOntologyTBox();
//        my_ont.printOntologyRBox();
//        my_ont.printOntologyAxioms();
//        my_ont.printOntologyLogicalAxioms();

//        logger.info("\n######## Modules of individuals ########");


        /* Initialization of variables for storing the results */
        long t_box_check_time = 0;
        long a_box_check_time = 0;
        long total_exec_time = 0;
        long no_of_modules = 0;
        List<Integer> module_sizes = new ArrayList<>();
//        AtomicInteger no_of_inconsistency = new AtomicInteger();
//        no_of_inconsistency.set(0);
        AtomicInteger no_of_fixes = new AtomicInteger();
        no_of_fixes.set(0);
        AtomicInteger no_of_applied_fixes = new AtomicInteger();
        no_of_applied_fixes.set(0);
        AtomicInteger no_of_reasoner_timeouts = new AtomicInteger();
        no_of_reasoner_timeouts.set(0);
        boolean ends_up_consistent = false;
        AtomicBoolean overtime = new AtomicBoolean(false);
        AtomicInteger fixingRounds = new AtomicInteger();
        fixingRounds.set(0);
        AtomicLong fixGenerationTime = new AtomicLong(0);
        AtomicLong fixFilteringTime = new AtomicLong(0);
        AtomicLong repairabilityCheckTime = new AtomicLong(0);
        AtomicLong inconsistencyCountingTime = new AtomicLong(0);
        long CheckConsistencyTime = 0;
        String experiment_folder = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        /* Check for T-Box inconsistency if input parameter -c is given */
        if (c_args.checkT) {
            long startTime = System.nanoTime();
            OWLOntology b = my_ont.getTBoxOnly();
            OntBreakdown tModuleBr = new OntBreakdown(b, c_args.debugLog);
            String tBoxInc;
            try {
                tBoxInc = tModuleBr.explainInconsistencySatisfiable2();
            } catch (Exception e){
                /* Not sure if this is required.. if b = my_ont.getOntology()
                   then if inconsistencies exist regardless of type, it is required.
                   When b = my_ont.getTBoxOnly(), then it is not required. */
                tBoxInc = tModuleBr.explainInconsistencySatisfiable();
            }
            long endTime = System.nanoTime();
            t_box_check_time = (endTime - startTime) / 1000000;
            try {
                logger.info("Processing of ontology " + my_ont.getOntology().getOntologyID().getOntologyIRI().get().toString() + " for T-Box inconsistency detection completed after " + (endTime - startTime) / 1000000 + " milliseconds.");
            }
            catch (Exception e) {
                logger.info("Processing of ontology: No name available.");
            }
            if (!tBoxInc.isEmpty()){
                logger.info("******* Found T-Box inconsistency: " + tBoxInc);
                /* Write inconsistency explanation to corresponding files */
                try {
                    File dir = new File("inconsistencies");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File("./inconsistencies/ontology_" + my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/")[my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/").length - 1].replace("]","") + "_tBox_inconsistency.txt");
                    if (!f.exists()) f.createNewFile();
                    FileWriter myWriter = new FileWriter("./inconsistencies/ontology_" + my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/")[my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/").length - 1].replace("]","") + "_tBox_inconsistency.txt");
                    myWriter.write("Ontology |> "+my_ont.getOntology().getOntologyID().getOntologyIRI().get().toString()+" <| has T-Box inconsistency.. \n Explanation:" + tBoxInc);
                    myWriter.close();
                } catch (IOException e) {
                    logger.severe("Could not write to file: " + "./inconsistencies/ontology_" + my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/")[my_ont.getOntology().getOntologyID().getOntologyIRI().toString().split("/").length - 1].replace("]","") + "_tBox_inconsistency.txt\n" + e.getMessage() + "\n" + e);
                    e.printStackTrace();
                }
            }
            else {
                logger.info("No T-Box inconsistency found...");
            }
        }

         /* Fixing initialization */

//        boolean aBoxConsistent = false;
//        AtomicBoolean pRepairable = new AtomicBoolean(true);
//        boolean longRangeInconsistencies = false;

        Random rndm = new Random();
         /*
        // TODO: tmp code, Add random immutale axioms
        Set<OWLAxiom> axioms = my_ont.getOntology().getABoxAxioms(Imports.INCLUDED);
        for (OWLAxiom ax : axioms){
            if(rndm.nextBoolean()){
                pt.addImmutableAxiom(ax);
            }
        }
        pt.printImmutables();
        //*/

        /*
        OWLObjectPropertyAssertionAxiom a_random_opa = (OWLObjectPropertyAssertionAxiom)my_ont.getOntology().axioms(AxiomType.OBJECT_PROPERTY_ASSERTION).toArray()[0];
        logger.info("A random OWLObjectPropertyAssertionAxiom:" + a_random_opa.toString()
                +"\n\t classes" + a_random_opa.getClassesInSignature().toString()
                +"\n\t individuals" + a_random_opa.getIndividualsInSignature().toString()
                +"\n\t anonymous_individuals" + a_random_opa.getAnonymousIndividuals().toString()
                +"\n\t datatypes" + a_random_opa.getDatatypesInSignature().toString()
                +"\n\t Signature" + a_random_opa.getSubject().toString()
        );
        System.exit(1);
        // */


        /* TODO: Remove comment here for the final correct version */
//        PositionTracker pt = new PositionTracker();
//        Fixer fixer = new Fixer(my_ont.getOntology(), pt, c_args.debugLog);

//        pRepairable = fixer.CheckRepairability();

        long aboxnewstart = System.nanoTime();
//        while (!aBoxConsistent && pRepairable && !longRangeInconsistencies) {
        /* Check for A-Box inconsistencies */
        ConcurrentHashMap<Integer, Pair<OWLEntity, Fix>> all_fixes = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<OWLAxiom>> inconsistencyExplanations = new ConcurrentHashMap<>();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLEntity>> module_expansions = new ConcurrentHashMap<>();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> modules = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<OWLEntity> completed_modules = new ConcurrentLinkedQueue<>();
        OWLOntologyManager man_init = createOWLOntologyManager();
        OWLClass errorTimeoutEnt = man_init.getOWLDataFactory().getOWLEntity(EntityType.CLASS,IRI.create("ReasoningTimedOut!"));
        OWLAxiom errorTimeoutAx = man_init.getOWLDataFactory().getOWLDeclarationAxiom(errorTimeoutEnt);
        Set<OWLAxiom> errorTimeout = new HashSet();
        errorTimeout.add(errorTimeoutAx);
        /* If no module splitting is enabled */
        if (c_args.no) {
            // "Do not extract modules, explain the whole KG"
            Set<Set<OWLAxiom>> explanations = null;
            if (!my_ont.isConsistent()){
                explanations = my_ont.getExplanations(c_args.timeout, c_args.explanationsLimit);
            }
            if (explanations != null && explanations.contains(errorTimeout)) {
                no_of_reasoner_timeouts.incrementAndGet();
            }
            if (explanations != null && !explanations.isEmpty() && !explanations.contains(errorTimeout)) {
                explanations.forEach( e -> {
                    inconsistencyExplanations.putIfAbsent(e.toString(), e);
                });

//                no_of_inconsistency.addAndGet(explanations.size());
//            }
//            String j = my_ont.explainInconsistency();
//            if (!j.isEmpty()) {
                logger.info("******* Found inconsistency in the whole ontology: " + explanations.toString());
//                String temp_term;
//                try {
//                    temp_term = my_ont.getOntology().getOntologyID().getOntologyIRI().get().getFragment();
//                }
//                // TODO: fix proper naming
//                catch (Exception e) {
//                    temp_term = generateString();
//                }
//                /* Write inconsistency explanation to corresponding files */
//                String exp_path = "inconsistencies/" + experiment_folder;
//                String file_path = "./" + exp_path +"/"+ temp_term + "_inconsistencies.txt";
//                try {
//                    //                    if (new_iri.toString().contains("#")) {temp_term = new_iri.toString().split("#")[new_iri.toString().split("#").length-1].replace("/","-");}//.split(">")[0];}
//                    //                    else {temp_term = new_iri.toString().split("/")[new_iri.toString().split("/").length-1];}//.split(">")[0];}
//                    ////                        temp_term = new_iri.getFragment();
//                    File dir = new File("inconsistencies");
//                    if (!dir.exists()) dir.mkdirs();
//                    File expdir = new File(exp_path);
//                    if (!expdir.exists()) expdir.mkdirs();
//                    File f = new File(file_path);
//                    if (!f.exists()) f.createNewFile();
//                    FileWriter myWriter = new FileWriter(file_path);
//                    myWriter.write("Whole ontology has inconsistencies.. \n Explanation:" + explanations.toString());
//                    myWriter.close();
//                } catch (IOException e) {
//                    logger.severe("Could not write to file: " + file_path + "\n" + e.getMessage() + "\n" + e);
//                    e.printStackTrace();
//                }
                PositionTracker pt = new PositionTracker();
                Fixer fixer = new Fixer(my_ont.getOntology(), pt, c_args);

                boolean aBoxConsistent = false;
                boolean pRepairable = true;
                int progress_all = 0;
                pRepairable = fixer.CheckRepairability();
                int round = 0;
                if (c_args.fixinc) {
                    while (!aBoxConsistent && pRepairable && !overtime.get()) {
                        /* Fixing */
                        HashSet<Fix> sFixes = new HashSet<Fix>();
                        if(c_args.fixSelection==2 || c_args.fixSelection==1){// trivial-fix or greedy-fix
                            // No soundness check of the returned fixes is needed in this case. They have already been checked (if needed)
                            sFixes = fixer.ComputeSomeSoundFixes(explanations);
//                            logger.info("     ** Found a (first) sound fix: ");
                        } else { // rank-fix or random-fix (default)
                            HashSet<Fix> fixes = fixer.ComputeFixes(explanations);
//                            logger.info("     ** Found fixes: ");
                            Fixer.printFixes(fixes); // Global sound fixes, as the fixer considers the whole ontology
//                            logger.info("     ** Sound fixes: ");
                            sFixes = fixer.KeepSoundFixes(fixes);

                            long timeSoFar = (System.nanoTime()-totalExecTimeStart) / 1000000;
//                        System.out.println("overtime.get() "+overtime.get());
//                        System.out.println("timeSoFar "+(timeSoFar));
//                        System.out.println("fixingTimeout "+(c_args.fixingTimeout));

                            if(timeSoFar >= c_args.fixingTimeout){
                                overtime.set(true);
//                            System.out.println("overtime.get() "+overtime.get());
                            }
                        }
                        if(!overtime.get()) {
                            Fixer.printFixes(sFixes);
                            // Choose and apply a fix
                            Fix chosenFix = null;
                            if (!sFixes.isEmpty()) {
                                // Get a random fix
                                int rndmNumber = rndm.nextInt(sFixes.size());
                                // iterate the HashSet
                                Iterator<Fix> it = sFixes.iterator();
                                int currentIndex = 0;
                                while (it.hasNext()) {
                                    Fix tmpFix = it.next();
                                    // if current index is equal to random number
                                    if (currentIndex == rndmNumber)
                                        chosenFix = tmpFix;
                                    // increase the current index
                                    currentIndex++;
                                }
                                // Apply the chosen fix
                                if (chosenFix != null) {
                                    fixer.applyFix(chosenFix);
                                }
                                Pair<OWLEntity, Fix> pairOfChosenFix = new Pair(my_ont.getOntologyIndividuals().findFirst(), chosenFix);
                                all_fixes.putIfAbsent(++progress_all, pairOfChosenFix);
                               // logger.info("******* Apply Sound fix: " + chosenFix);

//                            /* Write fixes to corresponding files */
//                            String inconcistensy_code = "Fix_" + my_ont.getOntologyIndividuals().findFirst().get().getIRI().getFragment() + "_round_" + round;
//                            round++;
//                            file_path = "./" + exp_path +"/"+ inconcistensy_code + ".txt";
//                            try {
//                                File dir = new File("inconsistencies");
//                                if (!dir.exists()) dir.mkdirs();
//                                File exp_dir = new File(exp_path);
//                                if (!exp_dir.exists()) exp_dir.mkdirs();
//                                File f = new File(file_path);
//                                if (!f.exists()) f.createNewFile();
//                                FileWriter myWriter = new FileWriter(file_path);
//                                myWriter.write("\n Inconsistency |> " + inconcistensy_code + " <| \n Explanation:" + explanations.toString());
////                                myWriter.write("\nNumber of alternative fixes :" + fixes.size());
//                                myWriter.write("\nNumber of alternative sound fixes :" + sFixes.size());
//                                myWriter.write("\nChosen sound fix :" + chosenFix);
//                                myWriter.write("\nImmutable axioms in whole ontology after fixing:" + pt + "\n ");
//                                myWriter.close();
//                            } catch (IOException e) {
//                                logger.severe("Could not write to file: " + file_path +"\n" + e.getMessage() + "\n" + e);
//                                e.printStackTrace();
//                            }

                        } else {
                            logger.info("******* No Sound fixes available!");
                        }
                        if (fixer.CheckConsistency()) {
                            logger.info("++++++++++++++++ The Ontology is Consistent! ");
                            aBoxConsistent = true;
                            ends_up_consistent = true;
                        } else {
                            //                                    logger.info("++++++++++++++++ Inconsistency cannot be fixed by the current version");
                            pRepairable = fixer.CheckRepairability();
                            if (pRepairable) {
                                logger.info("++++++++++++++++ The Ontology is Inconsistent but P-repairable! ");
                                explanations = my_ont.getExplanations(c_args.timeout, c_args.explanationsLimit);
                            } else {
                                logger.info("++++++++++++++++ The Ontology is Inconsistent and Not P-repairable! ");
                            }
                        }
                        //Update overtime for fixing time-out
                        long timeSoFar = (System.nanoTime()-totalExecTimeStart) / 1000000;
//                        System.out.println("overtime.get() "+overtime.get());
//                        System.out.println("timeSoFar "+(timeSoFar));
//                        System.out.println("fixingTimeout "+(c_args.fixingTimeout));

                            if (timeSoFar >= c_args.fixingTimeout) {
                                overtime.set(true);
                                //                            System.out.println("overtime.get() "+overtime.get());
                            }
                        }

                    }
                    no_of_fixes.addAndGet(all_fixes.size());
                    no_of_applied_fixes.addAndGet(all_fixes.size());
                }
                fixingRounds.set(round);
                fixGenerationTime.set(Fixer.getFixGenerationTime());
                fixFilteringTime.set(Fixer.getFixFilteringTime());
                repairabilityCheckTime.set(Fixer.getRepairabilityCheckTime());
                inconsistencyCountingTime.set(Fixer.getInconsistencyCountingTime());
            } else {
                logger.info("No A-Box inconsistency found...");
                long totalExecTimeFinish = System.nanoTime();
                logger.info("++++++++++++++++ Finished! ");
                logger.info("++++++++++++++++ Total execution time: " + (totalExecTimeFinish - totalExecTimeStart) / 1000000 + " milliseconds.");
            }
        }

        /* If module splitting is enabled */
        else {
            /* Log some info */
            if (my_ont.getOntologyIndividuals().toArray().length >= 2) {
                logger.info("Delegating " + my_ont.getOntologyIndividuals().toArray().length + " module extraction tasks for parallel execution");
            } else {
                logger.info("Parallelism is not required for module extraction... \n\t\tNo. of tasks: " + my_ont.getOntologyIndividuals().toArray().length);
            }

            /* Decompose into modules for each individual in the original ontology (SHORT VERSION) */
            modules = my_ont.getABoxModulesConcurrentHashMap();
//                System.out.println(modules.size());
            int modulesCount = modules.size();
            no_of_modules = modulesCount;
            AtomicInteger progress2 = new AtomicInteger();
            AtomicInteger progress_all = new AtomicInteger();
            List<OWLEntity> leads = new ArrayList<>();
            if (c_args.extend) {
                /* Extend individual modules with more axioms to account for
                   more inconsistency types */
                /* leads contain object properties */
                leads = discoverLeadsForOPExt(my_ont.getTBoxOnly());
            }
            List<OWLEntity> finalLeads = leads;


            /* For each module, perform in parallel (parallelismThreshold should be 1) */
            ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> finalModules = new ConcurrentHashMap<>(modules);
//            modules.forEach(1, (k, v) -> {
            modules.forEach(1, (k, v) -> {
                OWLOntologyManager man_t = createOWLOntologyManager();
                IRI new_iri = null;
                try {
                    new_iri = k.getIRI(); //IRI.create(my_ont.getOntology().getOntologyID().getOntologyIRI().get() + "/module/" + k);
                } catch (Exception e) {
                    new_iri = IRI.create("http://moduleOf.ont/" + k);
                }
                try {
                    progress2.set(progress2.get() + 1);
//                    logger.info("######## " + progress2.get() + "/" + modulesCount + " Module |> " + k + " <| ########");

                    /* Initialize OWLOntology to include module's A-Box */
                    OWLOntology all_boxes = man_t.createOntology(v.stream(), new_iri);
                    /* Include the T-Box */
                    my_ont.getTBoxOnly().getAxioms().forEach(all_boxes::addAxiom);

                    if (c_args.debugLog){
//                        System.out.println(k.toStringID());
                        all_boxes.getAxioms().forEach(System.out::println);
                    }

                    if (c_args.extend){
                        /* Extend individual modules with more axioms to account for
                           more inconsistency types */
                        List<OWLEntity> inds_to_include = expandAdditionalIndividuals(all_boxes, k, finalLeads);
                        if (!inds_to_include.isEmpty()){
//                            System.out.println("Found " + inds_to_include.size() + " individuals to expand");
                            inds_to_include.forEach(ii->{
                                ConcurrentLinkedQueue<OWLAxiom> exts = finalModules.get(ii);
                                exts.forEach(all_boxes::addAxiom);
                                /* remember expansions to exclude fixes later */
                                ConcurrentLinkedQueue<OWLEntity> temp = module_expansions.computeIfPresent(ii, (k1,v1) -> v1);
                                if(temp != null){
                                    if(!temp.contains(ii)) {
                                        temp.add(ii);
                                        module_expansions.putIfAbsent(k, temp);
                                    }
                                }
                                else {
                                    ConcurrentLinkedQueue<OWLEntity> temp1 = new ConcurrentLinkedQueue<>();
                                    temp1.add(ii);
                                    module_expansions.putIfAbsent(k, temp1);
                                }
//                                System.out.println("module expansions size: " + module_expansions.size());
                                /**/
                            });
                        }
                        /**/
                    }
                    //System.out.println("\n\n\n\n\nMODULE EXPANSIONS");
                    //System.out.println(module_expansions.toString());
                    AtomicBoolean skipThis = new AtomicBoolean(false);
//                    module_expansions.forEach( (key,val) -> {
//                        if (val.contains(k) && key!=k && completed_modules.contains(key)){
//                            logger.info("%%%%%%% Module of " + k
//                                    + " is skipped, since it has been included in the module of "
//                                    + key + "\nExpansions of the latter: " + module_expansions.get(key)
//                                    + "\nReasoning on the latter is completed: " + completed_modules.contains(key));
//                            skipThis.set(true);
//                        }
//                    } );

                    for (OWLEntity key : module_expansions.keySet()) {
                        if (module_expansions.get(key).contains(k) && key != k && completed_modules.contains(key)) {
//                            logger.info("%%%%%%% Module of " + k
//                                    + " is skipped, since it has been included in the module of "
//                                    + key + "\nExpansions of the latter: " + module_expansions.get(key)
//                                    + "\nReasoning on the latter is completed: " + completed_modules.contains(key));
                            skipThis.set(true);
                            break;
                        }
                    }

                    //System.out.println("\n\n\n\n\n");
                    if (!skipThis.get()) {
                        OntBreakdown temp_br = new OntBreakdown(all_boxes, c_args.debugLog);
//                        logger.info("Module size: " + all_boxes.getAxioms().size());
                        module_sizes.add(all_boxes.getAxioms().size());
//                        System.out.println(module_sizes.stream().max(Integer::compare).get());
                        /* Explain inconsistencies (if any) */
                        long startTime1 = System.nanoTime();
//                    String j = temp_br.explainInconsistency();
                        Set<Set<OWLAxiom>> explanations = null;
                        temp_br.save("temp_module_ontology.ttl");
                        if (!temp_br.isConsistent()) {
                            explanations = temp_br.getExplanations(c_args.timeout, c_args.explanationsLimit);
                        }
                        if (explanations != null && explanations.contains(errorTimeout)) {
                            no_of_reasoner_timeouts.incrementAndGet();
                        }
                        if (explanations != null && !explanations.isEmpty() && !explanations.contains(errorTimeout)) {
                            explanations.forEach( e -> {

                                List mylist = e.stream().map(ax -> ax.toString()).sorted().collect(Collectors.toList());
                                logger.info("\n\n" + mylist + "\n\n");
                                inconsistencyExplanations.putIfAbsent(mylist.toString(), e);
                            });
//                            inconsistencyExplanations.add(explanations);
//                            inconsistencyExplanations.putIfAbsent(explanations.toString(), explanations);
//                            // Tackled this, by using SkipListSet TODO: increment this only if current
                            //  explanations do not exist in the global registry
                            //no_of_inconsistency.addAndGet(explanations.size());
                            //                    }
                            long endTime1 = System.nanoTime();
//                            logger.info("Processing of module " + new_iri + " completed after " + (endTime1 - startTime1) / 1000000 + " milliseconds.");

                            /* If inconsistency is found, store explanations to files */
                            //                    if (!j.isEmpty()) {

                            /* Write inconsistency explanation to corresponding files */
//                            String exp_path = "inconsistencies/" + experiment_folder;
//                            String file_path = "./" + exp_path +"/"+ new_iri.getFragment() + "_inconsistencies.txt";
//                            try {
//                                //                    if (new_iri.toString().contains("#")) {temp_term = new_iri.toString().split("#")[new_iri.toString().split("#").length-1].replace("/","-");}//.split(">")[0];}
//                                //                    else {temp_term = new_iri.toString().split("/")[new_iri.toString().split("/").length-1];}//.split(">")[0];}
//                                ////                        temp_term = new_iri.getFragment();
//                                File dir = new File("inconsistencies");
//                                if (!dir.exists()) dir.mkdirs();
//                                File expdir = new File(exp_path);
//                                if (!expdir.exists()) expdir.mkdirs();
//                                File f = new File(file_path);
//                                if (!f.exists()) f.createNewFile();
//                                FileWriter myWriter = new FileWriter(file_path);
//                                myWriter.write("Module of |> " + k + " <| has inconsistencies.. \n Explanation:" + explanations.toString());
//                                myWriter.close();
//                            } catch (IOException e) {
//                                logger.severe("Could not write to file: " + file_path + "\n" + e.getMessage() + "\n" + e);
//                                e.printStackTrace();
//                            }


                            if (c_args.fixinc) {
                                /* Initialize Fixing */
                                PositionTracker pt = new PositionTracker();
                                Fixer fixer = new Fixer(all_boxes, pt, c_args);

                                boolean aBoxConsistent = false;
                                boolean pRepairable = true;
                                pRepairable = fixer.CheckRepairability();
                                int round = 0;
                                while (!aBoxConsistent && pRepairable && !overtime.get()) {
                                    /* Fixing */
                                    HashSet<Fix> sFixes = new HashSet<Fix>();
                                    if (c_args.fixSelection==2 || c_args.fixSelection==1) {// trivial-fix or greedy-fix
                                        // No soundness check of the returned fixes is needed in this case. They have already been checked (if needed)
                                        sFixes = fixer.ComputeSomeSoundFixes(explanations);
//                                        logger.info("     ** Found a (first) sound fix: ");
                                    } else { // rank-fix or random-fix (default)
                                        HashSet<Fix> fixes = fixer.ComputeFixes(explanations);
//                                        logger.info("     ** Found fixes: ");
                                        Fixer.printFixes(fixes); // Global sound fixes, as the fixer considers the whole ontology
//                                        logger.info("     ** Sound fixes: ");
                                        sFixes = fixer.KeepSoundFixes(fixes);
                                    }
                                    Fixer.printFixes(sFixes);
                                    // Choose and apply a fix
                                    Fix chosenFix = null;
                                    if (!sFixes.isEmpty()) {
                                        // Get a random fix
                                        int rndmNumber = rndm.nextInt(sFixes.size());
                                        // iterate the HashSet
                                        Iterator<Fix> it = sFixes.iterator();
                                        int currentIndex = 0;
                                        while (it.hasNext()) {
                                            Fix tmpFix = it.next();
                                            // if current index is equal to random number
                                            if (currentIndex == rndmNumber)
                                                chosenFix = tmpFix;
                                            // increase the current index
                                            currentIndex++;
                                        }
                                        // Apply the chosen fix
                                        if (chosenFix != null) {
                                            fixer.applyFix(chosenFix);
                                        }
                                        Pair<OWLEntity, Fix> pairOfChosenFix = new Pair(k, chosenFix);
                                        all_fixes.putIfAbsent(progress_all.incrementAndGet(), pairOfChosenFix);
                                        logger.info("******* Apply Sound fix: " + chosenFix);

//                                        /* Write fixes to corresponding files */
//                                        String inconcistensy_code = "Fix_" + new_iri.getFragment() + "_round_" + round;
//                                        file_path = "./" + exp_path +"/"+ inconcistensy_code + ".txt";
//                                        try {
//                                            File dir = new File("inconsistencies");
//                                            if (!dir.exists()) dir.mkdirs();
//                                            File exp_dir = new File(exp_path);
//                                            if (!exp_dir.exists()) exp_dir.mkdirs();
//                                            File f = new File(file_path);
//                                            if (!f.exists()) f.createNewFile();
//                                            FileWriter myWriter = new FileWriter(file_path);
//                                            myWriter.write("\n Inconsistency |> " + inconcistensy_code + " <| \n Explanation:" + explanations.toString());
////                                myWriter.write("\nNumber of alternative fixes :" + fixes.size());
//                                            myWriter.write("\nNumber of alternative sound fixes :" + sFixes.size());
//                                            myWriter.write("\nChosen sound fix :" + chosenFix);
//                                            myWriter.write("\nImmutable axioms in whole ontology after fixing:" + pt + "\n ");
//                                            myWriter.close();
//                                        } catch (IOException e) {
//                                            logger.severe("Could not write to file: " + file_path +"\n" + e.getMessage() + "\n" + e);
//                                            e.printStackTrace();
//                                        }
                                    } else {
                                        logger.info("******* No Sound fixes available!");
                                    }
                                    if (fixer.CheckConsistency()) {
                                        logger.info("++++++++++++++++ The module is Consistent! ");
                                        aBoxConsistent = true;
                                    } else {
                                        pRepairable = fixer.CheckRepairability();
                                        if (pRepairable) {
                                            logger.info("++++++++++++++++ The module is Inconsistent but P-repairable! ");
                                            explanations = temp_br.getExplanations(c_args.timeout, c_args.explanationsLimit);
                                        } else {
                                            logger.info("++++++++++++++++ The module is Inconsistent and Not P-repairable! ");
                                        }
                                    }
                                    //Update overtime for fixing time-out
                                    long timeSoFar = (System.nanoTime()-totalExecTimeStart) / 1000000;
                                    if(timeSoFar >= c_args.fixingTimeout){
                                        overtime.set(true);
                                    }
                                }
                                no_of_fixes.addAndGet(all_fixes.size());
                                fixingRounds.addAndGet(round);
                                fixGenerationTime.addAndGet(Fixer.getFixGenerationTime());
                                fixFilteringTime.addAndGet(Fixer.getFixFilteringTime());
                                repairabilityCheckTime.addAndGet(Fixer.getRepairabilityCheckTime());
                                inconsistencyCountingTime.addAndGet(Fixer.getInconsistencyCountingTime());
                            }
                        }
                        completed_modules.add(k);
                    }
//                    else{
//                        logger.info("Module of " + k + " has been skipped..");
//                    }

                } catch (OWLOntologyCreationException e) {
                    e.printStackTrace();
                }
            });
        }

        /* Repair A-Box inconsistencies */
        logger.info("++++++++++++++++ Ontology Abox (Before fixing): " + my_ont.getOntologyABox().collect(Collectors.toList()).size());

        logger.info("******* All inconsistencies found " + inconsistencyExplanations.size());

        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> no_dup_modules = new ConcurrentHashMap<>(modules);
        if (c_args.extend) {
            /* Determine which individuals have been expanded to other modules
            thus we should not take them into account, as their fixes will
            constitute unwanted duplicates that could potentially harm
            overall consistency at the end of the process.
            */
            List<OWLEntity> avoid = new ArrayList<>();
            module_expansions.forEach((k, v) -> {
                v.forEach(exps -> {
                    if (((no_dup_modules.get(exps) != null) && (k!=exps)) && (!avoid.contains(exps))) {
//                        System.out.println("Removing and avoiding "+ exps);
//                        System.out.println((no_dup_modules.get(exps) != null));
//                        System.out.println((k!=exps));
//                        System.out.println((!avoid.contains(exps)));
                        no_dup_modules.remove(k);
                        avoid.add(exps);
//                        System.out.println(avoid);
//                        System.out.println(!avoid.contains(exps));
                    }
                });
            });
            /*
            System.out.println(module_expansions.size() + " " + no_dup_modules.size());
            System.out.println(module_expansions);
            System.out.println(no_dup_modules);
            */
        }


        if (c_args.fixinc) {
            if(overtime.get()){
                logger.info("++++++++++++++++ Fixing timed out!");
            } else {
                PositionTracker pt = new PositionTracker();
                Fixer fixer = new Fixer(my_ont.getOntology(), pt, c_args);
                all_fixes.forEach((k, v) -> {
                    /* Exclude fixes from duplicate module expansions */
                    System.out.print("FIX: ");
                    System.out.println(k + " " + v);
                    if (no_dup_modules.get(v.getValue0()) != null) {
                        if (fixer.applyFix(v.getValue1())) {
                            System.out.println("APPLIED!");
                            no_of_applied_fixes.incrementAndGet();
                        } else {
                            System.out.println("internally SKIPPED!");
                        }
                    } else {
                        System.out.println("SKIPPED!");
                    }
                });
                long CheckConsistencyStart = System.nanoTime();
                if (fixer.CheckConsistency()) {
                    //TODO: If module splitting is enabled, it may be faster to get the explanations (in paralel) then using CheckConsistency().
                    logger.info("++++++++++++++++ After fixing: The ontology is Consistent! ");
                    ends_up_consistent = true;
                    //            aBoxConsistent = true;
                } else {
                    logger.info("++++++++++++++++ After fixing: Inconsistency could not be fixed.");

                }
                CheckConsistencyTime = (System.nanoTime() - CheckConsistencyStart)/1000000;
            }
        }

        long aboxnewend = System.nanoTime();
        a_box_check_time = (aboxnewend - aboxnewstart)/1000000;
        logger.info("++++++++++++++++ ABox modules new total execution time: " + (aboxnewend - aboxnewstart)/1000000 + " milliseconds.");

        // TODO generalize for other file types (other than .ttl)
        //my_ont.save(c_args.output + c_args.aBox.replace(".ttl","_fixed.ttl"));
        if(!c_args.output.isEmpty()){
            my_ont.save(c_args.output);
        }

        long totalExecTimeFinish = System.nanoTime();
        total_exec_time = (totalExecTimeFinish - totalExecTimeStart)/1000000;
        logger.info("++++++++++++++++ Finished! ");
        logger.info("++++++++++++++++ Total execution time: " + (totalExecTimeFinish - totalExecTimeStart)/1000000 + " milliseconds.");

        /* Create results .csv, or append if exists */
        String savestr = "results.csv";
        File f = new File(savestr);

        PrintWriter out = null;
        if ( f.exists() && !f.isDirectory() ) {
            out = new PrintWriter(new FileOutputStream(new File(savestr), true));
        }
        else {
            out = new PrintWriter(savestr);
            out.append("T-Box file,A-Box file,A-Box size," +
                    "T-Box check time,A-Box check time,Total exec time,Fix calc. time,Fix filt. time,Check rep. time, Inc. count time, Final cons. Check time," +
                    "No. of modules,Avg. module size,STD of module sizes,No. of inconsistency,Total No. of fixes,No. of applied fixes,No. of fixing rounds," +
                    "No. of Reasoner Timeouts,Ends up consistent?,Module Split?,Extend Modules?,MCD fix?,Fix selection (1:trivial; 2:greedy; 3:rank; default:Random), " +
                    "fixing Timeout, fixing overtime , experiment_folder\n");
        }
        out.append( c_args.tBox + "," +
                    c_args.aBox + "," +
                    my_ont.getOntologyABox().toArray().length + "," +
                    t_box_check_time + "," +
                    a_box_check_time + "," +
                    total_exec_time + "," +
                    fixGenerationTime.get() + "," +
                    fixFilteringTime.get() + "," +
                    repairabilityCheckTime + "," +
                    inconsistencyCountingTime + "," +
                    CheckConsistencyTime + "," +
                    no_of_modules + "," +
                    getAverageValue(module_sizes) + "," +
                    getStdValue(module_sizes) + "," +
                    inconsistencyExplanations.size() + "," +
                    no_of_fixes.get() + "," +
                    no_of_applied_fixes.get() + "," +
                    fixingRounds + "," +
                    no_of_reasoner_timeouts + "," +
                    ends_up_consistent + "," +
                    !c_args.no + "," +
                    c_args.extend + "," +
                    c_args.mcd + "," +
                    c_args.fixSelection + "," +
                    c_args.fixingTimeout + "," +
                    overtime.get() + "," +
                    experiment_folder + "," +
                    "\n");
        out.close();


//        System.out.println("++++++++++++++++ ABox modules first total execution time: " + (aboxfirstend - aboxfirststart)/1000000 + " milliseconds.");

//        long t_box_check_time = 0;
//        long a_box_check_time = 0;
//        long  total_exec_time = 0;
//        List<Integer> module_sizes = new ArrayList<>();
//        int no_of_inconsistency = 0;
//        int no_of_fixes = 0;
//        boolean ends_up_consistent = false;
    }


    private static List<OWLEntity> discoverLeadsForOPExt(OWLOntology module){
        List<OWLEntity> leads = new ArrayList<>();
        module.getAxioms().forEach(a -> {
            a.getNestedClassExpressions().forEach( b -> {
                if ((b.getClassExpressionType()==ClassExpressionType.OBJECT_ALL_VALUES_FROM)  ||
                        (b.getClassExpressionType()==ClassExpressionType.OBJECT_SOME_VALUES_FROM) ||
                        (b.getClassExpressionType()==ClassExpressionType.DATA_ALL_VALUES_FROM) ||
                        (b.getClassExpressionType()==ClassExpressionType.DATA_SOME_VALUES_FROM)
                ) {
                    a.getObjectPropertiesInSignature().forEach(c -> {
                        if (!leads.contains(c)){
                            leads.add(c);
                            // TODO sameIndividual
                            // TODO any others?
                            // TODO ? Is this object property found in any "inverse" axioms?
                        }
                    });
                }
            });
        });
        return leads;
    }



    private static List<OWLEntity> expandAdditionalIndividuals(OWLOntology module, OWLEntity base, List<OWLEntity> leads) {
        List<OWLEntity> extensions = new ArrayList<>();
//        OWLDataFactory df = new OWLDataFactoryImpl();
        module.getAxioms().forEach(k -> {
            leads.forEach(p -> {
                if (k.containsEntityInSignature(p)){
                    if (k.getIndividualsInSignature().contains(base)) {
                        k.getIndividualsInSignature().forEach(z -> {
                            if (z.compareTo(base) != 0) {
//                                logger.info("Based on " + p + ":\n In module of: " + base + " added " + z + " because " + k + " exists in module");
                                extensions.add(z);
                            }
                        });
                    }
                }});
            });
        return extensions;
    }

    public static String generateString()
    {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return "random-" + generatedString;
    }

    public static double getAverageValue(List<Integer> my_list){
        double averageValue = 0;
        double sum = 0;
        try{
            if(my_list.size() > 0){
                for (Integer integer : my_list) {
                    sum += integer;
                }
                averageValue = (sum / (double)my_list.size());
            }
            return averageValue;
        }
        catch (NullPointerException e){
            return -1;
        }
    }

    public static double getStdValue(List<Integer> my_list){
        double averageValue = 0;
        double sum = 0;
        try {
            if (my_list.size() > 0) {
                for (Integer integer : my_list) {
                    sum += integer;
                }
                averageValue = (sum / (double) my_list.size());
            }
            double standardDeviation = 0.0;
            for (Integer integer : my_list) {
                standardDeviation += Math.pow(integer - averageValue, 2);
            }
            return Math.sqrt(standardDeviation / (double) my_list.size());
        }
        catch (NullPointerException e){
            return -1;
        }
    }

}

