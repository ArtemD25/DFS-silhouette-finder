package com.shpp.p2p.cs.adavydenko.assignment12;

/**
 * The class representing a silhouette found by the program
 * on a user provided image. The object Silhouette contains
 * information on how many pixels does this silhouette has
 * and which pixels of the image exactly.
 */
public class Silhouette {
    /**
     * Number of pixels the silhouette has.
     */
    protected int numOfPixels = 0;

    /**
     * The pixels of the image that are deemed as part of this
     * particular silhouette are marked with true.
     */
    protected boolean[][] silhouettePixels;

    /**
     * Fills boolean array to indicate which image pixels exactly are deemed
     * as this silhouette`s pixels
     */
    public Silhouette() {
        silhouettePixels = Assignment12Part1.fillBooleanArray();
    }
}
