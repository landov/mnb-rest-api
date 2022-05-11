package hu.landov.mnb.rest.controller;

import hu.landov.mnb.ExchangeRate;
import hu.landov.mnb.MNBWebserviceFacade;
import hu.landov.mnb.MNBWebserviceFacadeException;
import hu.landov.mnb.rest.dto.StoredRate;
import hu.landov.mnb.rest.dto.error.ErrorDetail;
import hu.landov.mnb.rest.exception.InvalidRequestException;
import hu.landov.mnb.rest.exception.MnbServerErrorException;
import hu.landov.mnb.rest.exception.ResourceNotFoundException;
import hu.landov.mnb.rest.repository.StoredRateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerErrorException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//TODO Error handling
@RestController
@CrossOrigin(origins = "*")
public class MnbRestController {

    private MNBWebserviceFacade facade;
    //Stored currenciy IDs for error checking
    private List<String> currencies;

    @Inject
    private StoredRateRepository storedRateRepository;

    private static final String DATE_DESCRIPTION = "Date (optional) for which exchange rate to be retrieved. Default the last available date.";
    private static final String DATE_EXAMPLE = "2019-09-16";
    private static final String TARGET_DESCRIPTION = "Target currency (optional) default HUF.";
    private static final String LASTKNOWN_DESCRIPTION = "Set to true: If there is no exchange rate for the given date, get the last known one.";

