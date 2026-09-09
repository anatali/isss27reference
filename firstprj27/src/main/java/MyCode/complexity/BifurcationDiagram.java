package MyCode.complexity;

import javax.swing.*;
import java.awt.*;

public class BifurcationDiagram extends JPanel {

    // Parametri della simulazione
    private static final double R_MIN = 2.8;
    private static final double R_MAX = 4.0;
    private static final int TRANSIENT_STEPS = 500; // Passi scartati per eliminare il transitorio
    private static final int PLOT_STEPS = 300;      // Punti disegnati per ciascun valore di r

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Abilita l'Antialiasing per una resa grafica migliore
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Sfondo nero
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // Colore verde ad alta trasparenza per evidenziare la densità dei punti caotici
        g2d.setColor(new Color(0, 255, 120, 40));

        // Scansione colonna per colonna (pixel orizzontali)
        for (int pixelX = 0; pixelX < width; pixelX++) {
            // Mappatura dal pixel X al parametro r
            double r = R_MIN + ((double) pixelX / width) * (R_MAX - R_MIN);

            double x = 0.5; // Valore iniziale arbitrario in (0, 1)

            // 1. Fase di transitorio: fa stabilizzare il sistema sull'attrattore
            for (int i = 0; i < TRANSIENT_STEPS; i++) {
                x = r * x * (1.0 - x);
            }

            // 2. Fase di disegno: registra e traccia i punti dell'attrattore
            for (int i = 0; i < PLOT_STEPS; i++) {
                x = r * x * (1.0 - x);

                // Mappatura dello stato x [0, 1] al pixel Y (invertito poiché Y=0 è in alto)
                int pixelY = height - (int) (x * height);

                // Disegna un singolo punto
                g2d.fillRect(pixelX, pixelY, 1, 1);
            }
        }

        // Disegna gli assi e le etichette dei parametri
        drawOverlay(g2d, width, height);
    }

    private void drawOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));

        // Testo informativo
        g2d.drawString("Mappa Logistica: x(n+1) = r * x(n) * (1 - x(n))", 20, 30);
        g2d.drawString("r min: " + R_MIN, 20, height - 20);
        g2d.drawString("r max: " + R_MAX, width - 100, height - 20);
        g2d.drawString("Feigenbaum Point ≈ 3.5699 (Inizio del Caos)", (int)(width * 0.64), 30);

        // Linea guida per la soglia del caos
        int chaosThresholdX = (int) (((3.5699 - R_MIN) / (R_MAX - R_MIN)) * width);
        g2d.setColor(new Color(255, 50, 50, 150));
        g2d.drawLine(chaosThresholdX, 0, chaosThresholdX, height);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Diagramma di Biforcazione - Mappa Logistica (May)");
            BifurcationDiagram panel = new BifurcationDiagram();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1024, 700);
            frame.add(panel);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}