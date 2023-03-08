package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws StockQuoteServiceException {
    String tiingoURL = buildURL(symbol, from, to);
    String responseString=null;
    try {
      responseString = restTemplate.getForObject(tiingoURL, String.class);
    } catch (HttpClientErrorException e) {
        throw new StockQuoteServiceException ("TooManyRequests: 429 Unknown Status Code");
    }
    
    TiingoCandle[] tiingoCandleArray;
    try {
      tiingoCandleArray = getObjectMapper().readValue(responseString, TiingoCandle[].class);
      if (tiingoCandleArray == null || responseString == null)
        throw new StockQuoteServiceException ("Invalid Response Found");
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException (e.getMessage());
    }
    return Arrays.stream(tiingoCandleArray).sorted(Comparator.comparing(Candle::getDate))
        .collect(Collectors.toList());
  }


  protected String buildURL(String symbol, LocalDate startDate, LocalDate endDate) {

    String uriTemplate = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?" + "startDate="
        + startDate + "&endDate=" + endDate + "&token=" + getToken();
    return uriTemplate;
  }

  protected String getToken() {    
    String token = "dfa8419def308a98ab86069ad43d8cc64a0cb431";
    return token;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

}
