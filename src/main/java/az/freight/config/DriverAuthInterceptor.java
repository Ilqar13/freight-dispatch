package az.freight.config;

import az.freight.entity.Driver;
import az.freight.service.DriverAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/driver/** üçün X-Driver-Token yoxlayır; giriş və CORS preflight istisnadır. */
@Component
@RequiredArgsConstructor
public class DriverAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Driver-Token";
    public static final String ATTR = "authDriver";

    private final DriverAuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        Driver driver = auth.authenticate(request.getHeader(HEADER));
        request.setAttribute(ATTR, driver);
        return true;
    }
}
