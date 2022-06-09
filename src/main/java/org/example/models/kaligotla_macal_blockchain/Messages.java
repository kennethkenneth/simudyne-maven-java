package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.graph.Message;

public class Messages {
    public static class candidateTransactionMessage extends Message.Empty {
        public int from;
        public int to;
        public int gas;
        public int value;
        public long createTick;
    }

    public static class broadcastTransactionsToMinersPTQ extends Message.Empty{
        public int gas;
        public int from;
        public int to;
        public int value;
        public long createTick;
    }

    public static class broadcastBlockToLedgers extends Message.Empty {
        public Block block;
    }

    public static class broadcastCandidateBlockToMiners extends Message.Empty {
        public Block block;
    }
}
