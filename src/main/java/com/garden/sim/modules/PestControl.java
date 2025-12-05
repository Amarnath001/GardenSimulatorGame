package com.garden.sim.modules;

import com.garden.sim.core.*;
import java.util.*;

public class PestControl {
    private final Garden garden;
    private final Logger log;
    private final Random rnd = new Random();

    public PestControl(Garden garden, Logger log) {
        this.garden = garden; this.log = log;
    }

    public void dailySweep() {
        int cures = 0;
        int totalInfestations = 0;
        for (Plant p : garden.getPlantsList()) {
            if (p.isDead()) continue;
            Set<String> parasites = new HashSet<>(p.getParasites());
            totalInfestations += parasites.size();
            for (String parasite : parasites) {
                int eff = 50 + rnd.nextInt(51); // 50..100
                int before = p.getParasites().size();
                p.cure(parasite, eff);
                if (p.getParasites().size() < before) {
                    cures++;
                    log.info("PestControl: cured " + parasite + " on " + p.getName() + " (efficacy: " + eff + "%)");
                }
            }
        }
        if (totalInfestations > 0) {
            log.info("PestControl: treated " + totalInfestations + " infestations, cured " + cures + " (success rate: " + 
                     String.format("%.1f", totalInfestations > 0 ? (100.0 * cures / totalInfestations) : 0) + "%)");
        }
    }
}