    {
        try {
            facade = new MNBWebserviceFacade();
        } catch (MNBWebserviceFacadeException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            currencies = facade.getCurrencies();
        } catch (MNBWebserviceFacadeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @RequestMapping(value = "/mnb/symbols", method = RequestMethod.GET, produces = {"application/json"})
    @Operation(summary = "Retrieves all available currency symbols.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucessful operation",
                    content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))}),
            @ApiResponse(responseCode = "500", description = "Server error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))})
    })
    public ResponseEntity<Iterable<String>> getSymbols() {
        try {
            Iterable<String> currencies = facade.getCurrencies();
            return new ResponseEntity<>(currencies, HttpStatus.OK);
        } catch (MNBWebserviceFacadeException e) {
            throw new MnbServerErrorException(e.getMessage());
        }
    }

    //TODO Add lastknown parameter
    @RequestMapping(value = "/mnb/currencies", method = RequestMethod.GET)
    @Operation(summary = "Retrieves all available currencies with exchange rate.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucessful operation",
                    content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExchangeRate.class)))}),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "500", description = "Server error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))})
    })
    public ResponseEntity<Iterable<ExchangeRate>> getCurrencies(
            @Parameter(description = DATE_DESCRIPTION, example = DATE_EXAMPLE) @RequestParam(required = false) String date,
            HttpServletRequest request
    ) {
        System.out.println(LocalDateTime.now().toString() + " " + request.getRemoteAddr());
        if (date != null) {
            checkDate(date);
        }
        try {
            List<ExchangeRate> currencies = new ArrayList<>();
            List<String> symbols = facade.getCurrencies();
            for (String currencyId : symbols) {
                try {
                    if (date == null) {
                        currencies.add(facade.getCurrentExchangeRate(currencyId));
                    } else {
                        currencies.add(facade.getHistoricalExchangeRate(currencyId, date));
                    }
                } catch (IndexOutOfBoundsException e) {
                    //Do nothing theres no data for a given currency
                }
            }
            //Iterable<String> currencies = facade.getCurrencies();
            return new ResponseEntity<>(currencies, HttpStatus.OK);
        } catch (MNBWebserviceFacadeException e) {
            throw new MnbServerErrorException(e.getMessage());
        }
    }

    @RequestMapping(value = "/mnb/currencies/{currencyId}", method = RequestMethod.GET)
    @Operation(summary = "Exchange rate of a single currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucessful operation",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ExchangeRate.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "500", description = "Server error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))})
    })
    public ResponseEntity<ExchangeRate> getCurrency(
            @PathVariable String currencyId,
            @Parameter(description = DATE_DESCRIPTION, example = DATE_EXAMPLE)
            @RequestParam(required = false) String date,
            @Parameter(description = TARGET_DESCRIPTION)
            @RequestParam(required = false) String target,
            @Parameter(description = LASTKNOWN_DESCRIPTION)
            @RequestParam(required = false) boolean lastknown) {
        try {
            checkCurrency(currencyId);
            ExchangeRate rate = null;
            if (date == null) {
                if ((target == null) || (target.equals("HUF"))) {
                    rate = getRate(currencyId);
                    //rate = facade.getCurrentExchangeRate(currencyId);
                } else {
                    //TODO Webservicefacade has no option for this without date. And current date not allways available
                    rate = getRate(currencyId, target);
                    //rate = facade.getExchangeRateBetween(currencyId, target, LocalDate.now());
                }
            } else {
                checkDate(date);
                boolean success = false;
                while (!success) {
                    if ((target == null) || (target.equals("HUF"))) {
                        try {
                            rate = getDatedRate(currencyId, date);
                            //rate = facade.getHistoricalExchangeRate(currencyId, date);
                            success = true;
                        } catch (IllegalArgumentException e) {
                            if (lastknown) {
                                date = goBackOneDay(date);
                            } else {
                                throw new ResourceNotFoundException("No rate were found. Set lastknow=true for last available rate.");
                            }
                        }
                    } else {
                        checkCurrency(target);
                        try {
                            rate = getRate(currencyId, date, target);
                            //rate = facade.getExchangeRateBetween(currencyId, target, date);
                            success = true;
                        } catch (IllegalArgumentException e) {
                            if (lastknown) {
                                date = goBackOneDay(date);
                            } else {
                                throw new ResourceNotFoundException("No rate were found. Set lastknow=true for last available rate.");
                            }
                        }
                    }
                }
            }
            return new ResponseEntity<>(rate, HttpStatus.OK);
        } catch (MNBWebserviceFacadeException e) {
            throw new MnbServerErrorException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @RequestMapping(value = "/mnb/currencies/{currencyId}/history", method = RequestMethod.GET)
    @Operation(summary = "Historical data of the given currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucessful operation",
                    content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExchangeRate.class)))}),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))}),
            @ApiResponse(responseCode = "500", description = "Server error", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetail.class))})
    })
    public ResponseEntity<Iterable<ExchangeRate>> getHistory(
            @PathVariable String currencyId,
            @Parameter(description = "Starting date of the request.", example = DATE_EXAMPLE) @RequestParam String from,
            @Parameter(description = "End date of the request. Optional, defaults to current date", example = DATE_EXAMPLE) @RequestParam(required = false) String to) {
        checkCurrency(currencyId);
        if (from == null) {
            throw new InvalidRequestException("Missing parameter: from");
        } else {
            checkDate(from);
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate;
        if (to != null) {
            checkDate(to);
            toDate = LocalDate.parse(to);
        } else {
            toDate = LocalDate.now();
        }
        try {
            Iterable<ExchangeRate> history = facade.getHistoricalExchangeRates(currencyId, fromDate, toDate);
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (MNBWebserviceFacadeException e) {
            throw new MnbServerErrorException(e.getMessage());
        }
    }

    private String goBackOneDay(String dateString) {
        LocalDate date = LocalDate.parse(dateString).minusDays(1);
        if (date.isBefore(LocalDate.parse("1975-01-01"))) {
            throw new MnbServerErrorException("Terrible error!");
        }
        return date.toString();
    }

    private void checkDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(date + " can't be parsed as date. Expected format YYYY-MM-DD");
        }
    }

    private void checkCurrency(String currencyId) {
        if (!currencies.contains(currencyId)) {
            throw new ResourceNotFoundException("Currency: " + currencyId + " cant' be found");
        }
    }


    private ExchangeRate getRateFromDatabase(String id) {
        Optional<StoredRate> storedRateOptional = storedRateRepository.findById(id);
        if (storedRateOptional.isPresent()) {
            StoredRate storedRate = storedRateOptional.get();
            String[] arrOfStr = storedRate.getId().split(":", 0);
            ExchangeRate rate = new ExchangeRate();
            rate.setDate(LocalDate.parse(arrOfStr[0]));
            rate.setCurrency(arrOfStr[1]);
            rate.setUnit(storedRate.getUnit());
            rate.setRate(storedRate.getRate());
            return rate;
        } else {
            return null;
        }
    }

    private ExchangeRate getRate(String currency) throws MNBWebserviceFacadeException {
        LocalDate lastDate = facade.getStoredInterval().getEndDate();
        return getRate(currency, lastDate.toString(), "HUF");
    }

    private ExchangeRate getRate(String currency, String target) throws MNBWebserviceFacadeException {
        LocalDate lastDate = facade.getStoredInterval().getEndDate();
        return getRate(currency, lastDate.toString(), target);
    }

    private ExchangeRate getDatedRate(String currency, String date) throws MNBWebserviceFacadeException {
        return getRate(currency, date, "HUF");
    }

    private ExchangeRate getRate(String currency, String date, String target) throws MNBWebserviceFacadeException {

        if (currency.equals("HUF")) {
            throw new InvalidRequestException("HUF is not listed by MNB.");
        }

        //If rate stored in database retrieve it
        String id = String.format("%s:%s:%s", date, currency, target);
        ExchangeRate rate = getRateFromDatabase(id);
        if (rate != null) {
            return rate;
        }

        //Otherwise, make new request, store and return

        if (target == "HUF") {
            rate = facade.getHistoricalExchangeRate(currency, date);

        } else {
            rate = facade.getExchangeRateBetween(currency, target, date);
        }
        if (rate != null) {
            try {
                StoredRate storeRate = new StoredRate();
                storeRate.setId(id);
                storeRate.setUnit(rate.getUnit());
                storeRate.setRate(rate.getRate());
                System.out.println("New Rate: " + storeRate);
                storedRateRepository.save(storeRate);
            } catch (DataIntegrityViolationException e) {
                //TODO Somethimes we are too fast for the SQL server
            }

        }
        return rate;
    }


}
