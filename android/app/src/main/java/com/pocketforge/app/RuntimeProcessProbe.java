package com.pocketforge.app;

/**
 * Fixed, side-effect-free entry point for the Preview ART child-process probe.
 * This runs under Android Runtime (ART), not a standalone desktop JDK.
 */
public final class RuntimeProcessProbe {
    private RuntimeProcessProbe() { }

    public static void main(String[] args) {
        System.out.println("POCKETFORGE_ART_RUNTIME_OK");
        System.out.println("java.version=" + System.getProperty("java.version", "unknown"));
        System.out.println("java.vm.name=" + System.getProperty("java.vm.name", "unknown"));
        System.out.println("java.vm.version=" + System.getProperty("java.vm.version", "unknown"));
        System.out.println("os.name=" + System.getProperty("os.name", "unknown"));
        System.out.flush();
    }
}
