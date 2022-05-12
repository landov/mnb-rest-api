package hu.landov.mnb.rest.config;

import hu.landov.mnb.MNBWebserviceFacade;
import hu.landov.mnb.MNBWebserviceFacadeException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MNBWebserviceFacadeConfig {

    @Bean
    public MNBWebserviceFacade getFacade() {
        try {
            return new MNBWebserviceFacade();
        } catch (final MNBWebserviceFacadeException e) {
            throw new RuntimeException(e);
        }
    }
}
