import javax.swing.*;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class URLPinger extends JFrame {
    private JTextField urlField;
    private JButton pingButton;
    private JLabel resultLabel;
    private JSpinner attemptsSpinner;
    private JSpinner timeoutSpinner;
    private JComboBox<String> methodComboBox;
    private JCheckBox followRedirectsCheckbox;
    private JTextArea responseArea;
    private JPanel advancedPanel;

    public URLPinger() {
        setTitle("Advanced URL Pinger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Create main components
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        urlField = new JTextField();
        pingButton = new JButton("Ping");
        resultLabel = new JLabel("Enter a URL and click Ping");
        
        // Create advanced options panel
        advancedPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // HTTP Method selector
        methodComboBox = new JComboBox<>(new String[]{"GET", "POST", "HEAD", "OPTIONS"});
        addComponent(advancedPanel, new JLabel("Method:"), gbc, 0, 0);
        addComponent(advancedPanel, methodComboBox, gbc, 1, 0);
        
        // Number of attempts
        SpinnerNumberModel attemptsModel = new SpinnerNumberModel(3, 1, 10, 1);
        attemptsSpinner = new JSpinner(attemptsModel);
        addComponent(advancedPanel, new JLabel("Attempts:"), gbc, 0, 1);
        addComponent(advancedPanel, attemptsSpinner, gbc, 1, 1);
        
        // Timeout setting
        SpinnerNumberModel timeoutModel = new SpinnerNumberModel(5000, 1000, 30000, 1000);
        timeoutSpinner = new JSpinner(timeoutModel);
        addComponent(advancedPanel, new JLabel("Timeout (ms):"), gbc, 0, 2);
        addComponent(advancedPanel, timeoutSpinner, gbc, 1, 2);
        
        // Follow redirects option
        followRedirectsCheckbox = new JCheckBox("Follow Redirects", true);
        gbc.gridwidth = 2;
        addComponent(advancedPanel, followRedirectsCheckbox, gbc, 0, 3);
        
        // Response area
        responseArea = new JTextArea(10, 40);
        responseArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseArea);
        
        // Add padding and borders
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        advancedPanel.setBorder(BorderFactory.createTitledBorder("Advanced Options"));
        
        // Add components to the panels
        inputPanel.add(new JLabel("URL:"), BorderLayout.WEST);
        inputPanel.add(urlField, BorderLayout.CENTER);
        inputPanel.add(pingButton, BorderLayout.EAST);
        
        // Create main panel layout
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(advancedPanel, BorderLayout.CENTER);
        
        // Add components to the frame
        add(mainPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(resultLabel, BorderLayout.SOUTH);
        
        // Add action listener
        pingButton.addActionListener(e -> pingURL());
        
        // Set window size and center it
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // Make the Enter key work too
        urlField.addActionListener(e -> pingURL());
    }
    
    private void addComponent(JPanel panel, JComponent component, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        panel.add(component, gbc);
    }
    
    private void pingURL() {
        String urlStr = urlField.getText().trim();
        if (urlStr.isEmpty()) {
            resultLabel.setText("Please enter a URL");
            return;
        }
        
        // Add http:// if no protocol is specified
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = "http://" + urlStr;
        }
        
        int attempts = (Integer) attemptsSpinner.getValue();
        int timeout = (Integer) timeoutSpinner.getValue();
        String method = (String) methodComboBox.getSelectedItem();
        boolean followRedirects = followRedirectsCheckbox.isSelected();
        
        java.util.List<Long> durations = new ArrayList<>();
        StringBuilder response = new StringBuilder();
        
        try {
            URL url = new URL(urlStr);
            
            for (int i = 0; i < attempts; i++) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                connection.setInstanceFollowRedirects(followRedirects);
                
                long startTime = System.currentTimeMillis();
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                durations.add(duration);
                
                // First attempt: collect headers and response info
                if (i == 0) {
                    response.append("Response Code: ").append(responseCode).append("\n\n");
                    response.append("Headers:\n");
                    connection.getHeaderFields().forEach((key, value) -> {
                        if (key != null) {
                            response.append(key).append(": ").append(value.get(0)).append("\n");
                        }
                    });
                    
                    if (method.equals("GET")) {
                        response.append("\nResponse Body (first 1000 chars):\n");
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()))) {
                            char[] buffer = new char[1000];
                            reader.read(buffer);
                            response.append(new String(buffer));
                        }
                    }
                }
                
                connection.disconnect();
            }
            
            // Calculate statistics
            long min = Collections.min(durations);
            long max = Collections.max(durations);
            double avg = durations.stream().mapToLong(Long::longValue).average().getAsDouble();
            
            resultLabel.setText(String.format("Min: %d ms, Max: %d ms, Avg: %.1f ms", min, max, avg));
            responseArea.setText(response.toString());
            
        } catch (Exception ex) {
            resultLabel.setText("Error: " + ex.getMessage());
            responseArea.setText("Error occurred: " + ex.toString());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new URLPinger().setVisible(true);
        });
    }
}
