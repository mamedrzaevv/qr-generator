/*
 * Decompiled with CFR 0.152.
 */
package com.qrtransfer;

import com.qrtransfer.QRDecoder;
import com.qrtransfer.QREncoder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class QRTransferApp
extends JFrame {
    public QRTransferApp() {
        this.setTitle("QR File Transfer System");
        this.setDefaultCloseOperation(3);
        this.setSize(400, 200);
        this.setLocationRelativeTo(null);
        this.initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        JLabel titleLabel = new JLabel("QR File Transfer System", 0);
        titleLabel.setFont(new Font("Arial", 1, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        JButton encoderButton = new JButton("\u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c Encoder (\u041a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435)");
        JButton decoderButton = new JButton("\u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c Decoder (\u0414\u0435\u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435)");
        encoderButton.setFont(new Font("Arial", 0, 14));
        decoderButton.setFont(new Font("Arial", 0, 14));
        encoderButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new QREncoder().setVisible(true)));
        decoderButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new QRDecoder().setVisible(true)));
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        buttonPanel.add(encoderButton);
        buttonPanel.add(decoderButton);
        this.add((Component)titleLabel, "North");
        this.add((Component)buttonPanel, "Center");
        JLabel infoLabel = new JLabel("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0435\u0436\u0438\u043c \u0440\u0430\u0431\u043e\u0442\u044b", 0);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        this.add((Component)infoLabel, "South");
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String mode;
            switch (mode = args[0].toLowerCase()) {
                case "encoder": {
                    SwingUtilities.invokeLater(() -> new QREncoder().setVisible(true));
                    break;
                }
                case "decoder": {
                    SwingUtilities.invokeLater(() -> new QRDecoder().setVisible(true));
                    break;
                }
                default: {
                    QRTransferApp.showMainWindow();
                    break;
                }
            }
        } else {
            QRTransferApp.showMainWindow();
        }
    }

    private static void showMainWindow() {
        SwingUtilities.invokeLater(() -> new QRTransferApp().setVisible(true));
    }
}
