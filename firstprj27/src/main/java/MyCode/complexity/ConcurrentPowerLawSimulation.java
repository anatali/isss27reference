package MyCode.complexity;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 Per trasformare la simulazione in un sistema multi-agente concorrente reale, 
 ogni agente deve essere eseguito su un proprio Thread (o gestito tramite Virtual Threads introdotte in Java 21).

 L'architettura ideale prevede un modello Actor-like basato su scambi di messaggi asincroni: 
 ogni agente possiede una coda di posta privata (BlockingQueue) e un riferimento agli altri agenti 
 o a un Message Broker centrale.
 
1) Agente come Runnable: Ogni agente esegue un loop continuo in cui:

	- Decide se avviare una nuova interazione (con frequenza regolata dalla Power Law).
	- Processa i messaggi ricevuti nella propria coda.

2) Scelta del Target tramite Preferential Attachment: Gli agenti con frequenza Power Law più alta 
   sono scelti più spesso come destinatari.

3) Sincronizzazione thread-safe: Uso di LinkedBlockingQueue per la comunicazione non bloccante 
   e AtomicInteger per il tracciamento delle metriche senza race condition.
*/

// Rappresenta un messaggio scambiato tra agenti
record Message(int senderId, String payload) {}

class ConcurrentAgent implements Runnable {
    private final int id;
    private final int targetInteractions; // Valore Power Law
    private final BlockingQueue<Message> mailbox = new LinkedBlockingQueue<>();
    private final List<ConcurrentAgent> network;
    private final Random random = new Random();
    
    // Metriche globali e locali
    private final AtomicInteger interactionsDone = new AtomicInteger(0);
    private final AtomicInteger messagesReceived = new AtomicInteger(0);
    private volatile boolean running = true;

    public ConcurrentAgent(int id, int targetInteractions, List<ConcurrentAgent> network) {
        this.id = id;
        this.targetInteractions = targetInteractions;
        this.network = network;
    }

    public int getId() { return id; }
    public int getTargetInteractions() { return targetInteractions; }
    public int getMessagesReceived() { return messagesReceived.get(); }
    public int getInteractionsDone() { return interactionsDone.get(); }

