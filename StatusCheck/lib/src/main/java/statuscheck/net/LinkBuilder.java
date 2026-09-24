package statuscheck.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;

public class LinkBuilder {
	
	private static final HttpClient client = clientFactory();
	
	public static final HttpRequest.Builder requestFactory(String link) {
		return HttpRequest.newBuilder()
       		 	.uri(URI.create("https://" + link))
       		 	.timeout(Duration.ofSeconds(5))
 	        	.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
 	        	.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
 	        	.header("Accept-Language", "en-US,en;q=0.9")
 	        	.GET();
	}
	
	public static final HttpClient clientFactory() {
		return HttpClient.newBuilder()
				.version(Version.HTTP_3)
	            .connectTimeout(Duration.ofSeconds(10))
	            .build(); 
	}
	
	public static  final HttpClient clientGet() {
		return client;
	}
}
