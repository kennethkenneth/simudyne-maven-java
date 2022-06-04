package org.example.models.kaligotla_macal_blockchain;

public class Transaction {
    // transaction[idt,agent_i,agent_j,tCreate,tVerify,gas,value]
    int transactionId;                // unique identifier for a transaction
    long tCreate;           // time when the transaction is created
    long tVerify;           // time when the transaction is verified
    int gas;                // value received by the miners who verify the transaction (transaction fee)
    int value;              // value of the transaction, sent by agent i and received by agent j
    MarketAgent agentI;     // sender of value in the transaction
    MarketAgent agentJ;     // receiver of value in the transaction

    boolean transferDone;

    public Transaction(long tCreate, int gas, int value, MarketAgent agentI, MarketAgent agentJ, int transactionId)
    {
        this.transactionId = transactionId;
        this.tCreate = tCreate;
        this.gas = gas;
        this.value = value;
        this.agentI = agentI;
        this.agentJ = agentJ;
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
        return this.agentI.w >= this.value;
    }
}
