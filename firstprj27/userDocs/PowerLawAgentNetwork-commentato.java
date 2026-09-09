package MyCode.leggePotenza;

import java.util.*;

// NOTA AGGIUNTA: questa classe rappresenta un singolo "agente" della simulazione,
// cioè un'entità che partecipa alla rete e scambia messaggi/interazioni con gli
// altri agenti. Implementa Comparable per poter essere ordinata facilmente
// (vedi compareTo() più sotto).
class Agent implements Comparable<Agent> {
    private final int id;
    private final int targetInteractions; // Frequenza stimata/capacità di interazione
    private int currentInteractions = 0;
    // NOTA AGGIUNTA: "targetInteractions" è un valore FISSO, deciso una volta sola
    // alla creazione dell'agente (estratto dalla power law): rappresenta quante
    // interazioni quell'agente "dovrebbe" avere secondo il modello statistico.
    // "currentInteractions" invece è un CONTATORE che si aggiorna nel tempo,
    // pensato per tenere traccia di quante interazioni sono REALMENTE avvenute
    // durante la simulazione (nel codice attuale viene incrementato da
    // incrementInteractions(), ma quel metodo non viene mai chiamato nel main:
    // è quindi un "aggancio" predisposto per estensioni future della simulazione).

    public Agent(int id, int targetInteractions) {
        this.id = id;
        this.targetInteractions = targetInteractions;
    }

    public int getId() { return id; }
    public int getTargetInteractions() { return targetInteractions; }
    public int getCurrentInteractions() { return currentInteractions; }
    public void incrementInteractions() { this.currentInteractions++; }

    @Override
    public int compareTo(Agent o) {
        return Integer.compare(o.targetInteractions, this.targetInteractions);
    }
    // NOTA AGGIUNTA: questo compareTo() è scritto "al contrario" rispetto
    // all'ordine naturale: normalmente Integer.compare(this.x, o.x) ordina in
    // modo CRESCENTE. Qui invece si confronta o.targetInteractions con
    // this.targetInteractions, quindi l'ordinamento risulta DECRESCENTE:
    // gli agenti con più interazioni previste finiscono in cima alla lista.
    // È un trucco comune per ottenere un ordinamento decrescente senza dover
    // usare un Comparator separato.
}

public class PowerLawAgentNetwork {

    private final Random random = new Random();

    /**
     * Genera un intero distribuito secondo una Power Law usando Inverse Transform Sampling.
     */
    // NOTA AGGIUNTA: una "power law" (legge di potenza) è una distribuzione
    // statistica in cui pochissimi elementi hanno valori altissimi e moltissimi
    // elementi hanno valori bassi (es. pochi "hub" molto attivi, tanti nodi
    // periferici quasi inattivi: fenomeno tipico dei social network, del web,
    // delle citazioni scientifiche, ecc.). "Inverse Transform Sampling" è una
    // tecnica generale per generare numeri casuali che seguono UNA distribuzione
    // qualsiasi (qui la power law), partendo semplicemente da un numero casuale
    // uniforme tra 0 e 1 (quello che random.nextDouble() ci dà già "gratis").
    public int nextPowerLawInt(double xMin, double alpha) {
        double u = random.nextDouble(); // Uniforme in [0, 1)
        // Evita divisioni per zero se u è esattamente 1.0
        // NOTA AGGIUNTA: se u fosse 1.0, il calcolo sotto (Math.pow(1.0 - u, ...))
        // diventerebbe Math.pow(0, esponente negativo), che produce infinito
        // (divisione per zero implicita) e romperebbe la simulazione. Questo
        // ciclo semplicemente "ripesca" un nuovo numero casuale finché non è
        // diverso da 1.0 (evento comunque rarissimo con nextDouble()).
        while (u == 1.0) {
            u = random.nextDouble();
        }
        double value = xMin * Math.pow(1.0 - u, -1.0 / (alpha - 1.0));
        // NOTA AGGIUNTA: questa è la formula matematica (CDF inversa) della
        // power law. In parole povere:
        // - "xMin" è il valore minimo possibile (qui: minimo 1 interazione);
        // - "alpha" (detto anche esponente della power law) controlla quanto la
        //   distribuzione è "sbilanciata": più alpha è vicino a 2, più è
        //   probabile ottenere valori grandi (coda lunga pesante); più alpha
        //   cresce, più i valori tendono a restare piccoli e vicini a xMin.
        return (int) Math.round(value);
        // NOTA AGGIUNTA: il valore generato è un numero decimale (double); viene
        // arrotondato all'intero più vicino perché qui rappresenta un NUMERO DI
        // INTERAZIONI, che ha senso solo come valore intero.
    }

