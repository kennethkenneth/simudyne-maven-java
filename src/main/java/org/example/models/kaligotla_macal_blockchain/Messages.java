package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.graph.Message;

public class Messages {
    public static class candidateTransactionMessage extends Message.Empty {
        public MarketAgent sender;
        public int gas;
        public int value;
        public long createTick;
    }

    public static class broadcastTransactionsToMinersPTQ extends Message.Empty{
        public int gas;
        public MarketAgent sender;
        public MarketAgent receiver;
        public int value;
        public long createTick;
    }

    public static class broadcastBlockToLedgers extends Message.Empty {
        public Block block;
    }

    public static class broadcastBlockToMiners extends Message.Empty {
        public Block block;
    }
}
