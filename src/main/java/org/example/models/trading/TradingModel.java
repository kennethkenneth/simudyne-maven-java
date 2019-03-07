package org.example.models.trading;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 100)
public class TradingModel extends AgentBasedModel<TradingModel.Globals> {

  public static final class Globals extends GlobalState {
    @Input(name = "Update Frequency")
    double updateFrequency = 0.01;

    @Constant(name = "Number of Traders")
    long nbTraders = 1000;

    @Input(name = "Lambda")
    double lambda = 10;

    @Input(name = "Volatility of Information Signal")
    double volatilityInfo = 0.001;

    double informationSignal;
  }

  {
    createLongAccumulator("buys", "Number of buy orders");
    createLongAccumulator("sells", "Number of sell orders");
    createDoubleAccumulator("price", "Price");
  }

  @Override
  public void init() {
    getGlobals().informationSignal =
        getContext().getPRNG().generator.nextGaussian() * getGlobals().volatilityInfo;
  }

  @Override
  public void setup() {
    Group<Trader> traderGroup = generateGroup(Trader.class, getGlobals().nbTraders);
    Group<Market> marketGroup = generateGroup(Market.class, 1);

    traderGroup.fullyConnected(marketGroup, Links.TradeLink.class);
    marketGroup.fullyConnected(traderGroup, Links.TradeLink.class);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    getGlobals().informationSignal =
        getContext().getPRNG().generator.nextGaussian() * getGlobals().volatilityInfo;

    run(Trader.processInformation(), Market.calcPriceImpact(), Trader.updateThreshold());
  }
}
