package domain;

import java.net.URI;

/** Esito di successo: indirizzo della pagina con il grafico (D6). Payload qak: done(URL). */
public record Done(URI page) implements Outcome {
    public Done {
        if (page == null) throw new IllegalArgumentException("page obbligatoria");
    }
}
