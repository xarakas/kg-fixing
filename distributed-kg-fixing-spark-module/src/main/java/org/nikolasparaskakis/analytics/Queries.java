package org.nikolasparaskakis.analytics;



import org.nikolasparaskakis.logs.bin.BinInnerRoundNumLog;
import org.nikolasparaskakis.logs.bin.BinReasoningLog;
import org.nikolasparaskakis.logs.bin.BinSizeLog;
import org.nikolasparaskakis.logs.module.ModuleInnerRoundNumLog;
import org.nikolasparaskakis.logs.module.ModuleReasoningLog;
import org.nikolasparaskakis.logs.module.ModuleSizeLog;



/**
 * A class that contains all the queries used in the analytics.
 */
public class Queries {

    public int limitInTop;
    public String moduleSizeLogsTable;
    public String binSizeLogsTable;
    public String moduleReasoningLogsTable;
    public String binReasoningLogsTable;
    public String moduleInnerRoundNumLogsTable;
    public String binInnerRoundNumLogsTable;

    public Queries(int limitInTop, String moduleSizeLogsTable, String binSizeLogsTable, String moduleReasoningLogsTable, String binReasoningLogsTable, String moduleInnerRoundNumLogsTable, String binInnerRoundNumLogsTable) {
        this.limitInTop = limitInTop;
        this.moduleSizeLogsTable = moduleSizeLogsTable;
        this.binSizeLogsTable = binSizeLogsTable;
        this.moduleReasoningLogsTable = moduleReasoningLogsTable;
        this.binReasoningLogsTable = binReasoningLogsTable;
        this.moduleInnerRoundNumLogsTable = moduleInnerRoundNumLogsTable;
        this.binInnerRoundNumLogsTable = binInnerRoundNumLogsTable;
    }

    public String getTotalIndividualsQuery() {
        return "SELECT " +
                "COUNT(*) as totalIndividuals " +
                "FROM " + this.moduleSizeLogsTable;
    }

    public String getTotalBinsQuery() {
        return "SELECT " +
                "COUNT(*) as totalBins " +
                "FROM " + this.binSizeLogsTable;
    }

    public String getModuleSizeQuery() {
        return "SELECT " +
                ModuleSizeLog.getOuterRoundVarName() + " as outerRound, " +
                "ROUND(AVG( " + ModuleSizeLog.getModuleSizeVarName() + " ), 2) as avgModuleSize, " +
                "ROUND(STDDEV( " + ModuleSizeLog.getModuleSizeVarName() + " ), 2) as stdModuleSize, " +
                "MAX( " + ModuleSizeLog.getModuleSizeVarName() + " ) as maxModuleSize " +
                "FROM " + this.moduleSizeLogsTable + " " +
                "GROUP BY outerRound " +
                "ORDER BY outerRound";
    }

    public String getBinSizeQuery() {
        return "SELECT " +
                BinSizeLog.getOuterRoundVarName() + " as outerRound, " +
                "ROUND(AVG( " + BinSizeLog.getBinSizeVarName() + " ), 2) as avgBinSize, " +
                "ROUND(STDDEV( " + BinSizeLog.getBinSizeVarName() + " ), 2) as stdBinSize, " +
                "MAX( " + BinSizeLog.getBinSizeVarName() + " ) as maxBinSize " +
                "FROM " + this.binSizeLogsTable + " " +
                "GROUP BY outerRound " +
                "ORDER BY outerRound";
    }

