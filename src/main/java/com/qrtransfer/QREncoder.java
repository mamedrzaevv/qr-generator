/*
 * Decompiled with CFR 0.152.
 */
package com.qrtransfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class QREncoder
        extends JFrame {
    private JTextArea logArea;
    private JButton selectFileButton;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JSlider speedSlider;
    private JLabel speedLabel;
    private File selectedFile;
    private boolean isRunning = false;
    private Timer displayTimer;
    private List<BufferedImage> qrImages;
    private int currentImageIndex = 0;
    private static final int QR_SIZE = 800;
    private static final int CHUNK_SIZE = 1000;
    private static final int DEFAULT_DISPLAY_TIME = 1000;

    public QREncoder() {
        this.setTitle("QR File Transfer - Encoder");
        this.setDefaultCloseOperation(3);
        this.setSize(900, 700);
        this.setLocationRelativeTo(null);
        this.initComponents();
        this.layoutComponents();
    }

    private void initComponents() {
        this.logArea = new JTextArea();
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font("Monospaced", 0, 12));
        this.selectFileButton = new JButton("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0444\u0430\u0439\u043b");
        this.startButton = new JButton(
                "\u041d\u0430\u0447\u0430\u0442\u044c \u043f\u043e\u043a\u0430\u0437 QR-\u043a\u043e\u0434\u043e\u0432");
        this.stopButton = new JButton("\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c");
        this.statusLabel = new JLabel("\u0413\u043e\u0442\u043e\u0432 \u043a \u0440\u0430\u0431\u043e\u0442\u0435");
        this.speedLabel = new JLabel(
                "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u043f\u043e\u043a\u0430\u0437\u0430 (\u043c\u0441):");
        this.speedSlider = new JSlider(0, 500, 3000, 1000);
        this.speedSlider.setMajorTickSpacing(500);
        this.speedSlider.setMinorTickSpacing(250);
        this.speedSlider.setPaintTicks(true);
        this.speedSlider.setPaintLabels(true);
        this.selectFileButton.addActionListener(e -> this.selectFile());
        this.startButton.addActionListener(e -> this.startDisplay());
        this.stopButton.addActionListener(e -> this.stopDisplay());
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(false);
    }

    private void layoutComponents() {
        this.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(this.selectFileButton);
        topPanel.add(this.startButton);
        topPanel.add(this.stopButton);
        topPanel.add(this.statusLabel);
        JPanel speedPanel = new JPanel(new FlowLayout());
        speedPanel.add(this.speedLabel);
        speedPanel.add(this.speedSlider);
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add((Component) topPanel, "North");
        controlPanel.add((Component) speedPanel, "South");
        this.add((Component) controlPanel, "North");
        JScrollPane scrollPane = new JScrollPane(this.logArea);
        this.add((Component) scrollPane, "Center");
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == 0) {
            this.selectedFile = fileChooser.getSelectedFile();
            this.log("\u0412\u044b\u0431\u0440\u0430\u043d \u0444\u0430\u0439\u043b: " + this.selectedFile.getName());
            this.log("\u0420\u0430\u0437\u043c\u0435\u0440 \u0444\u0430\u0439\u043b\u0430: "
                    + this.formatFileSize(this.selectedFile.length()));
            this.generateQRCodes();
            this.startButton.setEnabled(true);
            this.statusLabel.setText(
                    "\u0424\u0430\u0439\u043b \u0433\u043e\u0442\u043e\u0432 \u043a \u043f\u0435\u0440\u0435\u0434\u0430\u0447\u0435");
        }
    }

    private void generateQRCodes() {
        try {
            this.log(
                    "\u041d\u0430\u0447\u0438\u043d\u0430\u044e \u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044e QR-\u043a\u043e\u0434\u043e\u0432...");
            byte[] fileContent = this.readFile(this.selectedFile);
            this.qrImages = new ArrayList<BufferedImage>();
            HashMap<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("filename", this.selectedFile.getName());
            metadata.put("filesize", this.selectedFile.length());
            metadata.put("chunks", (int) Math.ceil((double) fileContent.length / 1000.0));
            metadata.put("chunkSize", 1000);
            metadata.put("timestamp", System.currentTimeMillis());
            ObjectMapper mapper = new ObjectMapper();
            String metadataJson = mapper.writeValueAsString(metadata);
            BufferedImage metadataQR = this.generateQRCode("METADATA:" + metadataJson);
            this.qrImages.add(metadataQR);
            this.log(
                    "\u0421\u043e\u0437\u0434\u0430\u043d QR-\u043a\u043e\u0434 \u0441 \u043c\u0435\u0442\u0430\u0434\u0430\u043d\u043d\u044b\u043c\u0438");
            int chunkCount = 0;
            for (int i = 0; i < fileContent.length; i += 1000) {
                int endIndex = Math.min(i + 1000, fileContent.length);
                byte[] chunk = Arrays.copyOfRange(fileContent, i, endIndex);
                String base64Chunk = Base64.getEncoder().encodeToString(chunk);
                String qrData = String.format("CHUNK:%d:%s", chunkCount, base64Chunk);
                BufferedImage qrImage = this.generateQRCode(qrData);
                this.qrImages.add(qrImage);
                if (++chunkCount % 10 != 0)
                    continue;
                this.log("\u0421\u043e\u0437\u0434\u0430\u043d\u043e QR-\u043a\u043e\u0434\u043e\u0432: " + chunkCount);
            }
            BufferedImage endQR = this.generateQRCode("END:" + chunkCount);
            this.qrImages.add(endQR);
            this.log(
                    "\u0413\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044f \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430. \u0412\u0441\u0435\u0433\u043e QR-\u043a\u043e\u0434\u043e\u0432: "
                            + this.qrImages.size());
            this.log("\u041c\u0435\u0442\u0430\u0434\u0430\u043d\u043d\u044b\u0435: " + metadataJson);
        } catch (Exception e) {
            this.log(
                    "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u0438 QR-\u043a\u043e\u0434\u043e\u0432: "
                            + e.getMessage());
            e.printStackTrace();
        }
    }

    private BufferedImage generateQRCode(String data) throws WriterException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, (Object) ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, 800, 800, hints);
        BufferedImage image = new BufferedImage(800, 800, 1);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 800, 800);
        graphics.setColor(Color.BLACK);
        for (int x = 0; x < 800; ++x) {
            for (int y = 0; y < 800; ++y) {
                if (!bitMatrix.get(x, y))
                    continue;
                graphics.fillRect(x, y, 1, 1);
            }
        }
        graphics.dispose();
        return image;
    }

    private void startDisplay() {
        if (this.qrImages == null || this.qrImages.isEmpty()) {
            this.log(
                    "\u041d\u0435\u0442 QR-\u043a\u043e\u0434\u043e\u0432 \u0434\u043b\u044f \u043f\u043e\u043a\u0430\u0437\u0430");
            return;
        }
        this.isRunning = true;
        this.currentImageIndex = 0;
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(true);
        final QRDisplayWindow displayWindow = new QRDisplayWindow();
        displayWindow.setVisible(true);
        this.displayTimer = new Timer(this.speedSlider.getValue(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (QREncoder.this.currentImageIndex < QREncoder.this.qrImages.size()) {
                    displayWindow.showQRCode(QREncoder.this.qrImages.get(QREncoder.this.currentImageIndex),
                            QREncoder.this.currentImageIndex + 1, QREncoder.this.qrImages.size());
                    ++QREncoder.this.currentImageIndex;
                    QREncoder.this.statusLabel
                            .setText("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u044e QR-\u043a\u043e\u0434 "
                                    + QREncoder.this.currentImageIndex + " \u0438\u0437 "
                                    + QREncoder.this.qrImages.size());
                } else {
                    QREncoder.this.currentImageIndex = 0;
                    QREncoder.this.log(
                            "\u0426\u0438\u043a\u043b \u043f\u043e\u043a\u0430\u0437\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d, \u043d\u0430\u0447\u0438\u043d\u0430\u044e \u0441\u043d\u0430\u0447\u0430\u043b\u0430");
                }
            }
        });
        this.displayTimer.start();
        this.log(
                "\u041d\u0430\u0447\u0430\u0442 \u043f\u043e\u043a\u0430\u0437 QR-\u043a\u043e\u0434\u043e\u0432. \u0412\u0441\u0435\u0433\u043e: "
                        + this.qrImages.size());
    }

    private void stopDisplay() {
        this.isRunning = false;
        if (this.displayTimer != null) {
            this.displayTimer.stop();
        }
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
        this.statusLabel
                .setText("\u041f\u043e\u043a\u0430\u0437 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d");
        this.log(
                "\u041f\u043e\u043a\u0430\u0437 QR-\u043a\u043e\u0434\u043e\u0432 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d");
    }

    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);) {
            byte[] byArray;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
                int bytesRead;
                byte[] buffer = new byte[8192];
                while ((bytesRead = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byArray = baos.toByteArray();
            }
            return byArray;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 0x100000L) {
            return String.format("%.1f KB", (double) bytes / 1024.0);
        }
        if (bytes < 0x40000000L) {
            return String.format("%.1f MB", (double) bytes / 1048576.0);
        }
        return String.format("%.1f GB", (double) bytes / 1.073741824E9);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            this.logArea.append("[" + String.valueOf(new Date()) + "] " + message + "\n");
            this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QREncoder().setVisible(true));
    }

    private class QRDisplayWindow
            extends JFrame {
        private JLabel imageLabel;
        private JLabel infoLabel;

        public QRDisplayWindow() {
            this.setTitle("QR Code Display");
            this.setDefaultCloseOperation(2);
            this.setSize(850, 900);
            this.setLocationRelativeTo(null);
            this.setAlwaysOnTop(true);
            this.imageLabel = new JLabel();
            this.imageLabel.setHorizontalAlignment(0);
            this.infoLabel = new JLabel();
            this.infoLabel.setHorizontalAlignment(0);
            this.infoLabel.setFont(new Font("Arial", 1, 16));
            this.setLayout(new BorderLayout());
            this.add((Component) this.imageLabel, "Center");
            this.add((Component) this.infoLabel, "South");
        }

        public void showQRCode(BufferedImage image, int current, int total) {
            this.imageLabel.setIcon(new ImageIcon(image));
            this.infoLabel.setText(String.format("QR-\u043a\u043e\u0434 %d \u0438\u0437 %d", current, total));
        }
    }
}
