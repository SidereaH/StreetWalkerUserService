package streetwalker.userservice.services.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class RequestContextHelper {

    /**
     * Возвращает текущий HttpServletRequest, если доступен в контексте.
     * @return Optional<HttpServletRequest>
     */
    public Optional<HttpServletRequest> getCurrentHttpRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

    /**
     * Извлекает IP-адрес клиента из HttpServletRequest.
     * Учитывает прокси-заголовки, такие как X-Forwarded-For.
     * @param request HttpServletRequest
     * @return IP-адрес клиента или "unknown"
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        // X-Forwarded-For может содержать несколько IP, берем первый (реальный клиентский)
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Извлекает User-Agent из HttpServletRequest.
     * @param request HttpServletRequest
     * @return User-Agent или null
     */
    public String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Получает IP-адрес из текущего HTTP-запроса, если доступен.
     * @return IP-адрес или null
     */
    public String getCurrentRequestIpAddress() {
        return getCurrentHttpRequest()
                .map(this::getClientIpAddress)
                .orElse(null);
    }

    /**
     * Получает User-Agent из текущего HTTP-запроса, если доступен.
     * @return User-Agent или null
     */
    public String getCurrentRequestUserAgent() {
        return getCurrentHttpRequest()
                .map(this::getUserAgent)
                .orElse(null);
    }
}