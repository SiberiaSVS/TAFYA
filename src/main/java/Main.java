import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis();
        SyntaxAnalysis syntaxAnalysis = new SyntaxAnalysis();

        lexicalAnalysis.analysis();
        Tables.printTables();

        syntaxAnalysis.analysis();
    }
}
