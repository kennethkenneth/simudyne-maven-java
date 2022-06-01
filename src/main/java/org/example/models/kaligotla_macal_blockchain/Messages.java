package org.example.models.kaligotla_macal_blockchain;

import org.example.models.schelling.AgentState;
import simudyne.core.graph.Message;

public class Messages {
    public static class candidateTransactionMessage extends Message.Empty {
        //public Transaction t;
        public MarketAgent sender;
        //public MinerAgent receiver;
        public int gas;
        public int value;
        public long createTick;
    }

    public static class TransferAmount extends Message {
        public int amount;
        public MarketAgent receiver;
    }
    public static class SubstractAmount extends Message {
        public int amount;
        public MarketAgent sender;
    }
}