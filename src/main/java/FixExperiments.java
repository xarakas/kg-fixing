import com.beust.jcommander.JCommander;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class FixExperiments {
    static Logger logger = Logger.getLogger(ExtractModules.class.getName());

    public static void main(String[] args) {
        for(int i=0; i<args.length; i++ )
            System.out.println(args[i]);

        ArrayList<String> tbox = new  ArrayList<>();
        tbox.add("example-KG/LUBM_with_disj.nt");

        ArrayList<String> abox = new  ArrayList<>();
        abox.add("example-KG/u1conf0025.nt");
        abox.add("example-KG/u1conf005.nt");
        abox.add("example-KG/u1conf025.nt");
        abox.add("example-KG/u1conf05.nt");
//        abox.add("example-KG/u1conf01.nt");
//        abox.add("example-KG/u1conf1.nt");

//        ArrayList<String> o = new  ArrayList<>();
//        o.add("TESTING31-10.owl");

//        String ft = "360000"; // 6 mins
        String ft = "60000"; // 1 min

        ArrayList<String> fixs = new  ArrayList<>();
        fixs.add("1");
        fixs.add("3");
        fixs.add("2");
        fixs.add("0");

        ArrayList<String[]> argList = new  ArrayList<>();
//      Number of repetitions for each experiment
        int repetitions = 5;
//      Experiment with modules
        boolean modules = true;
        for(String t : tbox) {
            for(String a : abox) {
                for(String s : fixs) {
                    // A) No module splitting,
                    // A.1) MCD position
                    String expMCDArgs[] = new String[]{ "-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s,
                            "-n", "-mcd"};
                    // A.2) Random position
                    String expArgs[] = new String[]{"-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s,
                            "-n"};
                    // B) Simple module splitting,
                    // B.1) MCD position
                    String mMCDArgs[] = new String[]{ "-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s,
                            "-mcd"};
                    // B.2) Random position
                    String mArgs[] = new String[]{"-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s};
                    // C) Extended module splitting,
                    // C.1) MCD position
                    String emMCDArgs[] = new String[]{ "-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s,
                            "-e", "-mcd"};
                    // C.2) Random position
                    String emArgs[] = new String[]{"-t", t, "-a", a, "-x", "-ft", ft, "-fixS", s,
                            "-e"};
                    for(int i = 0; i < repetitions; i++){
                        argList.add(expMCDArgs);
                        argList.add(expArgs);
                        if(modules){
                            argList.add(mMCDArgs);
                            argList.add(mArgs);
                            argList.add(emArgs);
                            argList.add(emMCDArgs);
                        }
                    }
                }
            }
        }
        logger.info("\n Run " + argList.size() + " experiments\n");

//        Run each of the experiments
        for(String[] ea : argList) {
            List eas = Arrays.asList(ea);
//            System.out.println(eas);
            try {
                ExtractModules.main(ea);
                Fixer.clearTimes(); // Clears the times aggregated from previous experiments
            } catch (OWLOntologyCreationException e) {
                logger.info("\n Experiment failed due to OWLOntologyCreationException. \n" +
                        "\t args: " + eas + " \n");
                e.printStackTrace();
            } catch (IOException e) {
                logger.info("\n Experiment failed due to IOException. \n" +
                        "\t args: " + eas + " \n");
                e.printStackTrace();
            }
        }
    }
}
