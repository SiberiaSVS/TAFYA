import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class UI extends JFrame{
    private JButton analysisButton;
    private JButton codeRedactorButton;
    private JPanel mainPanel;
    private JTextArea logArea;
    private JTextArea tableArea;
    private JTextArea lexemesArea;

    public UI() {
        setContentPane(mainPanel);
        setTitle("Анализатор");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800,600);
        setLocationRelativeTo(null);
        setVisible(true);

        analysisButton.addActionListener(e -> {
            logArea.setText("");
            tableArea.setText("");
            lexemesArea.setText("");
            LexicalAnalysis lexicalAnalysis = new LexicalAnalysis();

            try {
                lexicalAnalysis.analysis();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Tables.printTables();
        });
        codeRedactorButton.addActionListener(e -> {
            if(Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File("program.txt"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void log(String message){
        logArea.append(message + "\n");
    }

    public void printInTableArea(String string) {
        tableArea.append(string + "\n");
    }

    public void printInTableArea() {
        tableArea.append("\n");
    }

    public void addLexemeInLexemesArea(String string) {
        lexemesArea.append(string);
    }
}
