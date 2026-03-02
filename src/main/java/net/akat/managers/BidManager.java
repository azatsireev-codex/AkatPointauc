package net.akat.managers;

import net.akat.ActiveBid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BidManager {
    private final Map<String, ActiveBid> activeBids = new ConcurrentHashMap<>();

    public void addBid(ActiveBid bid) {
        activeBids.put(bid.bidId, bid);
    }

    public void removeBid(String bidId) {
        activeBids.remove(bidId);
    }

    public Map<String, ActiveBid> getActiveBids() {
        return activeBids;
    }
}
