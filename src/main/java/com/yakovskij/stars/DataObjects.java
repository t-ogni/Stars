package com.yakovskij.stars;

import android.annotation.SuppressLint;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;



public class DataObjects {

    public static class Star {
        public int hip;
        public float az, alt, mag, r, g, b;

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("HIP: %d, Az: %.2f, Alt: %.2f, Mag: %.2f, R: %.2f, G: %.2f, B: %.2f",
                    hip, az, alt, mag, r, g, b);
        }
    }

    public static class Constellation {
        public int id;
        public String abbreviation;
        public String name;
    }

    public static class ConstellationLine {
        public double alt0;
        public double az0;
        public double alt1;
        public double az1;
    }
    public static class ConstellationInfo {
        public String meaning_mythology;
        public String brightest_star;
        public String first_appearance;
        public double area_of_sky;
        public String best_time_to_see;
        public String celestial_hemisphere;
        public String picture_file;
    }

    public static class StarInfo {
        public double right_ascension;
        public double declination;
        public double magnitude;
        public double distance_parsecs;
        public String spectral_class;
        public double temperature_kelvin;
        public String bayer_flamsteed;
        public String constellation;
        public String name;
    }
    public static class ConstellationLinesMapping {
        public DataObjects.Constellation constellation;
        public List<ConstellationLine> lines;

        public ConstellationLinesMapping(DataObjects.Constellation constellation, List<DataObjects.ConstellationLine> lines) {
            this.constellation = constellation;
            this.lines = lines;
        }
    }

}