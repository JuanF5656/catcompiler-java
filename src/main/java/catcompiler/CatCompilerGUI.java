package catcompiler;

import catcompiler.interpreter.Interpreter;
import catcompiler.object.CatObject;
import catcompiler.object.ErrorObj;
import catcompiler.object.NullObj;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class CatCompilerGUI extends JFrame {

    private final JTextArea editor = new JTextArea();
    private final JTextArea output = new JTextArea();
    private final JTextArea lineNumbers = new JTextArea("1");

    public CatCompilerGUI() {
        setTitle("CatCompiler - Intérprete");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        // Editor de código
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setText("""
                let x = 10;
                let y = 20;

                print(x + y);

                function cuadrado(n) {
                    return n * n;
                }

                print(cuadrado(5));
                """);

        // Consola de resultados
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        output.setEditable(false);

        // Colores del editor
        editor.setBackground(new Color(30, 30, 30));
        editor.setForeground(new Color(220, 220, 220));
        editor.setCaretColor(Color.WHITE);
        editor.setSelectionColor(new Color(70, 90, 120));

        // Colores de la consola
        output.setBackground(new Color(20, 20, 20));
        output.setForeground(new Color(120, 220, 140));

        // Números de línea
        lineNumbers.setEditable(false);
        lineNumbers.setBackground(new Color(40, 40, 40));
        lineNumbers.setForeground(Color.GRAY);
        lineNumbers.setFont(editor.getFont());
        lineNumbers.setMargin(new Insets(5, 8, 5, 8));

        // Botones
        JButton runButton = new JButton("▶ Ejecutar");
        JButton clearButton = new JButton("Limpiar consola");
        JButton openButton = new JButton("Abrir archivo");
        JButton saveButton = new JButton("Guardar archivo");


        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(runButton);
        top.add(clearButton);
        top.add(openButton);
        top.add(saveButton);

        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setRowHeaderView(lineNumbers);

        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                int lines = editor.getLineCount();
                StringBuilder numbers = new StringBuilder();

                for (int i = 1; i <= lines; i++) {
                    numbers.append(i).append("\n");
                }

                lineNumbers.setText(numbers.toString());
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
        });

        // Dividir editor y consola
        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                createPanel("Código", editorScroll),
                createPanel("Consola", new JScrollPane(output))
        );

        split.setResizeWeight(0.65);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        // Acciones
        runButton.addActionListener(e -> executeCode());
        clearButton.addActionListener(e -> output.setText(""));
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveFile());
    }

    private JPanel createPanel(String title, Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void executeCode() {

        // Capturar lo que imprime print()
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        PrintStream out = new PrintStream(
                buffer, true, StandardCharsets.UTF_8
        );

        BufferedReader in = new BufferedReader(
                new StringReader("")
        );

        // Crear intérprete para esta ejecución
        Interpreter interpreter = new Interpreter(out, in);

        // Ejecutar código
        CatObject result = interpreter.run(editor.getText());

        String printed = buffer.toString(StandardCharsets.UTF_8);

        StringBuilder text = new StringBuilder(printed);

        // Mostrar errores o resultado
        if (result instanceof ErrorObj error) {
            text.append(error.message()).append("\n");

        } else if (result != NullObj.NULL) {
            text.append(result.inspect()).append("\n");
        }

        if (text.length() == 0) {
            text.append("Ejecución finalizada.\n");
        }

        output.append(text.toString());
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(
                new FileNameExtensionFilter("Archivos CatCompiler (*.cat)", "cat")
        );

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(
                        chooser.getSelectedFile().toPath()
                );

                editor.setText(content);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo abrir el archivo: " + ex.getMessage()
                );
            }
        }
    }

    private void saveFile() {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(
                new FileNameExtensionFilter("Archivos CatCompiler (*.cat)", "cat")
        );

        chooser.setSelectedFile(new java.io.File("programa.cat"));

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(".cat")) {
                file = new java.io.File(file.getAbsolutePath() + ".cat");
            }

            try {
                Files.writeString(file.toPath(), editor.getText());

                JOptionPane.showMessageDialog(
                        this,
                        "Archivo guardado correctamente."
                );

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo guardar el archivo: " + ex.getMessage()
                );
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CatCompilerGUI().setVisible(true);
        });
    }
}