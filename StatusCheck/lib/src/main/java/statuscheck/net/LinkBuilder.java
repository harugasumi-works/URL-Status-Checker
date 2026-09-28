package statuscheck.net;

import java.net.IDN;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;

public class LinkBuilder {
	
	private static final HttpClient client = clientFactory();
	
	static String toAsciiHost(String link) {
		int end = link.length();
		for (int i = 0; i < link.length(); i++) {
			char c = link.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}
		String authority = link.substring(0, end);
		String rest = link.substring(end);

		int at = authority.lastIndexOf('@');
		String userInfo = authority.substring(0, at + 1);
		String hostPort = authority.substring(at + 1);

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
		return userInfo + IDN.toASCII(host) + port + rest;
	}

	public static final HttpRequest.Builder requestFactory(String link) {
		return HttpRequest.newBuilder()
       		 	.uri(URI.create("https://" + toAsciiHost(link)))
       		 	.timeout(Duration.ofSeconds(5))
 	        	.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
 	        	.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
 	        	.header("Accept-Language", "en-US,en;q=0.9")
 	        	.GET();
	}
	
	public static final HttpClient clientFactory() {
		return HttpClient.newBuilder()
				.version(Version.HTTP_3)
				.followRedirects(HttpClient.Redirect.NORMAL)
	            .connectTimeout(Duration.ofSeconds(10))
	            .build(); 
	}
	
	public static  final HttpClient clientGet() {
		return client;
	}
}
