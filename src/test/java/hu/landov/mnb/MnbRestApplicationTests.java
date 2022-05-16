package hu.landov.mnb;

import hu.landov.mnb.rest.MnbRestApplication;
import hu.landov.mnb.rest.controller.MnbRestController;
import hu.landov.mnb.rest.dto.error.ErrorDetail;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MnbRestApplication.class)
public class MnbRestApplicationTests {

    @LocalServerPort
    private int port;

    private static final String HOST ="http://localhost:";
    private static final String PATH ="/mnb/currencies";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MnbRestController controller;

    @Test
    //Context existence
    public void contextLoads() throws  Exception{
        assertThat(controller).isNotNull();
    }

   @Test
    //404 page displayed
    public void wrongPath() throws  Exception{
        assertThat(this.restTemplate.getForObject(HOST+port+"/",String.class))
                .contains("Not Found");
    }

    @Test
    //Currencies listed
    public void currenciesListed() throws Exception{
        List<LinkedHashMap> rates = this.restTemplate.getForObject(HOST+port+PATH, List.class);
        assertThat(rates.get(0).get("currency").equals("EUR"));
    }

    @Test
    //Get a single currency
    public void getUsdCurrency() throws Exception{
        ExchangeRate exchangeRate = this.restTemplate.getForObject(HOST+port+PATH+"/USD", ExchangeRate.class);
        assertThat(exchangeRate.getCurrency().equals("USD"));
    }

    @Test
    //Get a single currency on a given date
    public void getAudOnDate() throws Exception{
        ExchangeRate exchangeRate = this.restTemplate.getForObject(HOST+port+PATH+"/AUD?date=2022-05-13", ExchangeRate.class);
        assertThat(exchangeRate.getRate()==254.22);
    }

    @Test
    //Get a single currency with a malformed date
    public void getAudWithWrongDate() throws Exception{
        ErrorDetail errorDetail = this.restTemplate.getForObject(HOST+port+PATH+"/AUD?date=aaa", ErrorDetail.class);
        assertThat(errorDetail.getDetail().equals("aaa can't be parsed as date. Expected format YYYY-MM-DD"));
    }

    @Test
    //Get a non-existing currency
    public void getNonExisting() throws Exception{
        ErrorDetail errorDetail = this.restTemplate.getForObject(HOST+port+PATH+"/XXX", ErrorDetail.class);
        assertThat(errorDetail.getDetail().equals("Currency: XXX cant' be found"));
    }

    @Test
    //Get on holiday w/o lastknown
    public void getOnHolyday() throws Exception{
        ErrorDetail errorDetail = this.restTemplate.getForObject(HOST+port+PATH+"/RUB?date=2021-12-25", ErrorDetail.class);
        assertThat(errorDetail.getStatus() == 404);
    }

    @Test
    //Get on holiday w lastknown
    public void getOnHolydayLastknown() throws Exception{
        ExchangeRate exchangeRate = this.restTemplate.getForObject(HOST+port+PATH+"/RUB?date=2021-12-25&lastknown=true", ExchangeRate.class);
        assertThat(exchangeRate.getDate().equals("2021-12-23"));
    }

    @Test
    //Get a history of currency
    public void getHistory() throws Exception {
        List<LinkedHashMap> rates = this.restTemplate.getForObject(HOST+port+PATH+"/USD/history?from=2022-01-01&to=2022-05-10", List.class);
        assertThat(rates.size() == 88);
    }






}
