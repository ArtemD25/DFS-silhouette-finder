package com.shpp.p2p.cs.adavydenko.assignment12;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * This class takes user image, reads it and defines the number of silhouettes
 * on the image. The background of the image and the color of the silhouettes
 * shall be in contrast.
 * <p>
 * One can control program outputs by changing three parameters:
 * --- COLORS_SIMILARITY_THRESHOLD
 * The larger this value, the fewer pixels with a similar color to the background color
 * will be considered as background color.
 * --- MINIMUM_SILHOUETTE_SIZE
 * If this parameter is set to zero, the program will define all kind of small objects
 * as silhouettes. It is advised to keep this parameter at 30-50 pixels level to get
 * correct results. But if one checks a big image (with one of the sides having 2 000
 * or more pixels), one shall increase this parameter significantly.
 * --- MINIMUM_SILHOUETTE_SIZE
 * The value of this parameter defines which objects on the image will be deemed as
 * non-silhouettes e.g. posterized pixels, artifacts and small insignificant objects
 * (fuzzy objects etc).
 * <p>
 * I took part of ideas from external resources:
 * - The BufferedImage concept
 * https://docs.oracle.com/javase/7/docs/api/java/awt/image/BufferedImage.html
 * - The File concept
 * https://metanit.com/java/tutorial/6.11.php
 * - The idea how to calculate pixel color difference
 * https://stackoverflow.com/questions/15262258/how-could-i-compare-colors-in-java
 * - The idea of using stack- and visited-arrays
 * https://evileg.com/ru/post/494/
 */
public class Assignment12Part1 {

    /**
     * The value indicating that two colors are totally equal.
     */
    static final double COLORS_ARE_TOTALLY_EQUIVALENT = 510.0;

    /**
     * The proportion of similarity between the two colors.
     * If a certain value is less than this value, the colors
     * are considered different. If larger, the colors
     * are considered equivalent.
     */
    static final double COLORS_SIMILARITY_THRESHOLD = 0.95;

    /**
     * The minimum number of pixels of a particular non-background
     * object to be deemed as a silhouette.
     */
    static final int MINIMUM_SILHOUETTE_SIZE = 60;

    /**
     * The image the user provided to the program.
     */
    static BufferedImage image = null;

    /**
     * The background color that differs from silhouettes colors
     * and its four channels values.
     */
    static Color bgColor;
    static int bgColorRed;
    static int bgColorGreen;
    static int bgColorBlue;
    static int bgColorAlpha;

    /**
     * A two-dimensional array consisting of the colors of the pixels
     * of the user provided image. Each array cell represents a corresponding
     * pixel in the user image.
     */
    static Color[][] imgArray;

    /**
     * An array indicating whether the program already inspected a particular
     * pixel of the image. The array has the same dimensions as the imgArray.
     */
    static boolean[][] visited;

    /**
     * An array containing the path through the image pixels the program did
     * while looking for silhouettes pixels.
     */
    static Stack<int[]> stack = new Stack<>();

    /**
     * An array with all silhouettes the program found on the image.
     */
    static ArrayList<Silhouette> silhouettes = new ArrayList<>();

    /**
     * Variable saying the program already inspected first silhouette pixel.
     * This variable prevents the program from exiting the dfs-algorithm
     * after starting inspecting silhouette`s first pixel. And helps exit
     * the dfs-algorithm when the stack is empty and all pixels are already
     * inspected.
     */
    static boolean firstPixelVisited = false;

    /**
     * Main method launching the finding silhouettes on the user image algorithm.
     *
     * @param args are command line arguments provided by user.
     */
    public static void main(String[] args) {
        findSilhouettes(args);
    }

