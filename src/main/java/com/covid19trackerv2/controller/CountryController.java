package com.covid19trackerv2.controller;

import com.covid19trackerv2.model.country.Country;
import com.covid19trackerv2.model.country.CountryDoc;
import com.covid19trackerv2.repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/country")
public class CountryController {

    @Autowired
    private Environment environment;

    @Autowired
    private CountryRepository countryRepo;

    @GetMapping("/all")
    public ResponseEntity<List<CountryDoc>> getCountries() {
        return ResponseEntity.ok().body(this.countryRepo.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<CountryDoc>> getCountriesPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().body(this.countryRepo.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getCountryById(@PathVariable String id) {
        Optional<CountryDoc> country = this.countryRepo.findById(id);
        return country.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok()
                        .body("Country document not found with id " + id));
    }

    @GetMapping("/date")
    public ResponseEntity<Object> getStateByDate(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {
        LocalDate date = LocalDate.of(year, month, day);
        Optional<CountryDoc> state = this.countryRepo.findByDate(date);
        return state.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok()
                        .body("Country document not found with date " + date.toString()));
    }

    @GetMapping("")
    public ResponseEntity<List<CountryDoc>> getCountryByName(@RequestParam String name) {
        List<CountryDoc> listWithCountryName = this.countryRepo.findByCountriesCountry(name.toLowerCase(Locale.US));
        for (CountryDoc countryDoc : listWithCountryName) {
            countryDoc.setCountries(
                    countryDoc.getCountries().stream().filter(
                            country -> country.getCountry().equalsIgnoreCase(name))
                            .collect(Collectors.toList()));
        }
        return ResponseEntity.ok().body(listWithCountryName);
    }

    @GetMapping("/all/totals")
    public ResponseEntity<Map<String, Long>> getAllCountryTotals() {
        Map<String, Long> totals = new HashMap<>();
        totals.put("confirmed", 0L);
        totals.put("active", 0L);
        totals.put("recovered", 0L);
        totals.put("deaths", 0L);
        // gets single document by most recent date
        Optional<CountryDoc> mostRecent = this.countryRepo.findTopByOrderByDateDesc();
        if (mostRecent.isPresent()) {
            for (Country country : mostRecent.get().getCountries()) {
                totals.put("confirmed", totals.get("confirmed") + country.getConfirmed());
                totals.put("active", totals.get("active") + country.getActive());
                totals.put("recovered", totals.get("recovered") + country.getRecovered());
                totals.put("deaths", totals.get("deaths") + country.getDeaths());
            }
        }
        return ResponseEntity.ok().body(totals);
    }

    @GetMapping("/totals")
    public ResponseEntity<Map<String, Long>> getCountryTotals(@RequestParam String name) {
        Map<String, Long> totals = new HashMap<>();
        totals.put("confirmed", 0L);
        totals.put("active", 0L);
        totals.put("recovered", 0L);
        totals.put("deaths", 0L);
        // gets single document by most recent date
        Optional<CountryDoc> mostRecent = this.countryRepo.findTopByOrderByDateDesc();
        if (mostRecent.isPresent()) {
            for (Country country : mostRecent.get().getCountries()) {
                // only care about the one country we are looking for
                // once found set values and break out of loop
                if (country.getCountry().equalsIgnoreCase(name)) {
                    totals.put("confirmed", country.getConfirmed());
                    totals.put("active", country.getActive());
                    totals.put("recovered", country.getRecovered());
                    totals.put("deaths", country.getDeaths());
                    break;
                }
            }
        }
        return ResponseEntity.ok().body(totals);
    }

    @GetMapping("/rates/incident_rate")
    public ResponseEntity<Map<String, Double>> getCountryIncidentRate(@RequestParam(required = false) String name) {
        Map<String, Double> rate = new HashMap<>();
        rate.put("incident_rate", 0.0);
        // gets single document by most recent date
        Optional<CountryDoc> mostRecent = this.countryRepo.findTopByOrderByDateDesc();
        double sum = 0.0;
        if (mostRecent.isPresent()) {
            for (Country country : mostRecent.get().getCountries()) {
                if (name != null) {
                    if (country.getCountry().equalsIgnoreCase(name)) {
                        rate.put("incident_rate", country.getIncidentRate());
                        return ResponseEntity.ok().body(rate);
                    }
                } else {
                    sum += country.getIncidentRate();
                }
            }
            rate.put("incident_rate", sum / mostRecent.get().getCountries().size());
        }

        return ResponseEntity.ok().body(rate);
    }

    @GetMapping("/rates/mortality_rate")
    public ResponseEntity<Map<String, Double>> getCountryMortalityRate(@RequestParam(required = false) String name) {
        Map<String, Double> rate = new HashMap<>();
        rate.put("mortality_rate", 0.0);
        // gets single document by most recent date
        Optional<CountryDoc> mostRecent = this.countryRepo.findTopByOrderByDateDesc();
        double sum = 0.0;
        if (mostRecent.isPresent()) {
            for (Country country : mostRecent.get().getCountries()) {
                if (name != null) {
                    if (country.getCountry().equalsIgnoreCase(name)) {
                        rate.put("mortality_rate", country.getMortalityRate());
                        return ResponseEntity.ok().body(rate);
                    }
                } else {
                    sum += country.getMortalityRate();
                }
            }
            rate.put("mortality_rate", sum / mostRecent.get().getCountries().size());
        }

        return ResponseEntity.ok().body(rate);
    }

    // TODO: add route to get total confirmed, deaths, recovered, active & average mortality and incident rate


    @DeleteMapping("delete_countries")
    public ResponseEntity<String> deleteAllCountries(@RequestBody(required = false) Map<String, String> password) {
        if (password == null || !password.containsKey("password")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password required for delete route");
        }
        if (password.get("password").equals(environment.getProperty("delete.route.password"))) {
            this.countryRepo.deleteAll();
            return ResponseEntity.ok().body("Countries DB cleared");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password given for delete route");
        }
    }

}
