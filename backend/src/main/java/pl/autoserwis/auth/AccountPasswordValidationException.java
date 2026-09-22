package pl.autoserwis.auth;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AccountPasswordValidationException extends ApiException {
    public AccountPasswordValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.ACCOUNT_PASSWORD_VALIDATION_FAILED,
            "Password cannot be changed.", fieldErrors);
    }
}
