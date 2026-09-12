package dev.riqvip.tameall.companion;

/** The mutually exclusive movement behavior a bonded creature performs. */
public enum CompanionMode {
    FOLLOW,
    STAY,
    GUARD;

    /** Returns the mode used for a newly bonded mob. */
    public static CompanionMode defaultMode() {
        return FOLLOW;
    }
}
