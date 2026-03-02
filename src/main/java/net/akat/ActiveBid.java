package net.akat;

import java.util.UUID;

public class ActiveBid {
    public final UUID playerUUID;
    public final String bidId;
    public final int amount;

    public ActiveBid(UUID playerUUID, String bidId, int amount) {
        this.playerUUID = playerUUID;
        this.bidId = bidId;
        this.amount = amount;
    }
}
