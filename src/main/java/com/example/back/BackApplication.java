package com.example.back;


import com.example.back.models.Artist;
import com.example.back.models.ResponceArtist;
import com.example.back.models.Vote;
import io.github.bucket4j.*;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class BackApplication {

	private static final Logger log = LoggerFactory.getLogger(BackApplication.class);

	private static final List<Artist> artists = new ArrayList<>();
	private static Bucket bucket;

	private static final int MAX_AMOUNT_OF_REQUESTS = 5;

	private static final int PERIOD = 1;

	private static final Map<String, Bucket> cache = new ConcurrentHashMap<>();

	public static Bucket resolveBucket(String apiKey) {
		return cache.computeIfAbsent(apiKey, BackApplication::newBucket);
	}

	private static Bucket newBucket(String apiKey) {
		Bandwidth limit = Bandwidth.classic(MAX_AMOUNT_OF_REQUESTS, Refill.greedy(20, Duration.ofMinutes(PERIOD)));
		return Bucket4j.builder()
				.addLimit(limit)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(BackApplication.class, args);
		artists.add(new Artist("Актер 1", 0));
		artists.add(new Artist("Актер 2", 0));
		artists.add(new Artist("Актер 3", 0));
		artists.add(new Artist("Актер 4", 0));
		artists.add(new Artist("Актер 5", 0));
		artists.add(new Artist("Актер 6", 0));
		artists.add(new Artist("Актер 7", 0));
		artists.add(new Artist("Актер 8", 0));
		artists.add(new Artist("Актер 9", 0));
		Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
		bucket = Bucket4j.builder()
				.addLimit(limit)
				.build();
	}

	@GetMapping("/votes")
	public Map<String, List<Artist>> getArtists() {
		HashMap<String, List<Artist>> map = new HashMap<>();
		map.put("data", artists);
		return map;
	}



	@PostMapping("/votes")
	ResponseEntity  newEmployee(@RequestBody Vote vote) {
		if(vote.getPhone() == null || vote.getArtist() == null){
			HttpHeaders responseHeaders = new HttpHeaders();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.headers(responseHeaders)
					.body("Не валидные данные");
		}

		Bucket bucket = resolveBucket(vote.getPhone());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("X-Rate-Limit-Remaining",
					Long.toString(probe.getRemainingTokens()));
			System.out.println("2");
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.headers(responseHeaders)
					.body("Слишком много запросов");
		}

		boolean foundArtist = false;

		for (Artist artist: artists) {
			System.out.println(artist);
			if(Objects.equals(artist.getName(), vote.getArtist())){
				foundArtist = true;
				artist.setVotes(artist.getVotes() + 1);
				break;
			}
		}

		if(!foundArtist){
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("X-Rate-Limit-Remaining",
					Long.toString(probe.getRemainingTokens()));

			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.headers(responseHeaders)
					.body("Актер не найден");

		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("X-Rate-Limit-Remaining",
				Long.toString(probe.getRemainingTokens()));

		return ResponseEntity.status(HttpStatus.OK)
				.headers(responseHeaders)
				.body("Голос Учтен");
	}
}
