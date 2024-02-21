# kg-fixing
A Java framework for detecting and fixing formal inconsistency in Knowledge Graphs

## Description
This repository is developed and maintained by the Complex Event Recognition ([CER](https://cer.iit.demokritos.gr/)) Group, Institute of Informatics & Telecommunications, National Centre for Scientific Research 'Demokritos'.
It has been implemented in the scope of the HORIZON Europe RIA project [ENEXA](http://enexa.eu) - Efficient Explainable Learning on Knowledge Graphs (GA ID: 101070305).

The module takes as input a Knowledge Graph (i.e. a T-Box and an A-Box file) and checks it for formal inconsistency. 
If inconsistencies are present, then the corresponding explanations are generated and targeted fixes are applied to make the KG consistent.

## Usage: 

```  Options:
    -h, --help

    -c, --check-t-box
      Check also for T-Box inconsistencies
      Default: false
    -d, --debug-logs
      Log debug messages
      Default: false
  * -t, --tbox
      File with the T-Box of the KG
      Default: <empty string>
    -a, --abox
      Comma-separated list of A-Box files of the KG
      Default: <empty string>
    -n, --no-extraction
      Do not extract modules, explain the whole KG
      Default: false
    -s, --save-ontology
      Save merged ontology to file
      Default: false
    -l, --logfilename
      Name of the log file
      Default: ExtractModules.log
    -o, --output-path
      Path to save the fixed KG file
      Default: <empty string>
    -e, --extend-module
      Extend modules to account for more inconsistency types
      Default: false
    -x, --fix-inconsistency
      Apply A-Box fixing method
      Default: false
    -mcd, --mcd-position
      Apply A-Box fixing based on Maximally ContaineD positions only 
      (mcd).Default: All joint positions are considered (opti-joint).
      Default: false
    -fixS, --fix-selection
      Strategy for selecting fixes.
            -1: trivial-fix. Apply (trivial) A-Box fixing using only new Individuals.
            -2: greedy-fix.  Apply A-Box fixing calculating only the first (non-trivial) sound fix.
            -3: rank-fix. Apply A-Box fix ranking based on number of explanations
            -other: random-fix. Calculate all sound fixes and select a random one.
      Default: 0
    -to, --reasoner-timeout
      Timeout for the explanations retrieval task in milliseconds (default: 
      24hours) 
      Default: 86400000
    -ft, --fixing-timeout
      Timeout for the fixing task in milliseconds (default: 24hours)
      Default: 86400000
    -explim, --explanations-limit
      Maximum number of explanations to retrieve (default: Integer.MAX_VALUE)
      Default: Integer.MAX_VALUE
 ```
 
 ## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[GPL-3.0 license](https://www.gnu.org/licenses/gpl-3.0.html)
