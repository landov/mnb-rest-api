package hu.landov.mnb.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class MnbServerErrorException extends RuntimeException{

    public MnbServerErrorException(){}

    public MnbServerErrorException(String message){
        super(message);
    }

    public MnbServerErrorException(String message, Throwable cause){
        super(message, cause);
    }

}
