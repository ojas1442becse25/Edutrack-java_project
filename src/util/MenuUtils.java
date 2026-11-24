package util;

/**
 * Console presentation formatting utilities:
 * ASCII box banners, tabular alignment, status indicators, and headers.
 */
public class MenuUtils {

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_CYAN   = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BOLD   = "\u001B[1m";

    /**
     * Prints an ASCII header box with centered or left-aligned title.
     */
    public static void printHeader(String title) {
        int width = 72;
        String line = "=".repeat(width);
        System.out.println("\n" + line);
        System.out.println("  " + title.toUpperCase());
        System.out.println(line);
    }

    /**
     * Prints a sub-section banner.
     */
    public static void printSubHeader(String subtitle) {
        System.out.println("\n--- " + subtitle + " ---");
    }

    /**
     * Generates a success banner.
     */
    public static String successBanner(String message) {
        return "[✓ SUCCESS] " + message;
    }

    /**
     * Generates an error banner.
     */
    public static String errorBanner(String message) {
        return "[✗ ERROR] " + message;
    }

    /**
     * Generates a warning banner.
     */
    public static String warningBanner(String message) {
        return "[! WARNING] " + message;
    }

    /**
     * Generates an info banner.
     */
    public static String infoBanner(String message) {
        return "[i INFO] " + message;
    }

    /**
     * Prints the application startup banner.
     */
    public static void printAppLogo() {
        System.out.println("========================================================================");
        System.out.println("  ███████╗██████╗ ██╗   ██╗████████╗██████╗  █████╗  ██████╗██╗  ██╗   ");
        System.out.println("  ██╔════╝██╔══██╗██║   ██║╚══██╔══╝██╔══██╗██╔══██╗██╔════╝██║ ██╔╝   ");
        System.out.println("  █████╗  ██║  ██║██║   ██║   ██║   ██████╔╝███████║██║     █████╔╝    ");
        System.out.println("  ██╔══╝  ██║  ██║██║   ██║   ██║   ██╔══██╗██╔══██║██║     ██╔═██╗    ");
        System.out.println("  ███████╗██████╔╝╚██████╔╝   ██║   ██║  ██║██║  ██║╚██████╗██║  ██╗   ");
        System.out.println("  ╚══════╝╚═════╝  ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝   ");
        System.out.println("          ACADEMIC & PLACEMENT MANAGEMENT CONSOLE SYSTEM                ");
        System.out.println("      Milestone 5 | Java 8+ | MySQL 8.0 | JDBC 3-Layer Architecture    ");
        System.out.println("========================================================================");
    }
}
