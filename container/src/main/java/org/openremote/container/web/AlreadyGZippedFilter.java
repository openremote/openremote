package org.openremote.container.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Set;

/**
 * A simple servlet filter that checks if the response has already been gzipped and if so, sets the gzip header.
 */
public class AlreadyGZippedFilter implements Filter {

   protected Set<String> alreadyGzippedContentTypes;

   public static class GzipHeaderWrapper extends HttpServletResponseWrapper {

      private final Set<String> gzippedContentTypes;

      public GzipHeaderWrapper(HttpServletResponse response, Set<String> gzippedContentTypes) {
         super(response);
         this.gzippedContentTypes = gzippedContentTypes;
      }

      /**
       * The DefaultServlet will call this method to set the content type.
       */
      @Override
      public void setContentType(String type) {
         // Set the original content type
         super.setContentType(type);

         if (type != null) {
            boolean isMatch = gzippedContentTypes.stream()
               .anyMatch(type::startsWith);

            if (isMatch) {
               super.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            }
         }
      }

      @Override
      public void setHeader(String name, String value) {
         if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name) && !"gzip".equalsIgnoreCase(value)) {
            // It's trying to set Content-Encoding to something else.
            // If we know this response *is* gzipped, ignore it.
            // This is advanced; you can omit this block for now if you want.
            String contentType = getContentType();
            boolean isMatch = gzippedContentTypes.stream()
               .anyMatch(contentType::startsWith);
            if (isMatch) {
               return; // Ignore the attempt to change the header
            }
         }
         super.setHeader(name, value);
      }

      @Override
      public void setContentLength(int len) {}

      @Override
      public void setContentLengthLong(long len) {}
   }

   public AlreadyGZippedFilter(Set<String> alreadyGzippedContentTypes) {
      this.alreadyGzippedContentTypes = alreadyGzippedContentTypes;
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      if (response instanceof HttpServletResponse httpResponse) {
         // Create a wrapper so we can set the header before the response is committed
         GzipHeaderWrapper responseWrapper = new GzipHeaderWrapper(httpResponse, alreadyGzippedContentTypes);

         chain.doFilter(request, responseWrapper);
      } else {
         chain.doFilter(request, response);
      }
   }
}
