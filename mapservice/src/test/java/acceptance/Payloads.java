package acceptance;

import java.net.URI;
import unibo.basicomm23.interfaces.IApplMessage;

/** Estrae i dati dai payload dichiarati nel modello: done(URL), failed(CAUSE). */
final class Payloads {
    private Payloads() {}

    /** done('http://...') -> URI */
    static URI url(IApplMessage reply) {
        String c = reply.msgContent().trim();                 // done('http://host:port/...')
        if (!c.startsWith("done(") || !c.endsWith(")"))
            throw new IllegalArgumentException("payload inatteso: " + c);
        String arg = c.substring(5, c.length() - 1).trim();
        if (arg.startsWith("'") && arg.endsWith("'")) arg = arg.substring(1, arg.length() - 1);
        return URI.create(arg);
    }
}
