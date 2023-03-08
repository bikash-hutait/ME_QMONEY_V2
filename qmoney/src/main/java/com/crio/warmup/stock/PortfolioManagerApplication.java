package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {


   public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
  
      List<String> listOfSymbols=new ArrayList<>();
      ObjectMapper objectMapper = getObjectMapper();
      File inputTrade = resolveFileFromResources(args[0]);  
      PortfolioTrade[] trades = objectMapper.readValue(inputTrade, PortfolioTrade[].class);    
      for (PortfolioTrade trade : trades) {
        listOfSymbols.add(trade.getSymbol());
      }
       return listOfSymbols;
    }


    
    private static void printJsonObject(Object object) throws IOException {
      Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
      ObjectMapper mapper = new ObjectMapper();
      logger.info(mapper.writeValueAsString(object));
    }
  
    private static File resolveFileFromResources(String filename) throws URISyntaxException {
      return Paths.get(
          Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
    }
  
    private static ObjectMapper getObjectMapper() {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
    }
  
    public static List<PortfolioTrade> readTradesFromJson(String filename)
    throws IOException, URISyntaxException {
  List<PortfolioTrade> portfolioTrades = getObjectMapper().readValue(
      resolveFileFromResources(filename), new TypeReference<List<PortfolioTrade>>() {});
  return portfolioTrades;
}



private static String readFileAsString(String fileName) throws IOException, URISyntaxException {
  return new String(Files.readAllBytes(resolveFileFromResources(fileName).toPath()), "UTF-8");
}

  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    final String apiToken = "dfa8419def308a98ab86069ad43d8cc64a0cb431";

    // Get FILENAME from command line
    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    // Get Enddate from command line
    LocalDate endDate = LocalDate.parse(args[1]);

    RestTemplate restTemplate = new RestTemplate();
    List<TotalReturnsDto> totalReturnsDtos = new ArrayList<>();
    List<String> listOfSortedSymbolsClosingPrice = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      String tiingoEndpoint = prepareUrl(portfolioTrade, endDate, apiToken);

      TiingoCandle[] tiingoCandleArray = restTemplate.getForObject(tiingoEndpoint, TiingoCandle[].class);
      totalReturnsDtos.add(new TotalReturnsDto(portfolioTrade.getSymbol(),
          tiingoCandleArray[tiingoCandleArray.length - 1].getClose()));
    }

    // Shorting Data
    Collections.sort(totalReturnsDtos,
        (a, b) -> Double.compare(a.getClosingPrice(), b.getClosingPrice()));

    for (TotalReturnsDto totalReturnsDto : totalReturnsDtos) {
      listOfSortedSymbolsClosingPrice.add(totalReturnsDto.getSymbol());
    }
    return listOfSortedSymbolsClosingPrice;
  }


  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    return "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
        + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    RestTemplate restTemplate = new RestTemplate();
    String tiingoRestURL = prepareUrl(trade, endDate, token);
    TiingoCandle[] tiingoCandleArray =
        restTemplate.getForObject(tiingoRestURL, TiingoCandle[].class);
    return Arrays.stream(tiingoCandleArray).collect(Collectors.toList());
  }


  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {

       
    double total_num_years = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }


  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@d29f28";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";


   return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
       toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
       lineNumberFromTestFileInStackTrace});
 }

 static Double getOpeningPriceOnStartDate(List<Candle> candles) {
  return candles.get(0).getOpen();
}


public static Double getClosingPriceOnEndDate(List<Candle> candles) {
  return candles.get(candles.size() - 1).getClose();
}

public static String getToken() {
  return "dfa8419def308a98ab86069ad43d8cc64a0cb431";
}

public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
    throws IOException, URISyntaxException {
  List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
  List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
  
  // Tiingo API Token
  final String apiToken = "dfa8419def308a98ab86069ad43d8cc64a0cb431";
  
  // Geting date from comandline args
  LocalDate localDate = LocalDate.parse(args[1]);

  for (PortfolioTrade portfolioTrade : portfolioTrades) {
    List<Candle> candles = fetchCandles(portfolioTrade, localDate, apiToken);
    AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(localDate, portfolioTrade,
        getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
    annualizedReturns.add(annualizedReturn);
  }
  return annualizedReturns.stream()
      .sorted((a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()))
      .collect(Collectors.toList());
}

  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.


  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
    List<PortfolioTrade> portfolioTrades = objectMapper.readValue(contents, new TypeReference<List<PortfolioTrade>>() {});
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }

// Testing New Impementation Via PortfolioManager & StockQuotesService
public static List<AnnualizedReturn> mainCalculateReturnsAfterNewServiceProvider(String[] args)
throws Exception {
String file = args[0];
LocalDate endDate = LocalDate.parse(args[1]);
String contents = readFileAsString(file);
ObjectMapper objectMapper = getObjectMapper();
PortfolioManager portfolioManager =
  PortfolioManagerFactory.getPortfolioManager("tingoo",new RestTemplate());
List<PortfolioTrade> portfolioTrades =
  objectMapper.readValue(contents, new TypeReference<List<PortfolioTrade>>() {});
// List<Candle> candles =
// ((PortfolioManagerImpl)portfolioManager).getStockQuote(portfolioTrade.getSymbol(),
// portfolioTrade.getPurchaseDate(), endDate);

List<AnnualizedReturn> anReturn =
  portfolioManager.calculateAnnualizedReturnParallel(portfolioTrades, endDate,10);
return anReturn;
}

  public static void main(String[] args) throws Exception {
    //printJsonObject(mainCalculateReturnsAfterRefactor(new String[] {"trades.json", "2020-01-01"}));
     // printJsonObject(mainCalculateReturnsAfterNewServiceProvider(new String[] {"trades.json", "2020-01-01"}));

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());


}

}

