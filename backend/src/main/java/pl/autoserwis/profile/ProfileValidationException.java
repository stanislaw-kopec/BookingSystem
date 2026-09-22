package pl.autoserwis.profile;

import org.springframework.http.HttpStatus;
import pl.autoserwis.exception.ApiException;
import pl.autoserwis.exception.ApiErrorCode;

import java.util.Map;

public class ProfileValidationException extends ApiException {
    public ProfileValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.PROFILE_VALIDATION_FAILED,
            "Company profile data is incomplete.", fieldErrors);
    }
}
