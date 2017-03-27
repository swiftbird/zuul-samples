package hello.filters.post;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import hello.filters.pre.SimpleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Created by associate on 9/26/16.
 */
public class SimplePostFilter extends ZuulFilter {

    static final Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    private static DynamicIntProperty INITIAL_STREAM_BUFFER_SIZE = DynamicPropertyFactory
            .getInstance()
            .getIntProperty(ZuulConstants.ZUUL_INITIAL_STREAM_BUFFER_SIZE, 1024);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            log.info("Determine which body to send");
            writeResponse();
        }
        catch (Exception ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
        }
        return null;
    }

    private void writeResponse() throws Exception {
        RequestContext context = RequestContext.getCurrentContext();
        // there is no body to send
        if (context.getResponseBody() == null
                && context.getResponseDataStream() == null) {

            return;
        }
        HttpServletResponse servletResponse = context.getResponse();
        if (servletResponse.getCharacterEncoding() == null) { // only set if not set
            servletResponse.setCharacterEncoding("UTF-8");
        }
        OutputStream outStream = servletResponse.getOutputStream();
        InputStream is = null;
        try {
            if (RequestContext.getCurrentContext().getResponseBody() != null) {
                String body = RequestContext.getCurrentContext().getResponseBody();
                log.info("Body of response = " + body);
                writeResponse(
                        new ByteArrayInputStream(
                                body.getBytes(servletResponse.getCharacterEncoding())),
                        outStream);
                return;
            }
            boolean isGzipRequested = false;
            final String requestEncoding = context.getRequest()
                    .getHeader(ZuulHeaders.ACCEPT_ENCODING);

            if (requestEncoding != null
                    && HTTPRequestUtils.getInstance().isGzipped(requestEncoding)) {
                isGzipRequested = true;
            }
            is = context.getResponseDataStream();
            InputStream inputStream = is;
            if (is != null) {
                if (context.sendZuulResponse()) {
                    // if origin response is gzipped, and client has not requested gzip,
                    // decompress stream
                    // before sending to client
                    // else, stream gzip directly to client
                    if (context.getResponseGZipped() && !isGzipRequested) {
                        // If origin tell it's GZipped but the content is ZERO bytes,
                        // don't try to uncompress
                        final Long len = context.getOriginContentLength();
                        if (len == null || len > 0) {
                            try {
                                inputStream = new GZIPInputStream(is);
                            }
                            catch (java.util.zip.ZipException ex) {
                                log.debug(
                                        "gzip expected but not "
                                                + "received assuming unencoded response "
                                                + RequestContext.getCurrentContext()
                                                .getRequest().getRequestURL()
                                                .toString());
                                inputStream = is;
                            }
                        }
                        else {
                            // Already done : inputStream = is;
                        }
                    }
                    else if (context.getResponseGZipped() && isGzipRequested) {
                        servletResponse.setHeader(ZuulHeaders.CONTENT_ENCODING, "gzip");
                    }


                    writeResponse(wrapResponse(inputStream), outStream);
                }
            }
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
                outStream.flush();
                // The container will close the stream for us
            }
            catch (IOException ex) {
            }
        }
    }

    private InputStream wrapResponse(InputStream is) throws Exception{
        String result = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        String wrappedString = "{ \"message\" : \"" + result + "\" }";
        return new ByteArrayInputStream(wrappedString.getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(InputStream zin, OutputStream out) throws Exception {
        byte[] bytes = new byte[INITIAL_STREAM_BUFFER_SIZE.get()];
        int bytesRead = -1;
        while ((bytesRead = zin.read(bytes)) != -1) {
            try {
                out.write(bytes, 0, bytesRead);
                out.flush();
            }
            catch (IOException ex) {
                // ignore
            }
            // doubles buffer size if previous read filled it
            if (bytesRead == bytes.length) {
                bytes = new byte[bytes.length * 2];
            }
        }
    }
}
