package MyCode.complexity;

import java.util.*;

class Agent implements Comparable<Agent> {
    private final int id;
    private final int targetInteractions; // Frequenza stimata/capacità di interazione
    private int currentInteractions = 0;

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
}

public class PowerLawAgentNetwork {

    private final Random random = new Random();

    /**
     * Genera un intero distribuito secondo una Power Law usando Inverse Transform Sampling.
     */
    public int nextPowerLawInt(double xMin, double alpha) {
        double u = random.nextDouble(); // Uniforme in [0, 1)
        // Evita divisioni per zero se u è esattamente 1.0
        while (u == 1.0) {
            u = random.nextDouble();
        }
        double value = xMin * Math.pow(1.0 - u, -1.0 / (alpha - 1.0));
        return (int) Math.round(value);
    }

    public static void main(String[] args) {
        PowerLawAgentNetwork sim = new PowerLawAgentNetwork();

        int totalAgents = 100;
        double xMin = 1.0;   // Minimo 1 interazione
        double alpha = 2.1;  // Valore tipico per le reti sociali/complessità

        List<Agent> agents = new ArrayList<>();

        // 1. Inizializzazione degli Agenti con Frequenze Power Law
        for (int i = 0; i < totalAgents; i++) {
            int targetInteractions = sim.nextPowerLawInt(xMin, alpha);
            agents.add(new Agent(i, targetInteractions));
        }

        // Ordina gli agenti dal più attivo al meno attivo per la visualizzazione
        Collections.sort(agents);

        // 2. Reportistica sulla distribuzione generata
        System.out.println("===================================================================");
        System.out.println("     DISTRIBUZIONE POWER LAW DELLE INTERAZIONI (Alpha = " + alpha + ")");
        System.out.println("===================================================================");
        System.out.printf("%-10s %-25s %-30s%n", "Agent ID", "Interazioni Previste", "Visualizzazione (Istogramma)");
        System.out.println("-------------------------------------------------------------------");

        for (int i = 0; i < Math.min(15, agents.size()); i++) {
            Agent a = agents.get(i);
            String bar = "*".repeat(Math.min(a.getTargetInteractions(), 40));
            System.out.printf("%-10d %-25d %-30s%n", a.getId(), a.getTargetInteractions(), bar);
        }
        System.out.println("...");
        
        // Stampa anche gli ultimi 5 agenti (la coda lunga)
        for (int i = agents.size() - 5; i < agents.size(); i++) {
            Agent a = agents.get(i);
            String bar = "*".repeat(Math.min(a.getTargetInteractions(), 40));
            System.out.printf("%-10d %-25d %-30s%n", a.getId(), a.getTargetInteractions(), bar);
        }

        // 3. Esempio di applicazione: Selezione dell'interlocutore tramite Preferential Attachment
        System.out.println("\nSimulazione di routing: Selezione degli Hub per invio messaggi...");
        Agent source = agents.get(agents.size() - 1); // Un agente periferico
        Agent target = sim.selectTargetWeightedByPowerLaw(agents);
        
        System.out.println("L'Agente " + source.getId() + " (periferico) avvia un'interazione con l'Agente " 
                           + target.getId() + " (Target con peso/frequenza: " + target.getTargetInteractions() + ")");
    }

    /**
     * Seleziona un agente target con probabilità proporzionale alla sua frequenza di interazione.
     */
    public Agent selectTargetWeightedByPowerLaw(List<Agent> agents) {
        int totalWeight = agents.stream().mapToInt(Agent::getTargetInteractions).sum();
        int randomWeight = random.nextInt(totalWeight);

        int currentSum = 0;
        for (Agent agent : agents) {
            currentSum += agent.getTargetInteractions();
            if (currentSum > randomWeight) {
                return agent;
            }
        }
        return agents.get(0);
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