    public <T> String getCheckConsistencyTimeQuery(Class<T> clazz) {
        if (clazz == ModuleReasoningLog.class) {
            return "SELECT " +
                    ModuleReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleReasoningLog.getCheckConsistencyTimeMillisVarName() + "), 2) as avgCheckConsistencyTimeMillis, " +
                    "ROUND(STDDEV(" + ModuleReasoningLog.getCheckConsistencyTimeMillisVarName() + "), 2) as stdCheckConsistencyTimeMillis, " +
                    "MAX(" + ModuleReasoningLog.getCheckConsistencyTimeMillisVarName() + ") as maxCheckConsistencyTimeMillis " +
                    "FROM " + this.moduleReasoningLogsTable + " " +
                    "WHERE " + ModuleReasoningLog.getCheckConsistencyTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinReasoningLog.class) {
            return "SELECT " +
                    BinReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinReasoningLog.getCheckConsistencyTimeMillisVarName() + "), 2) as avgCheckConsistencyTimeMillis, " +
                    "ROUND(STDDEV(" + BinReasoningLog.getCheckConsistencyTimeMillisVarName() + "), 2) as stdCheckConsistencyTimeMillis, " +
                    "MAX(" + BinReasoningLog.getCheckConsistencyTimeMillisVarName() + ") as maxCheckConsistencyTimeMillis " +
                    "FROM " + this.binReasoningLogsTable + " " +
                    "WHERE " + BinReasoningLog.getCheckConsistencyTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }

    public <T> String getCheckRepairabilityTimeQuery(Class<T> clazz) {
        if (clazz == ModuleReasoningLog.class) {
            return "SELECT " +
                    ModuleReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleReasoningLog.getCheckRepairabilityTimeMillisVarName() + "), 2) as avgCheckRepairabilityTimeMillis, " +
                    "ROUND(STDDEV(" + ModuleReasoningLog.getCheckRepairabilityTimeMillisVarName() + "), 2) as stdCheckRepairabilityTimeMillis, " +
                    "MAX(" + ModuleReasoningLog.getCheckRepairabilityTimeMillisVarName() + ") as maxCheckRepairabilityTimeMillis " +
                    "FROM " + this.moduleReasoningLogsTable + " " +
                    "WHERE " + ModuleReasoningLog.getCheckRepairabilityTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinReasoningLog.class) {
            return "SELECT " +
                    BinReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinReasoningLog.getCheckRepairabilityTimeMillisVarName() + "), 2) as avgCheckRepairabilityTimeMillis, " +
                    "ROUND(STDDEV(" + BinReasoningLog.getCheckRepairabilityTimeMillisVarName() + "), 2) as stdCheckRepairabilityTimeMillis, " +
                    "MAX(" + BinReasoningLog.getCheckRepairabilityTimeMillisVarName() + ") as maxCheckRepairabilityTimeMillis " +
                    "FROM " + this.binReasoningLogsTable + " " +
                    "WHERE " + BinReasoningLog.getCheckRepairabilityTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }


    public <T> String getGetExplanationsTimeQuery(Class<T> clazz) {
        if (clazz == ModuleReasoningLog.class) {
            return "SELECT " +
                    ModuleReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleReasoningLog.getGetExplanationsTimeMillisVarName() + "), 2) as avgGetExplanationsTimeMillis, " +
                    "ROUND(STDDEV(" + ModuleReasoningLog.getGetExplanationsTimeMillisVarName() + "), 2) as stdGetExplanationsTimeMillis, " +
                    "MAX(" + ModuleReasoningLog.getGetExplanationsTimeMillisVarName() + ") as maxGetExplanationsTimeMillis " +
                    "FROM " + this.moduleReasoningLogsTable + " " +
                    "WHERE " + ModuleReasoningLog.getGetExplanationsTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinReasoningLog.class) {
            return "SELECT " +
                    BinReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinReasoningLog.getGetExplanationsTimeMillisVarName() + "), 2) as avgGetExplanationsTimeMillis, " +
                    "ROUND(STDDEV(" + BinReasoningLog.getGetExplanationsTimeMillisVarName() + "), 2) as stdGetExplanationsTimeMillis, " +
                    "MAX(" + BinReasoningLog.getGetExplanationsTimeMillisVarName() + ") as maxGetExplanationsTimeMillis " +
                    "FROM " + this.binReasoningLogsTable + " " +
                    "WHERE " + BinReasoningLog.getGetExplanationsTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }


    public <T> String getComputeFixesTimeQuery(Class<T> clazz) {
        if (clazz == ModuleReasoningLog.class) {
            return "SELECT " +
                    ModuleReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleReasoningLog.getComputeFixesTimeMillisVarName() + "), 2) as avgComputeFixesTimeMillis, " +
                    "ROUND(STDDEV(" + ModuleReasoningLog.getComputeFixesTimeMillisVarName() + "), 2) as stdComputeFixesTimeMillis, " +
                    "MAX(" + ModuleReasoningLog.getComputeFixesTimeMillisVarName() + ") as maxComputeFixesTimeMillis " +
                    "FROM " + this.moduleReasoningLogsTable + " " +
                    "WHERE " + ModuleReasoningLog.getComputeFixesTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinReasoningLog.class) {
            return "SELECT " +
                    BinReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinReasoningLog.getComputeFixesTimeMillisVarName() + "), 2) as avgComputeFixesTimeMillis, " +
                    "ROUND(STDDEV(" + BinReasoningLog.getComputeFixesTimeMillisVarName() + "), 2) as stdComputeFixesTimeMillis, " +
                    "MAX(" + BinReasoningLog.getComputeFixesTimeMillisVarName() + ") as maxComputeFixesTimeMillis " +
                    "FROM " + this.binReasoningLogsTable + " " +
                    "WHERE " + BinReasoningLog.getComputeFixesTimeMillisVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }


    public <T> String getExplanationsCountQuery(Class<T> clazz) {
        if (clazz == ModuleReasoningLog.class) {
            return "SELECT " +
                    ModuleReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleReasoningLog.getExplanationsCountVarName() + "), 2) as avgExplanationsCount, " +
                    "ROUND(STDDEV(" + ModuleReasoningLog.getExplanationsCountVarName() + "), 2) as stdExplanationsCount, " +
                    "MAX(" + ModuleReasoningLog.getExplanationsCountVarName() + ") as maxExplanationsCount " +
                    "FROM " + this.moduleReasoningLogsTable + " " +
                    "WHERE " + ModuleReasoningLog.getExplanationsCountVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinReasoningLog.class) {
            return "SELECT " +
                    BinReasoningLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinReasoningLog.getExplanationsCountVarName() + "), 2) as avgExplanationsCounts, " +
                    "ROUND(STDDEV(" + BinReasoningLog.getExplanationsCountVarName() + "), 2) as stdExplanationsCount, " +
                    "MAX(" + BinReasoningLog.getExplanationsCountVarName() + ") as maxExplanationsCount " +
                    "FROM " + this.binReasoningLogsTable + " " +
                    "WHERE " + BinReasoningLog.getExplanationsCountVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }


    public <T> String getInnerRoundNumQuery(Class<T> clazz) {
        if (clazz == ModuleInnerRoundNumLog.class) {
            return "SELECT " +
                    ModuleInnerRoundNumLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + ModuleInnerRoundNumLog.getInnerRoundNumVarName() + "), 2) as avgInnerRoundNum, " +
                    "ROUND(STDDEV(" + ModuleInnerRoundNumLog.getInnerRoundNumVarName() + "), 2) as stdInnerRoundNum, " +
                    "MAX(" + ModuleInnerRoundNumLog.getInnerRoundNumVarName() + ") as maxInnerRoundNum " +
                    "FROM " + this.moduleInnerRoundNumLogsTable + " " +
                    "WHERE " + ModuleInnerRoundNumLog.getInnerRoundNumVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else if (clazz == BinInnerRoundNumLog.class) {
            return "SELECT " +
                    BinInnerRoundNumLog.getOuterRoundVarName() + " as outerRound, " +
                    "ROUND(AVG(" + BinInnerRoundNumLog.getInnerRoundNumVarName() + "), 2) as avgInnerRoundNum, " +
                    "ROUND(STDDEV(" + BinInnerRoundNumLog.getInnerRoundNumVarName() + "), 2) as stdInnerRoundNum, " +
                    "MAX(" + BinInnerRoundNumLog.getInnerRoundNumVarName() + ") as maxInnerRoundNum " +
                    "FROM " + this.binInnerRoundNumLogsTable + " " +
                    "WHERE " + BinInnerRoundNumLog.getInnerRoundNumVarName() + " > 0 " +
                    "GROUP BY outerRound " +
                    "ORDER BY outerRound";
        }
        else {
            throw new IllegalArgumentException("Unsupported class: " + clazz);
        }
    }

    public String getTopModules() {
        return "SELECT " +
                ModuleSizeLog.getOuterRoundVarName() + " as outerRound, " +
                ModuleSizeLog.getBaseIndividualVarName() + " as baseIndividual, " +
                ModuleSizeLog.getModuleSizeVarName() + " as moduleSize " +
                "FROM " + this.moduleSizeLogsTable + " " +
                "ORDER BY moduleSize DESC, baseIndividual ASC " +
                "LIMIT " + this.limitInTop;
    }

    public String getTopBins() {
        return "SELECT " +
                BinSizeLog.getOuterRoundVarName() + " as outerRound, " +
                BinSizeLog.getBaseIndividualsVarName() + " as baseIndividuals, " +
                BinSizeLog.getBinSizeVarName() + " as binSize " +
                "FROM " + this.moduleSizeLogsTable + " " +
                "ORDER BY binSize DESC, baseIndividuals ASC " +
                "LIMIT " + this.limitInTop;
    }
}