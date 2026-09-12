package dev.riqvip.tameall.companion;

/** Result of the server-side Golden Wheat interaction. */
public record BondingResult(Status status, CompanionState state) {
    public enum Status {
        BONDED,
        NOT_LIVING,
        EXCLUDED_BOSS,
        ALREADY_BONDED,
        OWNED_BY_OTHER,
        INVALID_OWNER
    }

    public boolean succeeded() {
        return status == Status.BONDED;
    }
}
