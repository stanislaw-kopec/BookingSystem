package pl.autoserwis.user;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class AccountManagementValidationException extends ApiException {
    public AccountManagementValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.ACCOUNT_MANAGEMENT_VALIDATION_FAILED,
            "Account management parameters are invalid.", fieldErrors);
    }
}
