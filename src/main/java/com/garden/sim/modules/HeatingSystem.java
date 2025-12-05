package com.garden.sim.modules;

import com.garden.sim.core.*;

public class HeatingSystem {
    private final Logger log;
    private int targetMin = 60;

    public HeatingSystem(Logger log) {
        this.log = log;
    }

    public int mitigate(int currentF) {
        if (currentF < targetMin) {
            int lift = Math.min(10, targetMin - currentF);
            log.info("HeatingSystem mitigates +" + lift + "F");
            return currentF + lift;
        }
        return currentF;
    }
}
