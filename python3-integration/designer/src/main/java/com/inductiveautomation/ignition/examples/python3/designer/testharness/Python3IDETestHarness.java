package com.inductiveautomation.ignition.examples.python3.designer.testharness;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.designer.Python3IDE;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Standalone test harness for rapid Python3IDE UX development.
 * <p>
 * This launcher allows you to test the IDE UI without:
 * <ul>
 *   <li>Building and installing the module</li>
 *   <li>Running an Ignition Gateway</li>
 *   <li>Opening the Ignition Designer</li>
 * </ul>
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * ./gradlew runIDE
 * </pre>
 * <p>
 * <strong>How it works:</strong>
 * <ol>
 *   <li>Starts a mock REST API server on localhost:9088</li>
 *   <li>Creates a mock DesignerContext</li>
 *   <li>Launches Python3IDE in a standalone JFrame</li>
 *   <li>All REST API calls are intercepted and return mock data</li>
 * </ol>
 * <p>
 * <strong>Benefits:</strong>
 * <ul>
 *   <li>⚡ Instant feedback - Launch in ~3 seconds</li>
 *   <li>🔄 Rapid iteration - No module rebuild needed</li>
 *   <li>🎨 UX testing - Test themes, layouts, dialogs</li>
 *   <li>🧪 Mock scenarios - Simulate errors, timeouts</li>
 * </ul>
 */
public class Python3IDETestHarness {
    private static MockRestServer mockServer;

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Python 3 IDE - Standalone Test Harness");
        System.out.println("=".repeat(80));
        System.out.println();

        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("✓ System Look and Feel applied");
        } catch (Exception e) {
            System.err.println("⚠ Failed to set Look and Feel: " + e.getMessage());
        }

        // Start mock REST API server on port 9089 (9088 may be used by real Gateway)
        try {
            mockServer = new MockRestServer(9089);
            mockServer.start();
            System.out.println("✓ Mock REST server started on http://localhost:9089");
        } catch (IOException e) {
            System.err.println("✗ Failed to start mock REST server: " + e.getMessage());
            System.exit(1);
        }

        // Create mock Designer context
        DesignerContext mockContext = MockDesignerContext.create();
        System.out.println("✓ Mock DesignerContext created");

        // Launch IDE on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {
                // Create IDE panel
                Python3IDE idePanel = new Python3IDE(mockContext);
                System.out.println("✓ Python3IDE panel created");

                // Create and configure frame
                JFrame frame = new JFrame("Python 3 IDE - Test Harness (Mock Gateway)");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1400, 900);
                frame.setLocationRelativeTo(null);  // Center on screen

                // Add IDE panel to frame
                frame.add(idePanel);

                // Add shutdown hook to stop mock server
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n" + "=".repeat(80));
                    System.out.println("Shutting down test harness...");
                    if (mockServer != null) {
                        mockServer.stop();
                    }
                    System.out.println("✓ Test harness stopped");
                    System.out.println("=".repeat(80));
                }));

                // Show frame
                frame.setVisible(true);
                System.out.println("✓ IDE window opened");
                System.out.println();
                System.out.println("=".repeat(80));
                System.out.println("🚀 Python 3 IDE is now running!");
                System.out.println("=".repeat(80));
                System.out.println();
                System.out.println("Tips:");
                System.out.println("  • Change Gateway URL to: http://localhost:9089 (mock server)");
                System.out.println("  • Click 'Connect' to establish mock connection");
                System.out.println("  • All code execution returns mock responses");
                System.out.println("  • Themes, dialogs, and UI features work normally");
                System.out.println("  • Close window or press Ctrl+C to exit");
                System.out.println();

            } catch (Exception e) {
                System.err.println("✗ Failed to create IDE: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
