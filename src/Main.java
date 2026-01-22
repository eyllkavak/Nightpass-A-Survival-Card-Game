/**
 * CMPE 250 Project 1 - Nightpass Survivor Card Game
 *
 * This skeleton provides file I/O infrastructure. Implement your game logic
 * as you wish. There are some import that is suggested to use written below.
 * You can use them freely and create as manys classes as you want. However,
 * you cannot import any other java.util packages with data structures, you
 * need to implement them yourself. For other imports, ask through Moodle before
 * using.
 *
 * TESTING YOUR SOLUTION:
 * ======================
 *
 * Use the Python test runner for automated testing:
 *
 * python test_runner.py              # Test all cases
 * python test_runner.py --type type1 # Test only type1
 * python test_runner.py --type type2 # Test only type2
 * python test_runner.py --verbose    # Show detailed diffs
 * python test_runner.py --benchmark  # Performance testing (no comparison)
 *
 * Flags can be combined, e.g.:
 * python test_runner.py -bv              # benchmark + verbose
 * python test_runner.py -bv --type type1 # benchmark + verbose + type1
 * python test_runner.py -b --type type2  # benchmark + type2
 *
 * MANUAL TESTING (For Individual Runs):
 * ======================================
 *
 * 1. Compile: cd src/ && javac *.java
 * 2. Run: java Main ../testcase_inputs/test.txt ../output/test.txt
 * 3. Compare output with expected results
 *
 * PROJECT STRUCTURE:
 * ==================
 *
 * project_root/
 * ├── src/                     # Your Java files (Main.java, etc.)
 * ├── testcase_inputs/         # Input test files
 * ├── testcase_outputs/        # Expected output files
 * ├── output/                  # Generated outputs (auto-created)
 * └── test_runner.py           # Automated test runner
 *
 * REQUIREMENTS:
 * =============
 * - Java SDK 8+ (javac, java commands)
 * - Python 3.6+ (for test runner)
 *
 * @author Eylül Başak KAVAK
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * This class serves as the entry point for the card game simulation.
 * Its primary responsibilities include handling file, parsing the input
 * commands line by line, and delegating the core game logic.
 */
public class Main {

    /**
     *Reads commands from the input file, processes them, and writes the results to the output file.
     * @param args Command line arguments: [0] input file path, [1] output file path.
     */
    public static void main(String[] args) {
        // Check command line arguments
        if (args.length != 2) {
            System.out.println("Usage: java Main <input_file> <output_file>");
            System.out.println("Example: java Main ../testcase_inputs/test.txt ../output/test.txt");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        // Initialize the CardManager, which holds the game state and logic.
        CardManager manager = new CardManager();
        // Initialize file reader
        Scanner reader = null;
        try {
            reader = new Scanner(new File(inputFile));
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inputFile);
            e.printStackTrace();
            return;
        }

        // Initialize file writer
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
        } catch (IOException e) {
            System.out.println("Writing error: " + outputFile);
            e.printStackTrace();
            if (reader != null)
                reader.close();
            return;
        }

        // Process commands line by line
        try {
            while (reader.hasNext()) {
                String line = reader.nextLine().trim();
                if (line.isEmpty()) continue;

                Scanner scanner = new Scanner(line);
                String command = scanner.next();
                String out = "";

                switch (command) {
                    case "draw_card": {
                        String name = "";
                        int attack = 0;
                        int health = 0;
                        if (scanner.hasNext())
                            name = scanner.next();
                        if (scanner.hasNext())
                            attack = scanner.nextInt();
                        if (scanner.hasNext())
                            health = scanner.nextInt();
                        out = manager.handleDrawCard(name, attack, health);
                        break;
                    }
                    case "battle": {
                        int attack = 0;
                        int health = 0;
                        int heal = 0;
                        if (scanner.hasNext())
                            attack = scanner.nextInt();
                        if (scanner.hasNext())
                            health = scanner.nextInt();
                        if (scanner.hasNext())
                            heal = scanner.nextInt();
                        out = manager.handleBattle(attack, health,heal);
                        break;
                    }
                    case "find_winning": {
                        out = manager.handleFindWinning();
                        break;
                    }
                    case "deck_count": {
                        out = manager.handleDeckCount();
                        break;
                    }
                    case "discard_pile_count": {
                        out = manager.handleDiscardPileCount();
                        break;
                    }
                    case "steal_card": {
                        int attack = 0;
                        int health = 0;
                        if (scanner.hasNext())
                            attack = scanner.nextInt();
                        if (scanner.hasNext())
                            health = scanner.nextInt();
                        out = manager.handleStealCard(attack, health);
                        break;
                    }
                    default: {
                        System.out.println("Invalid command: " + command);
                        scanner.close();
                        writer.close();
                        reader.close();
                        return;
                    }
                }

                scanner.close();

                try {
                    writer.write(out + "\n");
                } catch (IOException e2) {
                    System.out.println("Writing error");
                    e2.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("Error processing commands: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up resources
        try {
            writer.close();
        } catch (IOException e2) {
            System.out.println("Writing error");
            e2.printStackTrace();
        }

        if (reader != null) {
            reader.close();
        }

        System.out.println("end");
        return;
    }
}