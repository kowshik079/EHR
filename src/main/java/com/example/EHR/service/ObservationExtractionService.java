package com.example.EHR.service;

import com.example.EHR.model.Observation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ObservationExtractionService {

    private static final Pattern BP_PATTERN = Pattern.compile("(?:BP|Blood Pressure)[:\u2013\u2014\u2212-]?\\s*(\\d{2,3})\\s*/\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HR_PATTERN = Pattern.compile("(?:HR|Heart Rate|Pulse)[:\u2013\u2014\u2212-]?\\s*(\\d{2,3})\\s*(?:bpm)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMP_PATTERN = Pattern.compile("(?:Temp|Temperature)[:\u2013\u2014\u2212-]?\\s*(\\d{2,3}\\.\\d|\\d{2,3})", Pattern.CASE_INSENSITIVE);

    public List<Observation> extractObservations(String text) {
        List<Observation> observations = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return observations;
        }

        Matcher bpMatcher = BP_PATTERN.matcher(text);
        while (bpMatcher.find()) {
            String systolic = bpMatcher.group(1);
            String diastolic = bpMatcher.group(2);
            observations.add(new Observation("Blood Pressure", systolic + "/" + diastolic));
        }

        Matcher hrMatcher = HR_PATTERN.matcher(text);
        while (hrMatcher.find()) {
            String hr = hrMatcher.group(1);
            observations.add(new Observation("Heart Rate", hr));
        }

        Matcher tempMatcher = TEMP_PATTERN.matcher(text);
        while (tempMatcher.find()) {
            String temp = tempMatcher.group(1);
            observations.add(new Observation("Temperature", temp));
        }

        return observations;
    }
}
