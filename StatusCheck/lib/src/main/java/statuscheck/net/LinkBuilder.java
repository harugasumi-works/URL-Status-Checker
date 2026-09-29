package statuscheck.net;

import java.net.IDN;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;

import statuscheck.util.ContentParser;

public class LinkBuilder {

	private static final HttpClient client = clientFactory();

	static String toAsciiHost(String link) {
		int schemeEnd = link.indexOf("://");
		if (schemeEnd < 0)
			return link;

		String scheme = link.substring(0, schemeEnd + 3);
		String remainder = link.substring(schemeEnd + 3);

		int end = remainder.length();
		for (int i = 0; i < remainder.length(); i++) {
			char c = remainder.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}

		String authority = remainder.substring(0, end);
		String rest = remainder.substring(end);

		int at = authority.lastIndexOf('@');
		String userInfo = at >= 0 ? authority.substring(0, at + 1) : "";
		String hostPort = at >= 0 ? authority.substring(at + 1) : authority;

		String host = hostPort;
		String port = "";

		int colon = hostPort.lastIndexOf(':');
		if (colon >= 0 && !hostPort.endsWith("]")) {
			host = hostPort.substring(0, colon);
			port = hostPort.substring(colon);
		}

		if (host.chars().allMatch(c -> c < 128)) {
			return link;
		}

		return scheme + userInfo + IDN.toASCII(host) + port + rest;
	}

	public static final HttpRequest.Builder requestFactory(String link) {
		String normalized = ContentParser.normalize(link);

		return HttpRequest.newBuilder()
				.uri(URI.create(toAsciiHost(normalized)))
				.timeout(Duration.ofSeconds(4))
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
								+ "AppleWebKit/537.36 (KHTML, like Gecko) "
								+ "Chrome/124.0.0.0 Safari/537.36")
				.header("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,"
								+ "image/avif,image/webp,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.9")
				.GET();
	}

	public static final HttpClient clientFactory() {
		return HttpClient.newBuilder()
				.version(Version.HTTP_3)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(2))
				.build();
	}

	public static final HttpClient clientGet() {
		return client;
	}
}