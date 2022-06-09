package org.example.models.kaligotla_macal_blockchain;

public class Transaction {
    // TODO: Include nonce, maxFeePerGas, maxPriorityFeePerGas, GasLimit, signature, etc.
    int transactionId;                  // unique identifier for a transaction // TODO: This is not in the standard
    long tCreate;                       // time when the transaction is created // TODO: This is not in the standard
    int gas;                            // value received by the miners who verify the transaction (transaction fee)
                                        // TODO: This is not in the standard
    int value;                          // value of the transaction, sent by agent i and received by agent j
    int from;                           // TODO: Replace with Ethereum's address data type
    int to;                             // TODO: Replace with Ethereum's address data type

    public Transaction(long tCreate, int gas, int value, int from, int to, int transactionId)
    {
        this.transactionId = transactionId;
        this.tCreate = tCreate;
        this.gas = gas;
        this.value = value;
        this.from = from;
        this.to = to;
    }

    public boolean isVerified()
    {
        return true; // this.agentI.w >= this.value;    // maybe we should re-think this
    }

    public String toString()
    {
        return "Sender: " + from + ", Receiver: " + to + ", Value:" + value + ", Trans Id:" + transactionId;
    }
}
