package util;

public class MenuUtils {

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_CYAN   = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BOLD   = "\u001B[1m";

    public static void printHeader(String title) {
        int width = 72;
        String line = "=".repeat(width);
        System.out.println("\n" + line);
        System.out.println("  " + title.toUpperCase());
        System.out.println(line);
    }

    public static void printSubHeader(String subtitle) {
        System.out.println("\n--- " + subtitle + " ---");
    }

    public static String successBanner(String message) {
        return "[✓ SUCCESS] " + message;
    }

    public static String errorBanner(String message) {
        return "[✗ ERROR] " + message;
    }

    public static String warningBanner(String message) {
        return "[! WARNING] " + message;
    }

    public static String infoBanner(String message) {
        return "[i INFO] " + message;
    }

    public static void printAppLogo() {
        System.out.println("========================================================================");
        System.out.println("  ███████╗██████╗ ██╗   ██╗████████╗██████╗  █████╗  ██████╗██╗  ██╗   ");
        System.out.println("  ██╔════╝██╔══██╗██║   ██║╚══██╔══╝██╔══██╗██╔══██╗██╔════╝██║ ██╔╝   ");
        System.out.println("  █████╗  ██║  ██║██║   ██║   ██║   ██████╔╝███████║██║     █████╔╝    ");
        System.out.println("  ██╔══╝  ██║  ██║██║   ██║   ██║   ██╔══██╗██╔══██║██║     ██╔═██╗    ");
        System.out.println("  ███████╗██████╔╝╚██████╔╝   ██║   ██║  ██║██║  ██║╚██████╗██║  ██╗   ");
        System.out.println("  ╚══════╝╚═════╝  ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝   ");
        System.out.println("          ACADEMIC & PLACEMENT MANAGEMENT SYSTEM                        ");
        System.out.println("                   EduTrack Console Application                         ");
        System.out.println("========================================================================");
    }
}
