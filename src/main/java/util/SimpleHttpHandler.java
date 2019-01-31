package util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class SimpleHttpHandler implements HttpHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final String context;

	public SimpleHttpHandler(String context) {
		this.context = context;
	}

	protected void writeHeaders(Headers responseHeaders) {

	}

	protected abstract HttpResponse handle(String[] urlfields, String body);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String url = exchange.getRequestURI().getPath();
//		url = url.substring(context.length());
		String[] fields = url.split("/");

		String body = Streams.InReadString(exchange.getRequestBody());
		if(logger.isTraceEnabled()) {
			logger.trace("Received http request `{}` with body: {}.", exchange.getRequestURI().getPath(), body);
		} else {
			logger.debug("Received http request `{}`.", exchange.getRequestURI().getPath());
		}
		HttpResponse httpResponse;
		try{
			httpResponse = handle(fields, body);
		} catch(Exception ex) {
			logger.error("Exception raised while handling `{}`:", url, ex);
			httpResponse = response(500, ex.getMessage());
		}

		if(httpResponse == null) {
			logger.error("null response returned by: {}", this.getClass().getName());
			httpResponse = response(500, "");
		} else {
			writeHeaders(exchange.getResponseHeaders());
			if(logger.isTraceEnabled()) {
				logger.trace("Responding to `{}` with {}.", url, httpResponse);
			} else {
				logger.debug("Responding to `{}` with {}.", url, httpResponse.returnCode);
			}
		}

		byte[] encodedBody = httpResponse.returnBody.getBytes();
		exchange.sendResponseHeaders(httpResponse.returnCode, encodedBody.length);
		exchange.getResponseBody().write(encodedBody);
		exchange.getResponseBody().close();
	}

	protected HttpResponse response(int returnCode, String returnBody) {
		return new HttpResponse(returnCode, returnBody);
	}

	protected static class HttpResponse {

		private final int returnCode;
		private final String returnBody;


		protected HttpResponse(int returnCode, String returnBody) {
			this.returnCode = returnCode;
			this.returnBody = returnBody;
		}

		public String toString(){
			return returnCode + ":" + returnBody;
		}
	}
}
