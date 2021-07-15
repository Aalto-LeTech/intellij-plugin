package fi.aalto.cs.apluscourses.utils;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import fi.aalto.cs.apluscourses.model.InvalidAuthenticationException;
import fi.aalto.cs.apluscourses.model.UnexpectedResponseException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.VersionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A utility class with methods for getting resources from a remote. For most use cases, the {@link
 * CoursesClient#fetch} methods are sufficient. The {@link CoursesClient#fetchAndMap} and {@link
 * CoursesClient#fetchAndConsume} methods can be used when direct access to the input stream of the
 * response is needed.
 */
public class CoursesClient {

  private static final AtomicReference<String> userAgent = new AtomicReference<>(null);

  /**
   * Makes a GET request to the given URL and returns the response body in a
   * {@link ByteArrayInputStream}.
   *
   * @param url          The URL to which the request is made.
   * @return             A {@link ByteArrayInputStream} containing the response body.
   * @throws IOException If an error (e.g. network error) occurs while downloading the file. This is
   *                     an instance of {@link UnexpectedResponseException} if the status code of
   *                     the response isn't 2xx or the response is missing a body.
   */
  @NotNull
  public static ByteArrayInputStream fetch(@NotNull URL url) throws IOException {
    return fetch(url, (HttpAuthentication) null);
  }

  /**
   * Makes a GET request to the given URL with the given authentication in the request and returns
   * the response body in a {@link ByteArrayInputStream}.
   *
   * @param url            The URL to which the request is made.
   * @param authentication The authentication that gets added to the request.
   * @return               A {@link ByteArrayInputStream} containing the response body.
   * @throws IOException   If an error (e.g. network error) occurs while downloading the file. This
   *                       is an instance of {@link InvalidAuthenticationException} if the response
   *                       status code is 401 or 403.
   */
  @NotNull
  public static ByteArrayInputStream fetch(@NotNull URL url,
                                           @Nullable HttpAuthentication authentication)
      throws IOException {
    return fetchAndMap(url, authentication,
        response -> {
          requireResponseEntity(response);
          return new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity()));
        });
  }

  /**
   * Downloads a file from the given URL into the given file.
   *
   * @throws IOException                 If an error (e.g. network error) occurs while downloading
   *                                     the file. This is an instance of {@link
   *                                     UnexpectedResponseException} if the status code of the
   *                                     response isn't 2xx or the response is missing a body.
   */
  public static void fetch(@NotNull URL url, @NotNull File file) throws IOException {
    fetch(url, file, null);
  }

  /**
   * Downloads a file from the given URL into the given file.
   *
   * @throws IOException                 If an error (e.g. network error) occurs while downloading
   *                                     the file. This is an instance of {@link
   *                                     UnexpectedResponseException} if the status code of the
   *                                     response isn't 2xx or the response is missing a body.
   */
  public static void fetch(@NotNull URL url,
                           @NotNull File file,
                           @Nullable HttpAuthentication authentication) throws IOException {
    fetchAndConsume(url, authentication,
        response -> {
          requireResponseEntity(response);
          FileUtils.copyInputStreamToFile(response.getEntity().getContent(), file);
        }
    );
  }

  /**
   * A functional interface for adding authentication to an HTTP request.
   */
  @FunctionalInterface
  public interface HttpAuthentication {
    void addToRequest(HttpRequest request);
  }

  /**
   * A functional interface for functions that map a {@link HttpResponse} to a desired result. See
   * {@link EntityUtils} for useful methods for working with {@link HttpEntity} instances.
   */
  @FunctionalInterface
  public interface ResponseMapper<T> {
    T map(@NotNull HttpResponse response) throws IOException;
  }

  /**
   * A functional interface for functions that consume a {@link HttpResponse} and use it for
   * side-effects.
   */
  @FunctionalInterface
  public interface ResponseConsumer {
    void consume(@NotNull HttpResponse response) throws IOException;
  }

  /**
   * Makes a GET request to the given URL and returns the mapped response.
   *
   * @param url            The URL to which the GET request is made.
   * @param authentication An instance of {@link HttpAuthentication} that gets added to the request,
   *                       or null if no authentication should be added.
   * @param mapper         A {@link ResponseMapper} that converts the {@link HttpResponse} instance
   *                       to the desired format.
   * @return The result of {@code mapper.map(response)}, where response is a {@link HttpResponse}
   *         containing the response.
   * @throws IOException If an issue occurs while making the request, which includes cases such as
   *                     an unknown host. This is an instance of {@link UnexpectedResponseException}
   *                     if the status code isn't 2xx.
   */
  public static <T> T fetchAndMap(@NotNull URL url,
                                  @Nullable HttpAuthentication authentication,
                                  @NotNull ResponseMapper<T> mapper) throws IOException {
    HttpGet request = new HttpGet(url.toString());
    if (authentication != null) {
      authentication.addToRequest(request);
    }
    return mapResponse(request, mapper);
  }

  /**
   * Makes a GET request to the given URL and consumes the response.
   *
   * @param url            The URL to which the GET request is made.
   * @param authentication An instance of {@link HttpAuthentication} that gets added to the request,
   *                       or null if no authentication should be added.
   * @param consumer       A {@link ResponseConsumer} that consumes the {@link HttpResponse}
   *                       instance.
   * @throws IOException If an issue occurs while making the request, which includes cases such as
   *                     an unknown host. This is an instance of {@link UnexpectedResponseException}
   *                     if the status code isn't 2xx.
   */
  public static void fetchAndConsume(@NotNull URL url,
                                     @Nullable HttpAuthentication authentication,
                                     @NotNull ResponseConsumer consumer) throws IOException {
    HttpGet request = new HttpGet(url.toString());
    if (authentication != null) {
      authentication.addToRequest(request);
    }
    consumeResponse(request, consumer);
  }

  /**
   * Sends a POST request to the given URL and returns the value created by the mapper.
   *
   * @param url            A URL.
   * @param authentication The method of authentication.
   * @param data           Map of request data.  Values can be strings, numbers or files.
   * @param mapper         A {@link ResponseMapper} that maps the HTTP response to the desired
   *                       format.
   *
   * @return The value created by passing the response to the given mapper.
   *
   * @throws IOException In case of I/O related errors or non-successful response.
   */
  @Nullable
  public static <T> T post(@NotNull URL url,
                           @Nullable HttpAuthentication authentication,
                           @Nullable Map<String, Object> data,
                           @NotNull ResponseMapper<T> mapper) throws IOException {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        builder.addPart(entry.getKey(), getContentBody(entry.getValue()));
      }
    }
    HttpPost request = new HttpPost(url.toString());
    request.setEntity(builder.build());

    if (authentication != null) {
      authentication.addToRequest(request);
    }

    try (CloseableHttpClient client = createClient();
         CloseableHttpResponse response = client.execute(request)) {
      requireSuccessStatusCode(response);
      return mapper.map(response);
    }
  }

  @NotNull
  private static ContentBody getContentBody(@NotNull Object value) {
    if (value instanceof String) {
      return new StringBody((String) value, ContentType.MULTIPART_FORM_DATA);
    }
    if (value instanceof Number) {
      return getContentBody(String.valueOf(value));
    }
    if (value instanceof File) {
      return new FileBody((File) value);
    }
    throw new IllegalArgumentException("Type of value not supported.");
  }

  /**
   * Executes the given request, performs some checks on the response and returns the result of
   * passing the response to the given mapper.
   */
  private static <T> T mapResponse(@NotNull HttpUriRequest request,
                                   @NotNull ResponseMapper<T> mapper) throws IOException {
    try (CloseableHttpClient client = createClient();
         CloseableHttpResponse response = client.execute(request)) {
      requireSuccessStatusCode(response);
      return mapper.map(response);
    }
  }

  /**
   * Executes the given request, performs some checks on the response and passes the response to the
   * given consumer.
   */
  private static void consumeResponse(@NotNull HttpUriRequest request,
                                      @NotNull ResponseConsumer consumer) throws IOException {
    try (CloseableHttpClient client = createClient();
         CloseableHttpResponse response = client.execute(request)) {
      requireSuccessStatusCode(response);
      consumer.consume(response);
    }
  }

  /**
   * Throws a {@link UnexpectedResponseException} if the response status code isn't 2xx. If the
   * response body includes JSON with an array for the key "errors" or a string for the key
   * "detail", then those messages are included in the exception message.
   */
  public static void requireSuccessStatusCode(@NotNull HttpResponse response)
      throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 200 && statusCode < 300) {
      return;
    }

    String details;
    try {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        details = "" + statusCode + " " + response.getStatusLine().getReasonPhrase();
      } else {
        // Two possibilities: {"errors": [...]} and {"detail": "..."} NOSONAR
        JSONObject json = new JSONObject(new JSONTokener(
            new ByteArrayInputStream(EntityUtils.toByteArray(entity))
        ));
        details = json.optString("detail");
        if (details == null || details.trim().isEmpty()) {
          details = json
              .getJSONArray("errors")
              .toList()
              .stream()
              .map(Object::toString)
              .collect(Collectors.joining(", "));
        }
      }
    } catch (JSONException e) {
      details = "" + statusCode + " " + response.getStatusLine().getReasonPhrase();
    }
    throw new UnexpectedResponseException(response, details);
  }

  /**
   * Throws a {@link UnexpectedResponseException} if the response entity is null.
   */
  private static void requireResponseEntity(@NotNull HttpResponse response)
      throws UnexpectedResponseException {
    if (response.getEntity() == null) {
      throw new UnexpectedResponseException(response, "Response is missing body");
    }
  }

  @NotNull
  private static CloseableHttpClient createClient() {
    return HttpClients.custom().setUserAgent(getUserAgent()).build();
  }

  @NotNull
  private static String getUserAgent() {
    String userAgentString = userAgent.get();
    if (userAgentString != null) {
      return userAgentString;
    }

    ApplicationInfo appInfo = ApplicationInfo.getInstance();
    ApplicationNamesInfo appNamesInfo = ApplicationNamesInfo.getInstance();
    String intellijVersion = appInfo.getFullVersion();
    String product = appNamesInfo.getProductName(); // E.g. "IDEA"
    String edition = appNamesInfo.getEditionName(); // E.g. "Community Edition"
    String os = System.getProperty("os.name");
    String pluginVersion = BuildInfo.INSTANCE.pluginVersion.toString();
    String apacheUserAgent =
        VersionInfo.getUserAgent("Apache-HttpClient", "org.apache.http.client", VersionInfo.class);

    userAgentString = String.format(
        "intellij/%s (%s; %s; %s) apluscourses/%s %s",
        intellijVersion,
        product,
        edition,
        os,
        pluginVersion,
        apacheUserAgent);

    return userAgent.compareAndSet(null, userAgentString) ? userAgentString : userAgent.get();
  }
}
