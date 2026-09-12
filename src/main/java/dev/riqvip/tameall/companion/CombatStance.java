package dev.riqvip.tameall.companion;

/** Combat intent is deliberately independent from movement/task mode. */
public enum CombatStance {
    /** Never acquire a target, including retaliation. */
    PASSIVE,
    /** Attack only things the owner or companion is actively fighting, or that attacked them. */
    ASSIST,
    /** Proactively scan the operating area for eligible targets. */
    DEFEND_AREA;

    public static CombatStance defaultStance() {
        return ASSIST;
    }
}
