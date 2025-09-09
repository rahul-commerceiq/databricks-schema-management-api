package ai.commerceiq.schemamanagement.interceptor;

import ai.commerceiq.schemamanagement.utils.StringConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

public class MdcInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    MDC.put(StringConstants.CORRELATION_ID_KEY, getCorrelationId());
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    MDC.remove(StringConstants.CORRELATION_ID_KEY);
  }

  private String getCorrelationId() {
    return UUID.randomUUID().toString();
  }
}