    public void receiveMessage(Message msg) {
        mailbox.offer(msg);
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running && interactionsDone.get() < targetInteractions) {
            try {
                // 1. Processa eventuali messaggi in ingresso (Non bloccante con timeout)
                Message incoming = mailbox.poll(10, TimeUnit.MILLISECONDS);
                if (incoming != null) {
                    messagesReceived.incrementAndGet();
                }

                // 2. Tenta di avviare una nuova interazione uscente
                if (random.nextDouble() < 0.3) { // Probabilità di azione per ciclo
                    ConcurrentAgent target = selectTargetByPowerLaw();
                    if (target != null && target.getId() != this.id) {
                        target.receiveMessage(new Message(this.id, "PING"));
                        interactionsDone.incrementAndGet();
                    }
                }

                // Pausa per simulare il tempo di elaborazione dell'agente
                Thread.sleep(random.nextInt(20) + 10);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Selezione del target ponderata sulla Power Law degli altri agenti.
     */
    private ConcurrentAgent selectTargetByPowerLaw() {
        int totalWeight = network.stream().mapToInt(ConcurrentAgent::getTargetInteractions).sum();
        if (totalWeight == 0) return null;

        int randomWeight = random.nextInt(totalWeight);
        int currentSum = 0;

        for (ConcurrentAgent agent : network) {
            currentSum += agent.getTargetInteractions();
            if (currentSum > randomWeight) {
                return agent;
            }
        }
        return network.get(0);
    }
}

public class ConcurrentPowerLawSimulation {

    // Generatore Power Law (Inverse Transform Sampling)
    public static int nextPowerLawInt(double xMin, double alpha, Random rnd) {
        double u = rnd.nextDouble();
        while (u == 1.0) u = rnd.nextDouble();
        return (int) Math.round(xMin * Math.pow(1.0 - u, -1.0 / (alpha - 1.0)));
    }

    public static void main(String[] args) throws InterruptedException {
        int totalAgents = 50;
        double xMin = 5.0;   // Minimo 5 interazioni
        double alpha = 2.0;  // Esponente Power Law
        Random rnd = new Random();

        List<ConcurrentAgent> agents = new CopyOnWriteArrayList<>();
        
        // 1. Inizializzazione degli agenti con frequenze Power Law
        for (int i = 0; i < totalAgents; i++) {
            int powerLawCapacity = nextPowerLawInt(xMin, alpha, rnd);
            agents.add(new ConcurrentAgent(i, powerLawCapacity, agents));
        }

        // 2. Avvio dei Thread per ogni Agente (Uso di ExecutorService / Virtual Threads)
        // Se usi Java 21+: Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        System.out.println("Avvio della simulazione concorrente con " + totalAgents + " agenti...");
        long startTime = System.currentTimeMillis();

        for (ConcurrentAgent agent : agents) {
            executor.submit(agent);
        }

        // 3. Attesa del completamento del lavoro
        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);

        if (!finished) {
            System.out.println("Timeout raggiunto. Arresto forzato degli agenti...");
            agents.forEach(ConcurrentAgent::stop);
            executor.shutdownNow();
        }

        long duration = System.currentTimeMillis() - startTime;

        // 4. Analisi dei risultati: verifica dell'effetto HUB
        agents.sort(Comparator.comparingInt(ConcurrentAgent::getTargetInteractions).reversed());

        System.out.println("\n=========================================================================");
        System.out.println("          RISULTATI DELLA SIMULAZIONE CONCORRENTE (" + duration + " ms)");
        System.out.println("=========================================================================");
        System.out.printf("%-10s %-20s %-20s %-20s%n", "Agent ID", "Capacità (PowerLaw)", "Inviati", "Ricevuti (Hub Effect)");
        System.out.println("-------------------------------------------------------------------------");

        // Mostra i primi 10 (gli Hub) e gli ultimi 3 (la periferia)
        for (int i = 0; i < Math.min(10, agents.size()); i++) {
            ConcurrentAgent a = agents.get(i);
            System.out.printf("%-10d %-20d %-20d %-20d%n", 
                    a.getId(), a.getTargetInteractions(), a.getInteractionsDone(), a.getMessagesReceived());
        }
        System.out.println("...");
        for (int i = agents.size() - 3; i < agents.size(); i++) {
            ConcurrentAgent a = agents.get(i);
            System.out.printf("%-10d %-20d %-20d %-20d%n", 
                    a.getId(), a.getTargetInteractions(), a.getInteractionsDone(), a.getMessagesReceived());
        }
    }
}
/*
Avvio della simulazione concorrente con 50 agenti...
Timeout raggiunto. Arresto forzato degli agenti...

=========================================================================
          RISULTATI DELLA SIMULAZIONE CONCORRENTE (15012 ms)
=========================================================================
Agent ID   Capacità (PowerLaw)  Inviati              Ricevuti (Hub Effect)
-------------------------------------------------------------------------
35         800                  92                   402                 
6          468                  127                  236                 
11         189                  145                  72                  
34         110                  108                  55                  
43         45                   45                   17                  
12         40                   40                   12                  
24         39                   39                   11                  
13         38                   38                   9                   
7          37                   37                   4                   
17         37                   37                   9                   
...
39         6                    6                    3                   
23         5                    5                    2                   
27         5                    5                    1                 
  
Cosa Notare nei Risultati Concorrenti

- Emergenza degli HUB (Colonna Ricevuti): Gli agenti in cima alla tabella (con Capacità alta) 
  riceveranno un numero sproporzionatamente elevato di messaggi da parte di tutti gli altri agenti.

- Isolamento della Periferia: Gli agenti in fondo alla coda invieranno pochissimi messaggi e 
  ne riceveranno ancora meno, riflettendo esattamente la dinamica delle reti complessi reali 
  (es. Twitter/X o reti di citazioni).

- Virtual Threads (Java 21+): Se la simulazione scala a migliaia di agenti, 
  sostituisci Executors.newFixedThreadPool(...) con Executors.newVirtualThreadPerTaskExecutor(). 
  Questo ti permette di eseguire anche 100.000 agenti concorrenti contemporaneamente senza saturare la RAM.  
*/