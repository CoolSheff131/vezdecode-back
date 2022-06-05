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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class BackApplication {

	private static final Logger log = LoggerFactory.getLogger(BackApplication.class);

	private static final List<Artist> artists = new ArrayList<>();
	private static final Map<Date,Vote> votes = new TreeMap<>();
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
				Date date = new Date();
				votes.put(date,vote);
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



	@GetMapping("/votes/stats")
	@ResponseBody
	public HashMap<String, List< HashMap<String, Long>>>  getVotesFilter(@RequestParam(required = false) String from,
											  @RequestParam(required = false) String to,
											  @RequestParam(required = false) String intervals,
											  @RequestParam(required = false) String artists) throws ParseException {
		Date fromDate;
		if(from == null){
			fromDate = new Date();
			for (var key: votes.keySet()) {
				if(key.before(fromDate)){
					fromDate = key;
				}
			}
		}else{
			SimpleDateFormat formatter6 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			fromDate = formatter6.parse(from);
		}

		Date endDate;
		if(to == null){
			endDate = new Date();
			for (var key: votes.keySet()) {
				if(key.after(endDate)){
					endDate = key;
				}
			}
		}else{
			SimpleDateFormat formatter6 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			endDate = formatter6.parse(to);
		}

		Calendar calendar1 = Calendar.getInstance();
		calendar1.setTime(fromDate);

		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(endDate);
		long milSec = calendar2.getTimeInMillis() - calendar1.getTimeInMillis();
		if(intervals == null){
			intervals = "10";
		}
		long milSecInPeriod = milSec / Integer.parseInt(intervals);

		if(milSecInPeriod < 1000){
			milSecInPeriod = 1000;
		}
		long runMilSec = calendar1.getTimeInMillis();
		List<Date> dates = new ArrayList<>();
		while (runMilSec < calendar2.getTimeInMillis()) {
			var time = runMilSec + milSecInPeriod;
			runMilSec += milSecInPeriod;
			var date = new Date(time);
			dates.add(date);
		}
		HashMap<String, List< HashMap<String, Long>>> data = new HashMap<>();

		List<HashMap<String, Long>> dataItems  = new ArrayList<>();
		List<String> artistArrayFilter = null;
		if(artists != null){
			artistArrayFilter = Arrays.stream(artists.split(",")).toList();
		}

		for (int i = 0; i < dates.size() - 1; i++) {
			HashMap<String, Long> dataItem = new HashMap<>();
			calendar1.setTime(dates.get(i));
			dataItem.put("start", calendar1.getTimeInMillis());
			calendar1.setTime(dates.get(i + 1));
			dataItem.put("end", calendar1.getTimeInMillis());
			var countVotes = 0;
			for (var key : votes.keySet()) {
				var artistVote = votes.get(key).getArtist();
				if(artistArrayFilter != null && !artistArrayFilter.contains(artistVote)){
					continue;
				}
				if (key.after(dates.get(i)) && key.before(dates.get(i+1))) {
					countVotes += 1;
				}
			}
			dataItem.put("votes", (long) countVotes);
			dataItems.add(dataItem);
		}

		data.put("data", dataItems);
		return data;
	}
}
