package org.example.models.kaligotla_macal_blockchain;

public class Transaction {
    // transaction[idt,agent_i,agent_j,tCreate,tVerify,gas,value]
    int idT;                // unique identifier for a transaction
    long tCreate;           // time when the transaction is created
    long tVerify;           // time when the transaction is verified
    int gas;                // value received by the miners who verify the transaction (transaction fee)
    int value;              // value of the transaction, sent by agent i and received by agent j
    MarketAgent agentI;     // sender of value in the transaction
    MarketAgent agentJ;     // receiver of value in the transaction

    boolean transferDone;

    int verify;             // 0: not verified; 1: verified. Also denoted as sigma.

    public Transaction(long tCreate, int gas, int value, MarketAgent agentI, MarketAgent agentJ)
    {
        this.tCreate = tCreate;
        this.gas = gas;
        this.value = value;
        this.agentI = agentI;
        this.agentJ = agentJ;
        this.verify = 0;
        this.transferDone = false;
    }

    public void markTransferAsDone()
    {
        this.transferDone = true;
    }

    public boolean isVerified()
    {
        if (this.agentI.w >= this.value) // + this.gas)
        {
            this.verify = 1;
            return true;
        }
        this.verify = 0;
        return false;
    }
}
