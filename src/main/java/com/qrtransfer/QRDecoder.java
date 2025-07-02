/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bytedeco.javacv.FFmpegFrameGrabber
 *  org.bytedeco.javacv.Frame
 *  org.bytedeco.javacv.Java2DFrameConverter
 */
package com.qrtransfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class QRDecoder
extends JFrame {
    private JTextArea logArea;
    private JButton selectVideoButton;
    private JButton startDecodingButton;
    private JButton stopDecodingButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private File selectedVideo;
    private boolean isDecoding = false;
    private Thread decodingThread;
    private Map<String, Object> metadata;
    private Map<Integer, byte[]> chunks;
    private Set<String> processedQRCodes;
    private int totalChunks = 0;
    private int receivedChunks = 0;

    public QRDecoder() {
        this.setTitle("QR File Transfer - Decoder");
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
        this.selectVideoButton = new JButton("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0432\u0438\u0434\u0435\u043e");
        this.startDecodingButton = new JButton("\u041d\u0430\u0447\u0430\u0442\u044c \u0434\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435");
        this.stopDecodingButton = new JButton("\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c");
        this.statusLabel = new JLabel("\u0413\u043e\u0442\u043e\u0432 \u043a \u0440\u0430\u0431\u043e\u0442\u0435");
        this.progressBar = new JProgressBar(0, 100);
        this.progressBar.setStringPainted(true);
        this.selectVideoButton.addActionListener(e -> this.selectVideo());
        this.startDecodingButton.addActionListener(e -> this.startDecoding());
        this.stopDecodingButton.addActionListener(e -> this.stopDecoding());
        this.startDecodingButton.setEnabled(false);
        this.stopDecodingButton.setEnabled(false);
    }

    private void layoutComponents() {
        this.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(this.selectVideoButton);
        topPanel.add(this.startDecodingButton);
        topPanel.add(this.stopDecodingButton);
        topPanel.add(this.statusLabel);
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add((Component)new JLabel("\u041f\u0440\u043e\u0433\u0440\u0435\u0441\u0441:"), "West");
        progressPanel.add((Component)this.progressBar, "Center");
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add((Component)topPanel, "North");
        controlPanel.add((Component)progressPanel, "South");
        this.add((Component)controlPanel, "North");
        JScrollPane scrollPane = new JScrollPane(this.logArea);
        this.add((Component)scrollPane, "Center");
    }

    private void selectVideo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter(){

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".mp4") || f.getName().toLowerCase().endsWith(".avi") || f.getName().toLowerCase().endsWith(".mov");
            }

            @Override
            public String getDescription() {
                return "Video files (*.mp4, *.avi, *.mov)";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == 0) {
            this.selectedVideo = fileChooser.getSelectedFile();
            this.log("\u0412\u044b\u0431\u0440\u0430\u043d \u0432\u0438\u0434\u0435\u043e\u0444\u0430\u0439\u043b: " + this.selectedVideo.getName());
            this.log("\u0420\u0430\u0437\u043c\u0435\u0440 \u0444\u0430\u0439\u043b\u0430: " + this.formatFileSize(this.selectedVideo.length()));
            this.startDecodingButton.setEnabled(true);
            this.statusLabel.setText("\u0412\u0438\u0434\u0435\u043e \u0433\u043e\u0442\u043e\u0432\u043e \u043a \u0434\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044e");
        }
    }

    private void startDecoding() {
        if (this.selectedVideo == null) {
            this.log("\u0412\u0438\u0434\u0435\u043e\u0444\u0430\u0439\u043b \u043d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d");
            return;
        }
        this.isDecoding = true;
        this.startDecodingButton.setEnabled(false);
        this.stopDecodingButton.setEnabled(true);
        this.metadata = new HashMap<String, Object>();
        this.chunks = new HashMap<Integer, byte[]>();
        this.processedQRCodes = new HashSet<String>();
        this.totalChunks = 0;
        this.receivedChunks = 0;
        this.decodingThread = new Thread(() -> {
            try {
                this.decodeVideo();
            }
            catch (Exception e) {
                this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0434\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0438: " + e.getMessage());
                e.printStackTrace();
            }
            finally {
                SwingUtilities.invokeLater(() -> {
                    this.isDecoding = false;
                    this.startDecodingButton.setEnabled(true);
                    this.stopDecodingButton.setEnabled(false);
                });
            }
        });
        this.decodingThread.start();
    }

    private void stopDecoding() {
        this.isDecoding = false;
        if (this.decodingThread != null) {
            this.decodingThread.interrupt();
        }
        this.startDecodingButton.setEnabled(true);
        this.stopDecodingButton.setEnabled(false);
        this.statusLabel.setText("\u0414\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e");
        this.log("\u0414\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u0435\u043c");
    }

    private void decodeVideo() {
        try {
            Frame frame;
            this.log("\u041d\u0430\u0447\u0438\u043d\u0430\u044e \u0434\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0432\u0438\u0434\u0435\u043e...");
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.selectedVideo);
            grabber.start();
            int totalFrames = grabber.getLengthInFrames();
            this.log("\u0412\u0441\u0435\u0433\u043e \u043a\u0430\u0434\u0440\u043e\u0432 \u0432 \u0432\u0438\u0434\u0435\u043e: " + totalFrames);
            int frameCount = 0;
            int processedFrames = 0;
            int[] qrCodesFound = new int[]{0};
            while (this.isDecoding && (frame = grabber.grab()) != null) {
                BufferedImage image;
                String qrData;
                ++frameCount;
                if (frame.image != null && (qrData = this.decodeQRCode(image = this.frameToBufferedImage(frame))) != null && !this.processedQRCodes.contains(qrData)) {
                    qrCodesFound[0] = qrCodesFound[0] + 1;
                    this.log("\u041d\u0430\u0439\u0434\u0435\u043d QR-\u043a\u043e\u0434 #" + qrCodesFound[0] + ": " + qrData.substring(0, Math.min(50, qrData.length())) + "...");
                    this.processQRData(qrData);
                    this.processedQRCodes.add(qrData);
                }
                int currentProcessedFrames = ++processedFrames;
                int progress = (int)((double)currentProcessedFrames / (double)totalFrames * 100.0);
                SwingUtilities.invokeLater(() -> {
                    this.progressBar.setValue(progress);
                    this.statusLabel.setText("\u041e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043e \u043a\u0430\u0434\u0440\u043e\u0432: " + currentProcessedFrames + " \u0438\u0437 " + totalFrames + " | \u041d\u0430\u0439\u0434\u0435\u043d\u043e QR-\u043a\u043e\u0434\u043e\u0432: " + qrCodesFound[0]);
                });
            }
            grabber.stop();
            grabber.release();
            this.log("\u041e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430. \u041d\u0430\u0439\u0434\u0435\u043d\u043e \u0443\u043d\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0445 QR-\u043a\u043e\u0434\u043e\u0432: " + qrCodesFound[0]);
            this.log("\u0420\u0430\u0437\u043c\u0435\u0440 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043d\u044b\u0445 QR-\u043a\u043e\u0434\u043e\u0432: " + this.processedQRCodes.size());
            if (this.isDecoding) {
                if (this.metadata.containsKey("filename") && this.receivedChunks == this.totalChunks) {
                    this.reconstructFile();
                } else {
                    this.log("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0432\u0441\u0435 \u0434\u0430\u043d\u043d\u044b\u0435. \u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043e \u0447\u0430\u043d\u043a\u043e\u0432: " + this.receivedChunks + " \u0438\u0437 " + this.totalChunks);
                    this.log("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u043f\u043e \u0442\u0438\u043f\u0430\u043c QR-\u043a\u043e\u0434\u043e\u0432:");
                    this.log("  - METADATA: " + (this.metadata.containsKey("filename") ? "\u043f\u043e\u043b\u0443\u0447\u0435\u043d\u044b" : "\u041d\u0415 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u044b"));
                    this.log("  - CHUNK: " + this.receivedChunks + " \u0447\u0430\u043d\u043a\u043e\u0432");
                    this.log("  - END: \u043f\u043e\u043b\u0443\u0447\u0435\u043d");
                }
            }
        }
        catch (Exception e) {
            this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0435 \u0432\u0438\u0434\u0435\u043e: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BufferedImage frameToBufferedImage(Frame frame) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }

    private String decodeQRCode(BufferedImage image) {
        try {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            Result result = reader.decode(bitmap);
            return result.getText();
        }
        catch (Exception e) {
            return null;
        }
    }

    private void processQRData(String qrData) {
        if (qrData.startsWith("METADATA:")) {
            this.log("\u041e\u0431\u0440\u0430\u0431\u0430\u0442\u044b\u0432\u0430\u044e METADATA...");
            this.processMetadata(qrData.substring(9));
        } else if (qrData.startsWith("CHUNK:")) {
            this.log("\u041e\u0431\u0440\u0430\u0431\u0430\u0442\u044b\u0432\u0430\u044e CHUNK...");
            this.processChunk(qrData);
        } else if (qrData.startsWith("END:")) {
            this.log("\u041e\u0431\u0440\u0430\u0431\u0430\u0442\u044b\u0432\u0430\u044e END...");
            this.processEnd(qrData);
        } else {
            this.log("\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0442\u0438\u043f QR-\u043a\u043e\u0434\u0430: " + qrData.substring(0, Math.min(20, qrData.length())) + "...");
        }
    }

    private void processMetadata(String metadataJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.metadata = mapper.readValue(metadataJson, Map.class);
            this.totalChunks = (Integer)this.metadata.get("chunks");
            String filename = (String)this.metadata.get("filename");
            Object filesizeObj = this.metadata.get("filesize");
            Long filesize = filesizeObj instanceof Integer ? Long.valueOf(((Integer)filesizeObj).longValue()) : (filesizeObj instanceof Long ? (Long)filesizeObj : Long.valueOf(filesizeObj.toString()));
            this.log("\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u044b \u043c\u0435\u0442\u0430\u0434\u0430\u043d\u043d\u044b\u0435:");
            this.log("  \u0424\u0430\u0439\u043b: " + filename);
            this.log("  \u0420\u0430\u0437\u043c\u0435\u0440: " + this.formatFileSize(filesize));
            this.log("  \u0427\u0430\u043d\u043a\u043e\u0432: " + this.totalChunks);
        }
        catch (Exception e) {
            this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0435 \u043c\u0435\u0442\u0430\u0434\u0430\u043d\u043d\u044b\u0445: " + e.getMessage());
        }
    }

    private void processChunk(String chunkData) {
        try {
            String[] parts = chunkData.split(":", 3);
            if (parts.length == 3) {
                int chunkNumber = Integer.parseInt(parts[1]);
                String base64Data = parts[2];
                byte[] chunkBytes = Base64.getDecoder().decode(base64Data);
                this.chunks.put(chunkNumber, chunkBytes);
                ++this.receivedChunks;
                if (this.receivedChunks % 10 == 0) {
                    this.log("\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043e \u0447\u0430\u043d\u043a\u043e\u0432: " + this.receivedChunks + " \u0438\u0437 " + this.totalChunks);
                }
            }
        }
        catch (Exception e) {
            this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0435 \u0447\u0430\u043d\u043a\u0430: " + e.getMessage());
        }
    }

    private void processEnd(String endData) {
        try {
            String[] parts = endData.split(":");
            if (parts.length == 2) {
                int expectedChunks = Integer.parseInt(parts[1]);
                this.log("\u041f\u043e\u043b\u0443\u0447\u0435\u043d \u0441\u0438\u0433\u043d\u0430\u043b \u043e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u044f. \u041e\u0436\u0438\u0434\u0430\u043b\u043e\u0441\u044c \u0447\u0430\u043d\u043a\u043e\u0432: " + expectedChunks);
            }
        }
        catch (Exception e) {
            this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0435 \u0441\u0438\u0433\u043d\u0430\u043b\u0430 \u043e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u044f: " + e.getMessage());
        }
    }

    private void reconstructFile() {
        try {
            String filename = (String)this.metadata.get("filename");
            Object filesizeObj = this.metadata.get("filesize");
            Long filesize = filesizeObj instanceof Integer ? Long.valueOf(((Integer)filesizeObj).longValue()) : (filesizeObj instanceof Long ? (Long)filesizeObj : Long.valueOf(filesizeObj.toString()));
            this.log("\u041d\u0430\u0447\u0438\u043d\u0430\u044e \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u0444\u0430\u0439\u043b\u0430: " + filename);
            File outputFile = new File("recovered_" + filename);
            try (FileOutputStream fos = new FileOutputStream(outputFile);){
                for (int i = 0; i < this.totalChunks; ++i) {
                    byte[] chunk = this.chunks.get(i);
                    if (chunk == null) {
                        this.log("\u041e\u0428\u0418\u0411\u041a\u0410: \u041e\u0442\u0441\u0443\u0442\u0441\u0442\u0432\u0443\u0435\u0442 \u0447\u0430\u043d\u043a " + i);
                        return;
                    }
                    fos.write(chunk);
                }
            }
            this.log("\u0424\u0430\u0439\u043b \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d: " + outputFile.getAbsolutePath());
            this.log("\u0420\u0430\u0437\u043c\u0435\u0440 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043d\u043e\u0433\u043e \u0444\u0430\u0439\u043b\u0430: " + this.formatFileSize(outputFile.length()));
            JOptionPane.showMessageDialog(this, "\u0424\u0430\u0439\u043b \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d!\n" + outputFile.getAbsolutePath(), "\u0423\u0441\u043f\u0435\u0445", 1);
        }
        catch (Exception e) {
            this.log("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0438 \u0444\u0430\u0439\u043b\u0430: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 0x100000L) {
            return String.format("%.1f KB", (double)bytes / 1024.0);
        }
        if (bytes < 0x40000000L) {
            return String.format("%.1f MB", (double)bytes / 1048576.0);
        }
        return String.format("%.1f GB", (double)bytes / 1.073741824E9);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            this.logArea.append("[" + String.valueOf(new Date()) + "] " + message + "\n");
            this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QRDecoder().setVisible(true));
    }
}
