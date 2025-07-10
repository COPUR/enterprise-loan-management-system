package com.loanmanagement.security.signature;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

/**
 * Mock implementation of HttpServletRequest for testing
 * Provides basic functionality needed for signature validation tests
 */
public class MockHttpServletRequest implements HttpServletRequest {

    private String method = "GET";
    private String requestURI = "/";
    private String queryString;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> parameters = new HashMap<>();

    // Setters for test setup
    
    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public void addParameter(String name, String value) {
        this.parameters.put(name, new String[]{value});
    }

    // HttpServletRequest implementation

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = headers.get(name);
        if (value == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Arrays.asList(value));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return new HashMap<>(parameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Integer.parseInt(contentLength) : -1;
    }

    @Override
    public long getContentLengthLong() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Long.parseLong(contentLength) : -1L;
    }

    // Minimal implementations for other required methods

    @Override
    public String getAuthType() { return null; }

    @Override
    public Cookie[] getCookies() { return new Cookie[0]; }

    @Override
    public long getDateHeader(String name) { return -1; }

    @Override
    public int getIntHeader(String name) { return -1; }

    @Override
    public String getPathInfo() { return null; }

    @Override
    public String getPathTranslated() { return null; }

    @Override
    public String getContextPath() { return ""; }

    @Override
    public String getRemoteUser() { return null; }

    @Override
    public boolean isUserInRole(String role) { return false; }

    @Override
    public Principal getUserPrincipal() { return null; }

    @Override
    public String getRequestedSessionId() { return null; }

    @Override
    public StringBuffer getRequestURL() { return new StringBuffer("http://localhost" + requestURI); }

    @Override
    public String getServletPath() { return ""; }

    @Override
    public HttpSession getSession(boolean create) { return null; }

    @Override
    public HttpSession getSession() { return null; }

    @Override
    public String changeSessionId() { return null; }

    @Override
    public boolean isRequestedSessionIdValid() { return false; }

    @Override
    public boolean isRequestedSessionIdFromCookie() { return false; }

    @Override
    public boolean isRequestedSessionIdFromURL() { return false; }

    @Override
    public boolean isRequestedSessionIdFromUrl() { return false; }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException { return false; }

    @Override
    public void login(String username, String password) throws ServletException {}

    @Override
    public void logout() throws ServletException {}

    @Override
    public Collection<Part> getParts() throws IOException, ServletException { return Collections.emptyList(); }

    @Override
    public Part getPart(String name) throws IOException, ServletException { return null; }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) throws IOException, ServletException { return null; }

    @Override
    public String getCharacterEncoding() { return "UTF-8"; }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {}

    @Override
    public String getContentType() { return getHeader("Content-Type"); }

    @Override
    public ServletInputStream getInputStream() throws IOException { return null; }

    @Override
    public String getProtocol() { return "HTTP/1.1"; }

    @Override
    public String getScheme() { return "http"; }

    @Override
    public String getServerName() { return "localhost"; }

    @Override
    public int getServerPort() { return 8080; }

    @Override
    public BufferedReader getReader() throws IOException { return null; }

    @Override
    public String getRemoteAddr() { return "127.0.0.1"; }

    @Override
    public String getRemoteHost() { return "localhost"; }

    @Override
    public Locale getLocale() { return Locale.getDefault(); }

    @Override
    public Enumeration<Locale> getLocales() { return Collections.enumeration(Arrays.asList(Locale.getDefault())); }

    @Override
    public boolean isSecure() { return false; }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) { return null; }

    @Override
    public String getRealPath(String path) { return null; }

    @Override
    public int getRemotePort() { return 0; }

    @Override
    public String getLocalName() { return "localhost"; }

    @Override
    public String getLocalAddr() { return "127.0.0.1"; }

    @Override
    public int getLocalPort() { return 8080; }

    @Override
    public ServletContext getServletContext() { return null; }

    @Override
    public AsyncContext startAsync() throws IllegalStateException { return null; }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException { return null; }

    @Override
    public boolean isAsyncStarted() { return false; }

    @Override
    public boolean isAsyncSupported() { return false; }

    @Override
    public AsyncContext getAsyncContext() { return null; }

    @Override
    public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }
}