    /**
     * Uploads user image, converts it to a colors array, defines background color,
     * iterates through all pixels and looks for silhouettes using dfs-algorithm.
     *
     * @param args are command line arguments provided by user.
     */
    private static void findSilhouettes(String[] args) {
        try {
            File file = new File(getFilePath(args)); // Gets image location
            image = ImageIO.read(file);              // Reads user image from the provided location
            bgColor = getBackgroundColor();          // Defines background color
            //displayBackgroundColor();              // Displays background color to console (for debugging purposes)
            setBgColorComponents();                  // Saves background color`s RGBA-components
            writeImageToArray();                     // Creates an array consisting of image pixel colors
            visited = fillBooleanArray();            // Fills visited-array to indicate visited pixels
            inspectImagePixels();                    // Inspects all image pixels to find silhouettes
            countAndDisplayNumOfSilhouettes();       // Counts and displays number of silhouettes
        } catch (Exception evt) {
            System.out.println(evt);                 // Display the error occurred if any
        }
    }

    /**
     * Defines the user image location.
     *
     * @param args are the command line arguments provided by user.
     * @return user image location as a string.
     */
    private static String getFilePath(String[] args) {
        String filePath;

        if (args.length == 0) {
            filePath = "test.jpg";
        } else {
            filePath = args[0];
        }
        return filePath;
    }

