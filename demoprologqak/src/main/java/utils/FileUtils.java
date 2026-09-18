package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    /**
     * Legge un file di testo e ne restituisce il contenuto completo come String.
     *
     * @param percorsoFile Percorso del file da leggere.
     * @return Il contenuto del file come String.
     * @throws IOException Se si verifica un errore durante la lettura del file.
     */
    public static String leggiFile(String percorsoFile) throws IOException {
        return Files.readString(Path.of(percorsoFile));
    }

    public static void main(String[] args) {
        try {
        	String CurrentDir = System.getProperty("user.dir");
            String contenuto = leggiFile(CurrentDir+"/src/main/java/utils/logisticmap.txt");
            System.out.println(contenuto);
        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file: " + e.getMessage());
        }
    }
}
