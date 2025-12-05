package com.garden.sim;

import com.garden.sim.api.GertenSimulationImpl;
import org.junit.jupiter.api.Test;

public class SmokeTest {
    @Test
    void canInitializeAndInvokeAPI() {
        var api = new GertenSimulationImpl();
        api.initializeGarden();
        api.rain(10);
        api.temperature(85);
        api.parasite("aphid");
        api.getState();
    }
}
