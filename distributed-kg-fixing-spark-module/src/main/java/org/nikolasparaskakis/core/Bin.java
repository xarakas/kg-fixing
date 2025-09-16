package org.nikolasparaskakis.core;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * Class representing a bin for bin packing.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class Bin {

    /**
     * The capacity of the bin.
     */
    public int capacity;

    /**
     * The list of modules assigned to this bin.
     */
    public List<ModuleData> items;

    /**
     * The used capacity of the bin.
     */
    public int used;

    /**
     * Constructor for Bin.
     * @param capacity The capacity of the bin.
     */
    public Bin(int capacity) {
        this.capacity = capacity;
        this.items = new ArrayList<>();
        this.used = 0;
    }

    /**
     * Checks if a module of given size can fit in the bin.
     * @param size The size of the module.
     * @return True if the module can fit in the bin, false otherwise.
     */
    public boolean canFit(int size) {
        return used + size <= capacity;
    }

    /**
     * Adds a module to the bin.
     * @param md The module to add.
     */
    public void add(ModuleData md) {
        items.add(md);
        used += md.getModuleSize();
    }

    /**
     * Gets the remaining capacity of the bin.
     * @return The remaining capacity of the bin.
     */
    public int remaining() {
        return capacity - used;
    }

    /**
     * Merges the neighbors of all modules in the bin into a single list without duplicates.
     * @return A list of unique neighbors from all modules in the bin.
     */
    public ArrayList<String> mergedNeighbors() {
        Set<String> all = new HashSet<>();
        for (ModuleData md : items) {
            all.addAll(md.getNeighbors());
        }
        return new ArrayList<>(all);
    }

    /**
     * Gets the base individuals of all modules in the bin without duplicates.
     * @return A list of unique base individuals from all modules in the bin.
     */
    public ArrayList<String> baseIndividuals() {
        Set<String> all = new HashSet<>();
        for (ModuleData md : items) {
            all.add(md.getBaseIndividual());
        }
        return new ArrayList<>(all);
    }

    /**
     * Assigns modules to bins using a best-fit decreasing strategy.
     * @param modules The list of modules to assign.
     * @param bins The list of bins to assign modules to.
     * @param binCapacity The capacity of each bin.
     */
    public static void assignModulesToBins(List<ModuleData> modules, List<Bin> bins, int binCapacity) {
        for (ModuleData md : modules) {
            Bin best = null;
            int bestRem = Integer.MAX_VALUE;
            for (Bin b : bins) {
                if (b.canFit(md.getModuleSize())) {
                    int rem = b.remaining() - md.getModuleSize();
                    if (rem < bestRem) {
                        bestRem = rem;
                        best = b;
                    }
                }
            }
            if (best != null) {
                best.add(md);
            }
            else {
                // fallback: expand bin to hold this module exactly
                int capacityForBin = Math.max(md.getModuleSize(), binCapacity);
                Bin b = new Bin(capacityForBin);
                b.add(md);
                bins.add(b);
            }

        }
    }
}