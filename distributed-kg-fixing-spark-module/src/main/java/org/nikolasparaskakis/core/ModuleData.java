package org.nikolasparaskakis.core;



import java.util.ArrayList;



/**
 * Class representing data about a module.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ModuleData {

    /**
     * The base individual of the module.
     */
    private final String baseIndividual;

    /**
     * The neighbors of the base individual in the module.
     */
    private final ArrayList<String> neighbors;

    /**
     * The size of the module.
     */
    private final int moduleSize;

    /**
     * Constructor for ModuleData.
     * @param baseIndividual The base individual of the module.
     * @param neighbors The neighbors of the base individual in the module.
     * @param moduleSize The size of the module.
     */
    public ModuleData(String baseIndividual, ArrayList<String> neighbors, int moduleSize) {
        this.baseIndividual = baseIndividual;
        this.neighbors = neighbors;
        this.moduleSize = moduleSize;
    }

    /**
     * Gets the base individual of the module.
     * @return The base individual of the module.
     */
    public String getBaseIndividual() {
        return baseIndividual;
    }

    /**
     * Gets the neighbors of the base individual in the module.
     * @return The neighbors of the base individual in the module.
     */
    public ArrayList<String> getNeighbors() {
        return neighbors;
    }

    /**
     * Gets the size of the module.
     * @return The size of the module.
     */
    public int getModuleSize() {
        return moduleSize;
    }
}