package com.inote.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CaptchaImageRenderer {

    private static final int WIDTH = 168;
    private static final int HEIGHT = 56;
    private static final Font[] FONTS = new Font[] {
            new Font("SansSerif", Font.BOLD, 34),
            new Font("Dialog", Font.BOLD, 34),
            new Font("Serif", Font.BOLD, 34)
    };

    private final SecureRandom secureRandom = new SecureRandom();

    public String renderAsDataUri(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            configure(graphics);
            paintBackground(graphics);
            paintNoise(graphics);
            paintGuides(graphics);
            paintCharacters(graphics, code);
            paintSpray(graphics, code);
            paintBorder(graphics);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render captcha image.", e);
        }
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void paintBackground(Graphics2D graphics) {
        graphics.setPaint(new GradientPaint(0, 0, new Color(250, 250, 250), WIDTH, HEIGHT, new Color(236, 239, 244)));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void paintNoise(Graphics2D graphics) {
        for (int i = 0; i < 180; i++) {
            int size = 1 + secureRandom.nextInt(3);
            int x = secureRandom.nextInt(WIDTH);
            int y = secureRandom.nextInt(HEIGHT);
            int alpha = 20 + secureRandom.nextInt(50);
            graphics.setColor(new Color(90, 100, 120, alpha));
            graphics.fill(new Ellipse2D.Double(x, y, size, size));
        }
    }

    private void paintGuides(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 2; i++) {
            graphics.setColor(new Color(75, 85, 99, 85));
            double y1 = 12 + secureRandom.nextInt(HEIGHT - 24);
            double y2 = 12 + secureRandom.nextInt(HEIGHT - 24);
            graphics.draw(new CubicCurve2D.Double(
                    -8,
                    y1,
                    40 + secureRandom.nextInt(30),
                    secureRandom.nextInt(HEIGHT),
                    WIDTH - 65 - secureRandom.nextInt(25),
                    secureRandom.nextInt(HEIGHT),
                    WIDTH + 8,
                    y2));
        }
    }

    private void paintCharacters(Graphics2D graphics, String code) {
        int slotWidth = (WIDTH - 32) / code.length();
        int baseline = 38;

        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            Font font = FONTS[secureRandom.nextInt(FONTS.length)].deriveFont(Font.BOLD, 33f + secureRandom.nextFloat() * 3f);
            graphics.setFont(font);

            double x = 16 + (i * slotWidth) + secureRandom.nextInt(7) - 3;
            double y = baseline + secureRandom.nextInt(9) - 4;
            double angle = (secureRandom.nextDouble() - 0.5) * 0.4;

            graphics.setColor(new Color(27, 30, 38));
            graphics.translate(x, y);
            graphics.rotate(angle);
            graphics.drawString(String.valueOf(ch), 0, 0);
            graphics.rotate(-angle);
            graphics.translate(-x, -y);

            paintCharacterSpray(graphics, x, y, slotWidth);
        }
    }

    private void paintCharacterSpray(Graphics2D graphics, double x, double y, int slotWidth) {
        Graphics2D overlay = (Graphics2D) graphics.create();
        try {
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            for (int i = 0; i < 28; i++) {
                int dx = secureRandom.nextInt(slotWidth + 12) - 6;
                int dy = secureRandom.nextInt(26) - 13;
                int size = 1 + secureRandom.nextInt(2);
                int alpha = 35 + secureRandom.nextInt(90);
                overlay.setColor(new Color(30, 35, 45, alpha));
                overlay.fill(new Rectangle2D.Double(x + dx, y + dy, size, size));
            }
        } finally {
            overlay.dispose();
        }
    }

    private void paintSpray(Graphics2D graphics, String code) {
        Graphics2D overlay = (Graphics2D) graphics.create();
        try {
            overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            for (int i = 0; i < code.length() * 60; i++) {
                int x = secureRandom.nextInt(WIDTH);
                int y = secureRandom.nextInt(HEIGHT);
                int size = 1 + secureRandom.nextInt(2);
                overlay.setColor(new Color(20, 25, 30, 40 + secureRandom.nextInt(60)));
                overlay.fill(new Ellipse2D.Double(x, y, size, size));
            }
        } finally {
            overlay.dispose();
        }
    }

    private void paintBorder(Graphics2D graphics) {
        graphics.setColor(new Color(133, 146, 166, 120));
        graphics.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 12, 12);
    }
}
