package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.graph.Link;

public class Links {
    public static class MarketToMinerLink extends Link.Empty{}
    public static class MarketToMarketLink extends Link.Empty{}     // TODO: Deprecate
    public static class MinerToMarketLink extends Link.Empty{}
    public static class MinerToMinerLink extends Link.Empty{}
    public static class MinerToBlockLink extends Link.Empty{}
}
