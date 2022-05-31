import org.example.models.GaiKapadia.GaiKapadiaModel;
import org.example.models.SimudyneSIR.SimudyneSIR;
import org.example.models.TumorGrowthSimulator.TumorGrowthModel;
import org.example.models.cda.CDAModel;
import org.example.models.creditCard.CreditCardModel;
import org.example.models.forestFire.ForestFireModel;
import org.example.models.mortgage.MortgageModel;
import org.example.models.schelling.SchellingModel;
import org.example.models.tokyo.TokyoModel;
import org.example.models.trading.TradingModel;
import org.example.models.volatilityModel.VolatilityModel;
import org.example.models.kaligotla_macal_blockchain.BlockchainModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    Server.register("Trading Model", TradingModel.class);
    Server.register("Mortgage Model", MortgageModel.class);
    Server.register("Credit Card Model", CreditCardModel.class);
    Server.register("Continuous Double Auction Model", CDAModel.class);
    Server.register("Volatility Model", VolatilityModel.class);
    Server.register("Chain Bankruptcy Model", TokyoModel.class);
    Server.register("S.I.R. Model", SimudyneSIR.class);
    Server.register("Tumor Growth Model", TumorGrowthModel.class);
    Server.register("Schelling Segregation Model", SchellingModel.class);
    Server.register("Forest Fire Model", ForestFireModel.class);
    Server.register("Gai-Kapadia Model", GaiKapadiaModel.class);
    Server.register("Kaligotla-Macal Blockchain Model", BlockchainModel.class);

    Server.run(args);
  }
}