    /**
     * Creates a two-dimensional array consisting of color objects.
     * Each of them represents the color a corresponding pixel of
     * the image provided by the user.
     * E.g. the imgArray[1][1] color represents the color of the
     * second pixel in the second row in the user image.
     */
    private static void writeImageToArray() {
        /* Creates additional one-pixel-thick rows / columns on the very top, bottom,
         left and right edges of the array to fill them later with background color */
        imgArray = new Color[image.getHeight() + 2][image.getWidth() + 2];
        fillPixelsWithBGColor(); // Fills array`s edges with background color

        // Copies the color values of the image pixels to the corresponding cells of the imgArray
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color currentColor = new Color(image.getRGB(j, i), true);
                imgArray[i + 1][j + 1] = currentColor;
            }
        }
    }

    /**
     * Fills the very top, bottom, left and right edges of the array
     * with background color.
     * This is the way to prevent the program from throwing an error
     * when a silhouette touches image edge.
     */
    private static void fillPixelsWithBGColor() {
        for (int y = 0; y < imgArray.length; y++) {
            imgArray[y][0] = bgColor;
            imgArray[y][imgArray[0].length - 1] = bgColor;
        }
        for (int x = 0; x < imgArray[0].length; x++) {
            imgArray[0][x] = bgColor;
            imgArray[imgArray.length - 1][x] = bgColor;
        }
    }

    /**
     * Iterates through each image pixel and looks for pixels of non-background color.
     * If found any, the program uses depth-first search algorithm to detect all such
     * pixels and deems them as silhouette pixels. If the program finds pixels of the
     * background color, it marks them visited and goes on looking for silhouettes.
     */
    private static void inspectImagePixels() {
        for (int x = 0; x < imgArray.length; x++) {
            for (int y = 0; y < imgArray[0].length; y++) {

                if (!visited[x][y]) { // The program enters this condition only if it has not inspected this pixel yet
                    Color currentColor = imgArray[x][y];
                    // If it is not a background pixel, deem it as a silhouette pixel and find other silhouette pixels
                    if (isNotSimilarToBackground(currentColor)) {
                        // Create new silhouette object if found first non-background pixel
                        Silhouette silhouette = new Silhouette();
                        silhouettes.add(silhouette); // Add this silhouette to the array with all silhouettes
                        dfs(x, y, silhouette); // Run recursive depth-first search algorithm
                        firstPixelVisited = false;
                    } else {
                        // If its is background pixel and we have not visited it yet, mark it visited
                        visited[x][y] = true;
                    }
                }
            }
        }
    }

    /**
     * Recursive depth-first search (DFS) algorithm that detects all pixels belonging to a silhouette.
     *
     * @param x          is the x-coordinate of the starting pixel for dfs-algorithm
     * @param y          is the y-coordinate of the starting pixel for dfs-algorithm
     * @param silhouette is the object containing the number of pixels this particular
     *                   silhouette consists of and the silhouette`s pixels coordinates
     */
    private static void dfs(int x, int y, Silhouette silhouette) {
        // Array with current pixel coordinates
        int[] currentPixelCoordinates = {x, y};

        // If stack is empty and all pixels are visited, step out of the dfs algorithm
        if (stack.size() == 0 && firstPixelVisited) {
            return;
        }
        firstPixelVisited = true; // Variable saying the program already inspected first silhouette pixel

        // If a particular pixel is not marked as visited yet, the program shall mark it visited
        if (!visited[x][y]) {
            silhouette.numOfPixels += 1; // Increase number of silhouette`s pixels by one
            visited[x][y] = true;
            silhouette.silhouettePixels[x][y] = true; // Link this pixel to the silhouette object
        }

        if (noChildrenLeft(x, y)) { // If the dfs-algorithm can not go deeper
            if (stack.size() > 0) { // If there are any pixels left in the stack
                int nextPixelX = stack.peek()[0]; // Next pixel x-coordinate
                int nextPixelY = stack.peek()[1]; // Next pixel y-coordinate
                stack.pop(); // Delete last pixel from the stack
                dfs(nextPixelX, nextPixelY, silhouette); // Run DFS on this last pixel
            }
        } else {
            stack.push(currentPixelCoordinates); // Add this pixel coordinates to the stack
            dfs(chooseAnyChildren(x, y)[0], chooseAnyChildren(x, y)[1], silhouette);
        }
    }

    /**
     * Chooses pixels the program shall inspect next. These pixels:
     * --- shall be not visited yet;
     * --- shall exist;
     * --- shall have non-background color;
     * --- shall be "children" and not "peers" to the currently inspected pixel.
     *
     * @param x is the x-coordinate of the current pixel.
     * @param y is the y-coordinate of the current pixel.
     * @return x- and y-coordinate of the next pixel to be inspected with dfs-algorithm.
     */
    private static int[] chooseAnyChildren(int x, int y) {
        int[] coordinatesArray = new int[2]; // Array to save coordinates of the next pixel to inspect

        if ((x + 1 < imgArray.length) && !visited[x + 1][y] && isNotSimilarToBackground(x + 1, y)) {
            coordinatesArray[0] = x + 1;
            coordinatesArray[1] = y;
        } else if ((x - 1 >= 0) && !visited[x - 1][y] && isNotSimilarToBackground(x - 1, y)) {
            coordinatesArray[0] = x - 1;
            coordinatesArray[1] = y;
        } else if ((y + 1 < imgArray[0].length) && !visited[x][y + 1] && isNotSimilarToBackground(x, y + 1)) {
            coordinatesArray[0] = x;
            coordinatesArray[1] = y + 1;
        } else if ((y - 1 >= 0) && !visited[x][y - 1] && isNotSimilarToBackground(x, y - 1)) {
            coordinatesArray[0] = x;
            coordinatesArray[1] = y - 1;
        }
        return coordinatesArray;
    }

    /**
     * Says whether there are any pixels the program can visit starting from the current pixel.
     * The program can not visit a pixel if it does not exist, if it is already visited or
     * if it is a background pixel.
     *
     * @param x is the x-coordinate of the current pixel.
     * @param y is the y-coordinate of the current pixel.
     * @return true if the neighbor pixels do not exist
     * or are already visited or have background color.
     */
    private static boolean noChildrenLeft(int x, int y) {

        if (((x + 1 >= imgArray.length) || visited[x + 1][y] || !isNotSimilarToBackground(x + 1, y))
                && ((x - 1 < 0) || visited[x - 1][y] || !isNotSimilarToBackground(x - 1, y))
                && ((y + 1 >= imgArray[0].length) || visited[x][y + 1] || !isNotSimilarToBackground(x, y + 1))
                && ((y - 1 < 0) || visited[x][y - 1]) || !isNotSimilarToBackground(x, y - 1)) {
            return true;
        }
        return false;
    }

    /**
     * Defines which color shall be deemed as background color.
     *
     * @return background color as Color object.
     */
    private static Color getBackgroundColor() {
        /* HashMap with all colors which can be fined on the picture
        edges and the number of pixels with these colors */
        HashMap<Color, Integer> numOfColors = new HashMap<>();

        inspectTopImageEdge(numOfColors);
        inspectBottomImageEdge(numOfColors);
        inspectLeftImageEdge(numOfColors);
        inspectRightImageEdge(numOfColors);
        return findMaxPixelNumber(numOfColors);
    }

    /**
     * Defines background color`s all four channels values
     * (red, green, blue and alpha).
     */
    private static void setBgColorComponents() {
        bgColorRed = bgColor.getRed();
        bgColorGreen = bgColor.getGreen();
        bgColorBlue = bgColor.getBlue();
        bgColorAlpha = bgColor.getAlpha();
    }

    /**
     * Inspects three top pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three top rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectTopImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < 3; j++) {
                Color color = new Color(image.getRGB(i, j), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three bottom pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three bottom rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectBottomImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = image.getHeight() - 3; j < image.getHeight(); j++) {
                Color color = new Color(image.getRGB(i, j), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three left pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three left rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectLeftImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < 3; j++) {
                Color color = new Color(image.getRGB(j, i), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three right pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three right rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectRightImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = image.getWidth() - 3; j < image.getWidth(); j++) {
                Color color = new Color(image.getRGB(j, i), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Calculates the color equivalence coefficient between the background color
     * and the color that is compared with the background color.
     * If the compared color is more than {COLORS_SIMILARITY_THRESHOLD} % similar to the background color,
     * it is deemed to be a background color and not a silhouettes color. If less - it is deemed to be
     * a silhouettes color.
     *
     * @param color is the color that is compared with the background color.
     * @return true if both colors are not similar.
     */
    private static boolean isNotSimilarToBackground(Color color) {
        // Values of red, green, blue and alpha channels of the pixel compared to the background color
        int pixelColorRed = color.getRed();
        int pixelColorGreen = color.getGreen();
        int pixelColorBlue = color.getBlue();
        int pixelColorAlpha = color.getAlpha();

        // Calculates the difference between two colors by calculating the difference between all four channels
        double colorDifference = Math.pow(Math.pow(bgColorRed - pixelColorRed, 2)
                + Math.pow(bgColorGreen - pixelColorGreen, 2)
                + Math.pow(bgColorBlue - pixelColorBlue, 2)
                + Math.pow(bgColorAlpha - pixelColorAlpha, 2), (0.5));

        // Calculates color equivalence coefficient between background and the current colors
        double colorEquivalenceCoefficient = (COLORS_ARE_TOTALLY_EQUIVALENT - colorDifference)
                / COLORS_ARE_TOTALLY_EQUIVALENT;

        // Returns true if color equivalence coefficient is more than certain threshold
        return !(colorEquivalenceCoefficient > COLORS_SIMILARITY_THRESHOLD);
    }

    /**
     * Calculates the color equivalence coefficient between the background color
     * and the color that is compared with the background color.
     * If the compared color is more than {COLORS_SIMILARITY_THRESHOLD} % similar to the background color,
     * it is deemed to be a background color and not a silhouettes color. If less - it is deemed to be
     * a silhouettes color.
     *
     * @param x is the x-coordinate of the color to be compared with the background color.
     * @param y is the y-coordinate of the color to be compared with the background color.
     * @return true if both colors are not similar.
     */
    private static boolean isNotSimilarToBackground(int x, int y) {
        Color color = imgArray[x][y];
        // Values of red, green, blue and alpha channels of the pixel compared to the background color
        int pixelColorRed = color.getRed();
        int pixelColorGreen = color.getGreen();
        int pixelColorBlue = color.getBlue();
        int pixelColorAlpha = color.getAlpha();

        // Calculates the difference between two colors by calculating the difference between all four channels
        double colorDifference = Math.pow(Math.pow(bgColorRed - pixelColorRed, 2)
                + Math.pow(bgColorGreen - pixelColorGreen, 2)
                + Math.pow(bgColorBlue - pixelColorBlue, 2)
                + Math.pow(bgColorAlpha - pixelColorAlpha, 2), (0.5));

        // Calculates color equivalence coefficient between background and the current colors
        double colorEquivalenceCoefficient = (COLORS_ARE_TOTALLY_EQUIVALENT - colorDifference)
                / COLORS_ARE_TOTALLY_EQUIVALENT;

        // Returns true if color equivalence coefficient is more than certain threshold
        return !(colorEquivalenceCoefficient > COLORS_SIMILARITY_THRESHOLD);
    }

    /**
     * Saves the color of a particular pixel to the hashmap as a key and the number of
     * pixels of this color as a value.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     * @param color       is a color of a particular pixel.
     */
    private static void saveColorData(HashMap<Color, Integer> numOfColors, Color color) {
        if (!numOfColors.containsKey(color)) {
            numOfColors.put(color, 1);
        } else {
            int pixelNum = numOfColors.get(color);
            numOfColors.put(color, pixelNum + 1);
        }
    }

    /**
     * Iterates through the numOfColors hashmap and finds the color with the biggest
     * number of pixels.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     * @return the color that is deemed to be a background color.
     */
    private static Color findMaxPixelNumber(HashMap<Color, Integer> numOfColors) {
        // Saves the biggest number of pixels of a particular color in the numOfColors hashmap
        int maxValue = 0;
        // Saves the color from the numOfColors hashmap with the biggest number of pixels
        Color bgColor = null;

        for (Color color : numOfColors.keySet()) {
            if (numOfColors.get(color) > maxValue) {
                maxValue = numOfColors.get(color);
                bgColor = color;
            }
        }
        return bgColor;
    }

    /**
     * Fills boolean array of the same size as the image array
     * with "false" values.
     *
     * @return an array of the same size that the image array
     * filled with "false" values.
     */
    protected static boolean[][] fillBooleanArray() {
        boolean[][] booleanArray = new boolean[imgArray.length][imgArray[0].length];

        for (int i = 0; i < imgArray.length; i++) {
            for (int j = 0; j < imgArray[0].length; j++) {
                booleanArray[i][j] = false;
            }
        }
        return booleanArray;
    }

    /**
     * Prints to console the red, green, blue and alpha channels
     * values of the color the program defined as the background color.
     */
    private static void displayBackgroundColor() {
        System.out.println("The background color is: "
                + "\n" + "-- red: " + bgColor.getRed()
                + "\n" + "-- green: " + bgColor.getGreen()
                + "\n" + "-- blue: " + bgColor.getBlue()
                + "\n" + "-- alpha: " + bgColor.getAlpha());
    }

    /**
     * Takes an array with all detected silhouettes and counts only those
     * with more than {minimumSilhouetteSize} pixels.
     */
    private static void countAndDisplayNumOfSilhouettes() {
        // Number of silhouettes detected on the image
        int numOfSilhouettes = 0;

        // Increase numOfSilhouettes by one if the silhouette has more than {minimumSilhouetteSize} pixels
        for (Silhouette silhouette : silhouettes) {
            if (silhouette.numOfPixels > MINIMUM_SILHOUETTE_SIZE) {
                numOfSilhouettes++;
                System.out.println("Silhouette №" + numOfSilhouettes + " - " + silhouette.numOfPixels + " pixels");
            }
        }
        System.out.println("Total number of silhouettes: " + numOfSilhouettes);
    }
}
