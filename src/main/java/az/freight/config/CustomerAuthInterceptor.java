package az.freight.config;

import az.freight.entity.Customer;
import az.freight.service.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/customer/** üçün X-Customer-Token yoxlayır; qeydiyyat, giriş və CORS preflight istisnadır. */
@Component
@RequiredArgsConstructor
public class CustomerAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Customer-Token";
    public static final String ATTR = "authCustomer";

    private final CustomerAuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        Customer customer = auth.authenticate(request.getHeader(HEADER));
        request.setAttribute(ATTR, customer);
        return true;
    }
}
