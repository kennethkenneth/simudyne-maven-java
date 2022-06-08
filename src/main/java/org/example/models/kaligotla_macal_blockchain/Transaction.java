package org.example.models.kaligotla_macal_blockchain;

public class Transaction {
    int transactionId;                // unique identifier for a transaction
    long tCreate;           // time when the transaction is created
    long tVerify;           // time when the transaction is verified
    int gas;                // value received by the miners who verify the transaction (transaction fee)
    int value;              // value of the transaction, sent by agent i and received by agent j
    //MarketAgent agentI;     // sender of value in the transaction
    //MarketAgent agentJ;     // receiver of value in the transaction
    int senderAddress;
    int receiverAddress;
    boolean transferDone;

    //public Transaction(long tCreate, int gas, int value, MarketAgent agentI, MarketAgent agentJ, int transactionId)
    public Transaction(long tCreate, int gas, int value, int senderAddress, int receiverAddress, int transactionId)
    {
        this.transactionId = transactionId;
        this.tCreate = tCreate;
        this.gas = gas;
        this.value = value;
        //this.agentI = agentI;
        //this.agentJ = agentJ;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
        this.transferDone = false;
    }

    public boolean isTransferDone()
    {
        return this.transferDone;
    }

    public void markTransferAsDone()
    {
        this.transferDone = true;
    }

    public boolean isVerified()
    {
        return true; // this.agentI.w >= this.value;    // maybe we should re-think this
    }
}