    public static void main(String[] args) {
        PowerLawAgentNetwork sim = new PowerLawAgentNetwork();

        int totalAgents = 100;
        double xMin = 1.0;   // Minimo 1 interazione
        double alpha = 2.1;  // Valore tipico per le reti sociali/complessità

        List<Agent> agents = new ArrayList<>();

        // 1. Inizializzazione degli Agenti con Frequenze Power Law
        // NOTA AGGIUNTA: qui vengono creati 100 agenti (id da 0 a 99), a ognuno
        // dei quali viene assegnata una "frequenza target" estratta casualmente
        // dalla power law appena definita. È qui che nasce la disuguaglianza tra
        // agenti: per pura casualità statistica, pochi avranno numeri altissimi
        // (i futuri "hub" della rete) e la maggior parte avrà numeri bassi.
        for (int i = 0; i < totalAgents; i++) {
            int targetInteractions = sim.nextPowerLawInt(xMin, alpha);
            agents.add(new Agent(i, targetInteractions));
        }

        // Ordina gli agenti dal più attivo al meno attivo per la visualizzazione
        Collections.sort(agents);
        // NOTA AGGIUNTA: questo metodo usa automaticamente il compareTo()
        // definito nella classe Agent, quindi la lista risulterà ordinata in
        // modo DECRESCENTE (dal valore di targetInteractions più alto al più
        // basso), come confermato dal commento originale qui sopra.

        // 2. Reportistica sulla distribuzione generata
        System.out.println("===================================================================");
        System.out.println("     DISTRIBUZIONE POWER LAW DELLE INTERAZIONI (Alpha = " + alpha + ")");
        System.out.println("===================================================================");
        System.out.printf("%-10s %-25s %-30s%n", "Agent ID", "Interazioni Previste", "Visualizzazione (Istogramma)");
        System.out.println("-------------------------------------------------------------------");
        // NOTA AGGIUNTA: "%-10s" ecc. sono specifiche di formattazione di
        // System.out.printf: il "-" allinea il testo a sinistra, il numero
        // indica la larghezza minima in caratteri della colonna (per ottenere
        // un output "tabellare" allineato, come si vede nell'esempio di output
        // riportato in fondo al file).

        for (int i = 0; i < Math.min(15, agents.size()); i++) {
            Agent a = agents.get(i);
            String bar = "*".repeat(Math.min(a.getTargetInteractions(), 40));
            // NOTA AGGIUNTA: costruisce una "barra" fatta di asterischi per
            // rappresentare graficamente (istogramma testuale) quante
            // interazioni ha l'agente. Il Math.min(..., 40) evita che agenti
            // con numeri altissimi (es. 70) producano righe troppo lunghe da
            // leggere: la barra viene "tagliata" a un massimo di 40 asterischi.
            System.out.printf("%-10d %-25d %-30s%n", a.getId(), a.getTargetInteractions(), bar);
        }
        System.out.println("...");
        // NOTA AGGIUNTA: questo primo ciclo stampa solo i PRIMI 15 agenti della
        // lista ordinata, cioè quelli con più interazioni previste (i futuri
        // "hub" della rete): la classica "testa" della distribuzione a power law.
        
        // Stampa anche gli ultimi 5 agenti (la coda lunga)
        for (int i = agents.size() - 5; i < agents.size(); i++) {
            Agent a = agents.get(i);
            String bar = "*".repeat(Math.min(a.getTargetInteractions(), 40));
            System.out.printf("%-10d %-25d %-30s%n", a.getId(), a.getTargetInteractions(), bar);
        }
        // NOTA AGGIUNTA: questo secondo ciclo stampa invece gli ULTIMI 5 agenti
        // della lista, cioè quelli con il valore di targetInteractions più
        // basso: rappresentano la "coda lunga" tipica della power law, cioè la
        // grande maggioranza di agenti poco attivi (spesso con una sola
        // interazione prevista, come si vede nell'esempio di output in fondo).

        // 3. Esempio di applicazione: Selezione dell'interlocutore tramite Preferential Attachment
        // NOTA AGGIUNTA: "Preferential Attachment" (attaccamento preferenziale)
        // è un concetto delle reti complesse secondo cui i nuovi collegamenti
        // tendono a formarsi PREFERIBILMENTE verso i nodi già molto connessi
        // ("i ricchi diventano più ricchi"): è proprio ciò che simula il metodo
        // selectTargetWeightedByPowerLaw() più sotto.
        System.out.println("\nSimulazione di routing: Selezione degli Hub per invio messaggi...");
        Agent source = agents.get(agents.size() - 1); // Un agente periferico
        // NOTA AGGIUNTA: essendo la lista ordinata in modo decrescente,
        // l'ULTIMO elemento (agents.size() - 1) è l'agente con il valore di
        // targetInteractions più basso, cioè un agente "periferico" (poco attivo).
        Agent target = sim.selectTargetWeightedByPowerLaw(agents);
        
        System.out.println("L'Agente " + source.getId() + " (periferico) avvia un'interazione con l'Agente " 
                           + target.getId() + " (Target con peso/frequenza: " + target.getTargetInteractions() + ")");
        // NOTA AGGIUNTA: questa parte finale è puramente dimostrativa: mostra
        // come un agente periferico "sceglie" con chi interagire, usando una
        // selezione pesata che favorisce statisticamente gli hub più attivi
        // (vedi il metodo subito sotto per il dettaglio dell'algoritmo).
    }

