package com.example.EHR.service;

import com.example.EHR.model.Observation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ObservationExtractionServiceTest {

    @Autowired
    ObservationExtractionService service;

    @Test
    void extractsBloodPressure() {
        String t = "Vitals: BP 128/82 mmHg, pulse 76";
        List<Observation> obs = service.extract(t);
        Observation bp = obs.stream().filter(o -> o.getType() == Observation.Type.BLOOD_PRESSURE).findFirst().orElseThrow();
        assertEquals(128, bp.getSystolic());
        assertEquals(82, bp.getDiastolic());
        assertEquals("mmHg", bp.getUnit());
    }

    @Test
    void extractsGlucoseMgdl() {
        String t = "Fasting glucose: 102 mg/dL";
        List<Observation> obs = service.extract(t);
        Observation g = obs.stream().filter(o -> o.getType() == Observation.Type.GLUCOSE).findFirst().orElseThrow();
        assertEquals(102.0, g.getValue());
        assertEquals("mg/dL", g.getUnit());
    }

    @Test
    void extractsGlucoseMmol() {
        String t = "Blood sugar 5.8 mmol/L";
        List<Observation> obs = service.extract(t);
        Observation g = obs.stream().filter(o -> o.getType() == Observation.Type.GLUCOSE).findFirst().orElseThrow();
        assertEquals(104.4, g.getValue()); // 5.8 * 18 = 104.4
        assertEquals("mg/dL", g.getUnit());
    }

    @Test
    void extractsHemoglobin() {
        String t = "Hemoglobin 13.5 g/dL";
        List<Observation> obs = service.extract(t);
        Observation h = obs.stream().filter(o -> o.getType() == Observation.Type.HEMOGLOBIN).findFirst().orElseThrow();
        assertEquals(13.5, h.getValue());
        assertEquals("g/dL", h.getUnit());
    }

    @Test
    void extractsHbA1c() {
        String t = "HbA1c: 6.9%";
        List<Observation> obs = service.extract(t);
        Observation a = obs.stream().filter(o -> o.getType() == Observation.Type.HBA1C).findFirst().orElseThrow();
        assertEquals(6.9, a.getValue());
        assertEquals("%", a.getUnit());
    }

    @Test
    void extractsCholesterolMgdl() {
        String t = "Total cholesterol 180 mg/dL";
        List<Observation> obs = service.extract(t);
        Observation c = obs.stream().filter(o -> o.getType() == Observation.Type.CHOLESTEROL).findFirst().orElseThrow();
        assertEquals(180.0, c.getValue());
        assertEquals("mg/dL", c.getUnit());
    }

    @Test
    void extractsCholesterolMmol() {
        String t = "Cholesterol: 4.7 mmol/L";
        List<Observation> obs = service.extract(t);
        Observation c = obs.stream().filter(o -> o.getType() == Observation.Type.CHOLESTEROL).findFirst().orElseThrow();
        assertEquals(181.7, c.getValue()); // 4.7 * 38.67 = 181.749 -> 181.7 rounded to 1
        assertEquals("mg/dL", c.getUnit());
    }

    @Test
    void robustnessNoCrashesOnNoise() {
        String t = "Report: BP hello/there; sugar N/A; HbA1c: -- %; random text";
        List<Observation> obs = service.extract(t);
        // No observations expected, but importantly: no exception
        assertNotNull(obs);
    }

    @Test
    void multipleObservations() {
        String t = "Vitals: BP 120/80; FBS 95 mg/dl; Hb 14.2 g/dL; HbA1c 7.2%; Cholesterol 5.0 mmol/L";
        List<Observation> obs = service.extract(t);
        assertEquals(5, obs.size());
        long bpCount = obs.stream().filter(o -> o.getType() == Observation.Type.BLOOD_PRESSURE).count();
        assertEquals(1, bpCount);
        List<Observation.Type> types = obs.stream().map(Observation::getType).collect(Collectors.toList());
        assertTrue(types.containsAll(List.of(
                Observation.Type.BLOOD_PRESSURE,
                Observation.Type.GLUCOSE,
                Observation.Type.HEMOGLOBIN,
                Observation.Type.HBA1C,
                Observation.Type.CHOLESTEROL
        )));
    }
}

