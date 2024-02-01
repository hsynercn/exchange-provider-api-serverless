package com.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.datasourceutil.EuropeanCentralBankCurrencySource;
import com.domain.ExchangeData;
import com.domain.ExchangeDataDBHandler;
import com.model.ExchangeRate;
import com.model.ExchangeRateList;
import com.serverless.Handler;
import com.sessiondata.ExchangeDataRepository;
import com.util.GraphCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CurrencyDataFetcher implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOG = LogManager.getLogger(Handler.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        LOG.info("CurrencyDataFetcher triggered");
        ExchangeRateList result = null;
        try {
            result = EuropeanCentralBankCurrencySource.extractData();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            LOG.error(e.getStackTrace());
            e.printStackTrace();
        }
        LOG.info("CurrencyDataFetcher result: " + result.getList());
        String mainCurrencyCode = "USD";
        ArrayList<ExchangeRate> traversalList = GraphCalculator.traversal(result, mainCurrencyCode);

        LOG.info("CurrencyDataFetcher traversalList: " + traversalList);

        ExchangeDataRepository exchangeDataRepository = new ExchangeDataRepository();
        exchangeDataRepository.setDate(result.getDate());
        exchangeDataRepository.setMainCode(mainCurrencyCode);
        exchangeDataRepository.setExchangeRates(traversalList);

        ExchangeData exchangeData = new ExchangeData();
        exchangeData.setExchangeDataJson(exchangeDataRepository.toString());
        exchangeData.setDate(exchangeDataRepository.getDate().toString());
        exchangeData.setMainCode(exchangeDataRepository.getMainCode());
        exchangeData.setInstanceDate(new Date().toString());

        LOG.info("CurrencyDataFetcher exchangeData: " + exchangeData.toString());
        ExchangeDataDBHandler.putItemOne(exchangeData);

        LOG.info("Item stored in DynamoDB");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        return response
                .withStatusCode(200)
                .withBody(exchangeDataRepository.toString());
    }
}