    /**
     * Seleziona un agente target con probabilità proporzionale alla sua frequenza di interazione.
     */
    // NOTA AGGIUNTA: questo metodo implementa la tecnica nota come "roulette
    // wheel selection" (selezione a ruota della fortuna): immagina una ruota
    // divisa in spicchi, uno per agente, dove la larghezza dello spicchio è
    // proporzionale al peso (targetInteractions) di quell'agente. Si "lancia la
    // pallina" (randomWeight) e si vede in quale spicchio cade.
    public Agent selectTargetWeightedByPowerLaw(List<Agent> agents) {
        int totalWeight = agents.stream().mapToInt(Agent::getTargetInteractions).sum();
        // NOTA AGGIUNTA: somma tutti i pesi (targetInteractions) di tutti gli
        // agenti: rappresenta la "lunghezza totale della ruota".
        int randomWeight = random.nextInt(totalWeight);
        // NOTA AGGIUNTA: sceglie un punto casuale lungo questa "ruota" (un
        // intero tra 0 e totalWeight - 1): è il punto in cui "cade la pallina".

        int currentSum = 0;
        for (Agent agent : agents) {
            currentSum += agent.getTargetInteractions();
            if (currentSum > randomWeight) {
                return agent;
            }
            // NOTA AGGIUNTA: si scorrono gli agenti accumulando i pesi uno alla
            // volta; non appena la somma accumulata supera il punto casuale
            // scelto sopra, si è trovato "in quale spicchio" cade la pallina,
            // e quell'agente viene restituito come target. Gli agenti con peso
            // più alto (targetInteractions maggiore) occupano "spicchi" più
            // larghi, quindi hanno più probabilità di essere scelti: è proprio
            // il meccanismo di Preferential Attachment descritto sopra.
        }
        return agents.get(0);
        // NOTA AGGIUNTA: questa riga è un "fallback" di sicurezza, teoricamente
        // mai raggiunto se il ciclo funziona correttamente (perché randomWeight
        // è sempre < totalWeight, quindi il ciclo dovrebbe sempre trovare e
        // restituire un agente prima di terminare naturalmente). È comunque una
        // buona pratica difensiva: il metodo deve restituire un Agent in ogni
        // possibile percorso del codice, altrimenti il compilatore Java darebbe
        // errore.
    }
}

/*
===================================================================
DISTRIBUZIONE POWER LAW DELLE INTERAZIONI (Alpha = 2.1)
===================================================================
Agent ID   Interazioni Previste      Visualizzazione (Istogramma)  
-------------------------------------------------------------------
92         70                        ****************************************
73         62                        ****************************************
6          22                        **********************        
10         21                        *********************         
62         18                        ******************            
26         13                        *************                 
74         11                        ***********                   
95         10                        **********                    
50         9                         *********                     
94         8                         ********                      
79         7                         *******                       
32         6                         ******                        
0          5                         *****                         
86         5                         *****                         
11         4                         ****                          
...
80         1                         *                             
82         1                         *                             
87         1                         *                             
89         1                         *                             
91         1                         *                             

Simulazione di routing: Selezione degli Hub per invio messaggi...
L'Agente 91 (periferico) avvia un'interazione con l'Agente 73 (Target con peso/frequenza: 62)
*/

C:\Didattica2026\protobookmaterial\contentLab\source\_static\img\GitHub26\DiagrammPowerLawAgentNetwork.png

Il diagramma mostra visivamente il meccanismo di `selectTargetWeightedByPowerLaw`: ogni agente occupa uno "spicchio" 
della barra proporzionale al proprio peso (`targetInteractions`), e un punto scelto a caso tra 0 e la somma totale 
dei pesi (560 nell'esempio) determina quale agente viene selezionato — proprio come nell'output del programma, 
		dove l'agente 91 finisce per interagire con l'agente 73, uno degli hub con peso maggiore.

I numeri sotto la barra (0, 190, 360, 470, 560) rappresentano la **somma cumulativa** dei pesi: è esattamente 
il valore che il ciclo `currentSum` calcola passo passo nel codice, confrontandolo con `randomWeight` 
per capire "in quale spicchio" è caduto il punto casuale.

