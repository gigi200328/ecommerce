package com.ojt.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitingFilter extends OncePerRequestFilter {

	// IP တစ်ခုချင်းစီအတွက် Request အရေအတွက်နှင့် အချိန်ကို သိမ်းဆည်းရန်
	private final Map<String, ClientRateLimit> requestCounts = new ConcurrentHashMap<>();

	// သတ်မှတ်ချက် - ၁ မိနစ်အတွင်း (60 စက္ကန့်) အများဆုံး Login/Register ၅ ကြိမ်သာ
	// ခွင့်ပြုမည်
	private static final int MAX_REQUESTS = 5;
	private static final long TIME_WINDOW_MS = Duration.ofMinutes(1).toMillis();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		// Login သို့မဟုတ် Register Endpoints များကိုသာ Rate Limit လုပ်မည်
		if (path.contains("/auth/customer/login") || path.contains("/auth/customer/register")) {
			String clientIp = request.getRemoteAddr();
			long currentTime = System.currentTimeMillis();

			ClientRateLimit limit = requestCounts.compute(clientIp, (ip, existing) -> {
				if (existing == null || currentTime - existing.startTime > TIME_WINDOW_MS) {
					return new ClientRateLimit(currentTime, new AtomicInteger(1));
				} else {
					existing.count.incrementAndGet();
					return existing;
				}
			});

			if (limit.count.get() > MAX_REQUESTS) {
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.setContentType("application/json");
				response.getWriter().write("{\"error\": \"Too many requests - please try again later.\"}");
				response.getWriter().flush();
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private static class ClientRateLimit {
		private final long startTime;
		private final AtomicInteger count;

		public ClientRateLimit(long startTime, AtomicInteger count) {
			this.startTime = startTime;
			this.count = count;
		}
	}
}