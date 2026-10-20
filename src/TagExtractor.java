
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagExtractor extends JFrame {

    private final JLabel sourceLabel =
            new JLabel("Source file: None selected");

    private final JTextArea resultsArea = new JTextArea();

    private final JButton chooseSourceButton =
            new JButton("Choose Text File");

    private final JButton chooseStopWordsButton =
            new JButton("Choose Stop-Words File");

    private final JButton extractButton =
            new JButton("Extract Tags");

    private final JButton saveButton =
            new JButton("Save Results");

    private Path sourceFile;
    private Path stopWordsFile;

    private Map<String, Integer> tagFrequencies =
            new TreeMap<>();

    private Set<String> stopWords = new TreeSet<>();

    public TagExtractor() {
        super("Tag / Keyword Extractor");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topPanel.add(sourceLabel);
        topPanel.add(chooseSourceButton);
        topPanel.add(chooseStopWordsButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(extractButton);
        buttonPanel.add(saveButton);

        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        resultsArea.setText("Choose a text file and a stop-words file, "
                + "then click Extract Tags.");

        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                "Extracted Tags and Frequencies"));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        chooseSourceButton.addActionListener(e -> chooseSourceFile());
        chooseStopWordsButton.addActionListener(e -> chooseStopWordsFile());
        extractButton.addActionListener(e -> extractTags());
        saveButton.addActionListener(e -> saveResults());
    }

    private void chooseSourceFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a Text File to Scan");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sourceFile = chooser.getSelectedFile().toPath();
            sourceLabel.setText("Source file: " + sourceFile.getFileName());
            resultsArea.setText("Source file selected. Choose stop words "
                    + "and click Extract Tags.");
        }
    }

    private void chooseStopWordsFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the Stop-Words File");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            stopWordsFile = chooser.getSelectedFile().toPath();

            try {
                loadStopWords();
                JOptionPane.showMessageDialog(
                        this,
                        "Loaded " + stopWords.size() + " stop words.",
                        "Stop Words Loaded",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException ex) {
                stopWordsFile = null;
                showError("Could not read the stop-words file.", ex);
            }
        }
    }

    private void loadStopWords() throws IOException {
        Set<String> loadedWords = new TreeSet<>();

        try (BufferedReader reader = Files.newBufferedReader(
                stopWordsFile, StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();

                if (word.matches("[a-z]+")) {
                    loadedWords.add(word);
                }
            }
        }

        stopWords = loadedWords;
    }

    private void extractTags() {
        if (sourceFile == null) {
            JOptionPane.showMessageDialog(
                    this, "Please choose a text file first.",
                    "Missing Source File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (stopWordsFile == null) {
            JOptionPane.showMessageDialog(
                    this, "Please choose a stop-words file first.",
                    "Missing Stop-Words File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, Integer> frequencies = new TreeMap<>();

        Pattern wordPattern = Pattern.compile("[a-z]+");

        try (BufferedReader reader = Files.newBufferedReader(
                sourceFile, StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = wordPattern.matcher(line.toLowerCase());

                while (matcher.find()) {
                    String word = matcher.group();

                    if (!stopWords.contains(word)) {
                        frequencies.put(
                                word,
                                frequencies.getOrDefault(word, 0) + 1
                        );
                    }
                }
            }

            tagFrequencies = frequencies;
            displayResults();

        } catch (IOException ex) {
            showError("Could not read the selected text file.", ex);
        }
    }

    private void displayResults() {
        StringBuilder output = new StringBuilder();

        output.append("Source file: ")
                .append(sourceFile.getFileName())
                .append("\n");

        output.append("Unique tags: ")
                .append(tagFrequencies.size())
                .append("\n\n");

        output.append(String.format("%-30s %s%n", "TAG", "FREQUENCY"));
        output.append("------------------------------------------\n");

        for (Map.Entry<String, Integer> entry : tagFrequencies.entrySet()) {
            output.append(String.format(
                    "%-30s %d%n",
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        resultsArea.setText(output.toString());
        resultsArea.setCaretPosition(0);
    }

    private void saveResults() {
        if (sourceFile == null || stopWordsFile == null
                || tagFrequencies.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Extract tags from a text file before saving results.",
                    "Nothing to Save",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Extracted Tags");
        chooser.setSelectedFile(
                new java.io.File("tag-output.txt")
        );

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path outputFile = chooser.getSelectedFile().toPath();

            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputFile, StandardCharsets.UTF_8)) {

                writer.write("Source file: " + sourceFile.getFileName());
                writer.newLine();
                writer.write("Tag\tFrequency");
                writer.newLine();

                for (Map.Entry<String, Integer> entry
                        : tagFrequencies.entrySet()) {
                    writer.write(entry.getKey() + "\t" + entry.getValue());
                    writer.newLine();
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Results saved to:\n" + outputFile,
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (IOException ex) {
                showError("Could not save the results.", ex);
            }
        }
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + ex.getMessage(),
                "File Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TagExtractor app = new TagExtractor();
            app.setVisible(true);
        });
    }
}
