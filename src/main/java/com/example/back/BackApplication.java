package com.example.back;


import com.example.back.models.Artist;
import com.example.back.models.ResponceArtist;
import com.example.back.models.Vote;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@SpringBootApplication
@RestController
public class BackApplication {

	private static final Logger log = LoggerFactory.getLogger(BackApplication.class);

	private static final List<Artist> artists = new ArrayList<>();
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
	}

	@GetMapping("/votes")
	public Map<String, List<Artist>> getArtists() {
		HashMap<String, List<Artist>> map = new HashMap<>();
		map.put("data", artists);
		return map;
	}



	@PostMapping("/votes")
	@ResponseStatus(code = HttpStatus.CREATED, reason = "OK")
	Vote newEmployee(@RequestBody Vote vote) {
		if(vote.getPhone() ==null|| vote.getArtist() == null){
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST
			);
		}
		log.debug(vote.toString());
		System.out.println(vote.toString());
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
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "исполнитель не найден под запрошенной меткой."
			);
		}
		return vote;
	}
}
