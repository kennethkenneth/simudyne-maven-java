package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;

public final class Globals extends GlobalState {

    PublicLedger pl = new PublicLedger();

    /*list of relative gas levels agents can choose from to indicate the priority of a transaction
    highGas = 10, mediumGas = 5, lowGas = 2 */
    @Input(name = "Gas Fee (Ξ)")
    public int gasFee = 5;

    @Input(name = "Delta T (sec.)")
    public double deltaT = 15; //the time increment between the Blockchain system updates (secs.)

    @Input(name = "Simulation Time (sec.)")
    public double simTime = 10*60; // we simulate a Blockchain for a period of 10 minutes

    //@Input(name = "Network Reward (Ξ)")
    //public int networkReward = 5; // network reward split among the miner agents who
    // verify a block of transactions (?)

    @Input(name = "Agents needed to Verify Trans.")
    public int agentsToVerifyTrans = 5; // number of miner agents the network requires to verify a block (?)

    @Input(name = "Block Length")
    public int blockLength = 5; //number of transactions in a block from the PTQ that is used
                                // to form a candidate block of transactions to verify. 2000???

    @Input(name = "Number of Agents")
    public int numAgents = 100; // Number of Agents in the simulation (?)

    @Input(name = "Fraction of Miner Agents")
    public float fracMiners = (float) 0.10; // Fraction of agents who are miner agents

    /*
    tBegin is the beginning time for the simulation. tEnd is the ending time for the simulation.
    scenarioResults is the log of results for the simulation run.
    */

    // TODO: Find out numbers used in the actual paper
    @Input(name = "Initial Transactions")
    public int numInitialTransactions = 40;

    @Input(name = "min Transactions every 15 sec.")
    public int minTransactions = 17; // 100,000 / 24 / 60 / 4

    @Input(name = "max Transactions every 15 sec.")
    public int maxTransactions = 70; // 500,000 / 24 / 60 / 4

    @Input(name = "Initial Transactions")
    public float probAgentTransacting = (float) (maxTransactions-minTransactions) / numAgents;

    @Input(name = "Initial Market Agent balance (Ξ)")
    public int initialMarketAgentBalance = 1000;

    @Input(name = "Initial Miner Agent balance (Ξ)")
    public int initialMinerAgentBalance = 1000;
}