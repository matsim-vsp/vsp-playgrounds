/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mdziakowski.activityReLocation;

import org.matsim.contrib.accessibility.FacilityTypes;

import java.util.Map;
import java.util.TreeMap;

/**
 * copied from dziemke
 */

public class OSMKeyForLeisure {

    public static Map<String, String> buildOsmAllLeisureToMatsimTypeMap() {
        Map<String, String> map = new TreeMap<String, String>();

        map.put("bar", FacilityTypes.LEISURE);
        map.put("bbq", FacilityTypes.LEISURE);
        map.put("biergarten", FacilityTypes.LEISURE);
        map.put("cafe", FacilityTypes.LEISURE);
        map.put("drinking_water", FacilityTypes.IGNORE);
        map.put("fast_food", FacilityTypes.LEISURE);
        map.put("food_court", FacilityTypes.LEISURE);
        map.put("ice_cream", FacilityTypes.LEISURE);
        map.put("pub", FacilityTypes.LEISURE);
        map.put("restaurant", FacilityTypes.LEISURE);
        // "education" section in osm wiki
        map.put("college", FacilityTypes.EDUCATION);
        map.put("kindergarten", FacilityTypes.EDUCATION);
        map.put("library", FacilityTypes.OTHER);
        map.put("archive", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("public_bookcase", FacilityTypes.IGNORE);
        map.put("music_school", FacilityTypes.EDUCATION); // Added on 2018-12-12
        map.put("driving_school", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("language_school", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("school", FacilityTypes.EDUCATION);
        map.put("university", FacilityTypes.EDUCATION);
        map.put("research_institute", FacilityTypes.EDUCATION);
        // "transportation" section in osm wiki
        map.put("bicycle_parking", FacilityTypes.IGNORE);
        map.put("bicycle_repair_station", FacilityTypes.IGNORE);
        map.put("bicycle_rental", FacilityTypes.OTHER);
        map.put("boat_rental", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("boat_sharing", FacilityTypes.IGNORE);
        map.put("bus_station", FacilityTypes.IGNORE);
        map.put("car_rental", FacilityTypes.OTHER);
        map.put("car_sharing", FacilityTypes.IGNORE);
        map.put("car_wash", FacilityTypes.OTHER);
        map.put("vehicle_inspection", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("charging_station", FacilityTypes.IGNORE);
        map.put("ferry_terminal", FacilityTypes.IGNORE);
        map.put("fuel", FacilityTypes.OTHER); // used to be "t"
        map.put("grit_bin", FacilityTypes.IGNORE);
        map.put("motorcycle_parking", FacilityTypes.IGNORE);
        map.put("parking", FacilityTypes.IGNORE);
        map.put("parking_entrance", FacilityTypes.IGNORE);
        map.put("parking_space", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("taxi", FacilityTypes.IGNORE);
        map.put("ticket_validator", FacilityTypes.IGNORE); // Added on 2018-12-12
        // "financial" section in osm wiki
        map.put("atm", FacilityTypes.OTHER);
        map.put("bank", FacilityTypes.OTHER);
        map.put("bureau_de_change", FacilityTypes.OTHER);
        // "healthcare" section in osm wiki
        map.put("baby_hatch", FacilityTypes.IGNORE);
        map.put("clinic", FacilityTypes.MEDICAL);
        map.put("dentist", FacilityTypes.MEDICAL);
        map.put("doctors", FacilityTypes.MEDICAL);
        map.put("hospital", FacilityTypes.MEDICAL);
        map.put("nursing_home", FacilityTypes.MEDICAL);
        map.put("pharmacy", FacilityTypes.MEDICAL);
        map.put("social_facility", FacilityTypes.IGNORE);
        map.put("veterinary", FacilityTypes.IGNORE);
        map.put("blood_donation", FacilityTypes.IGNORE);
        // "entertainment, arts & culture" section in osm wiki
        map.put("arts_centre", FacilityTypes.LEISURE);
        map.put("brothel", FacilityTypes.LEISURE);
        map.put("casino", FacilityTypes.LEISURE);
        map.put("cinema", FacilityTypes.LEISURE);
        map.put("community_centre", FacilityTypes.IGNORE);
        map.put("fountain", FacilityTypes.IGNORE);
        map.put("gambling", FacilityTypes.LEISURE);
        map.put("nightclub", FacilityTypes.LEISURE);
        map.put("planetarium", FacilityTypes.LEISURE);
        map.put("social_centre", FacilityTypes.OTHER);
        map.put("stripclub", FacilityTypes.LEISURE);
        map.put("studio", FacilityTypes.LEISURE);
        map.put("swingerclub", FacilityTypes.LEISURE);
        map.put("theatre", FacilityTypes.LEISURE);
        // "other" section in osm wiki
        map.put("animal_boarding", FacilityTypes.IGNORE);
        map.put("animal_shelter", FacilityTypes.IGNORE);
        map.put("baking_oven", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("bench", FacilityTypes.IGNORE);
        map.put("clock", FacilityTypes.IGNORE);
        map.put("courthouse", FacilityTypes.IGNORE);
        map.put("coworking_space", FacilityTypes.WORK);
        map.put("crematorium", FacilityTypes.IGNORE);
        map.put("crypt", FacilityTypes.IGNORE);
        map.put("dive_centre", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("dojo", FacilityTypes.IGNORE);
        map.put("embassy", FacilityTypes.OTHER);
        map.put("fire_station", FacilityTypes.WORK);
        map.put("game_feeding", FacilityTypes.LEISURE);
        map.put("grave_yard", FacilityTypes.IGNORE);
        map.put("gym", FacilityTypes.LEISURE); // Use "leisure=fitness_centre" instead
        map.put("hunting_stand", FacilityTypes.IGNORE);
        map.put("internet_cafe", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("kneipp_water_cure", FacilityTypes.IGNORE);
        map.put("marketplace", FacilityTypes.SHOPPING);
        map.put("photo_booth", FacilityTypes.LEISURE);
        map.put("place_of_worship", FacilityTypes.OTHER);
        map.put("police", FacilityTypes.POLICE);
        map.put("post_box", FacilityTypes.IGNORE);
        map.put("post_office", FacilityTypes.OTHER);
        map.put("prison", FacilityTypes.WORK);
        map.put("public_bath", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("ranger_station", FacilityTypes.IGNORE);
        // map.put("register_office", FacilityTypes.IGNORE); // Removed on 2018-12-12
        map.put("recycling", FacilityTypes.IGNORE);
        map.put("rescue_station", FacilityTypes.IGNORE);
        map.put("sanitary_dump_station", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("sauna", FacilityTypes.LEISURE); // Use "leisure=sauna" instead
        map.put("shelter", FacilityTypes.IGNORE);
        map.put("shower", FacilityTypes.IGNORE);
        map.put("table", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("telephone", FacilityTypes.IGNORE);
        map.put("toilets", FacilityTypes.IGNORE);
        map.put("townhall", FacilityTypes.OTHER);
        map.put("vending_machine", FacilityTypes.IGNORE);
        map.put("waste_basket", FacilityTypes.IGNORE);
        map.put("waste_disposal", FacilityTypes.IGNORE);
        map.put("waste_transfer_station", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("watering_place", FacilityTypes.IGNORE);
        map.put("water_point", FacilityTypes.IGNORE);

        map.put("adult_gaming_centre", FacilityTypes.LEISURE);
        map.put("amusement_arcade", FacilityTypes.LEISURE);
        map.put("beach_resort", FacilityTypes.LEISURE);
        map.put("bandstand", FacilityTypes.IGNORE);
        map.put("bird_hide", FacilityTypes.IGNORE);
        map.put("bowling_alley", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("common", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("dance", FacilityTypes.LEISURE);
        map.put("disc_golf_course", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("dog_park", FacilityTypes.LEISURE);
        map.put("escape_game", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("firepit", FacilityTypes.LEISURE);
        map.put("fishing", FacilityTypes.IGNORE);
        map.put("fitness_centre", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("garden", FacilityTypes.LEISURE);
        map.put("golf_course", FacilityTypes.LEISURE);
        map.put("hackerspace", FacilityTypes.LEISURE);
        map.put("horse_riding", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("ice_rink", FacilityTypes.LEISURE);
        map.put("marina", FacilityTypes.IGNORE);
        map.put("miniature_golf", FacilityTypes.LEISURE);
        map.put("nature_reserve", FacilityTypes.IGNORE);
        map.put("park", FacilityTypes.IGNORE);
        map.put("picnic_table", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("pitch", FacilityTypes.LEISURE);
        map.put("playground", FacilityTypes.LEISURE);
        map.put("recreation_ground", FacilityTypes.LEISURE); // not in Wiki, but in tags; Added on 2018-12-12
        map.put("sauna", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("slipway", FacilityTypes.IGNORE);
        map.put("sports_centre", FacilityTypes.LEISURE);
        map.put("stadium", FacilityTypes.LEISURE);
        map.put("summer_camp", FacilityTypes.LEISURE);
        map.put("swimming_pool", FacilityTypes.LEISURE);
        map.put("swimming_area", FacilityTypes.LEISURE);
        map.put("track", FacilityTypes.LEISURE);
        map.put("water_park", FacilityTypes.LEISURE);
        map.put("wildlife_hide", FacilityTypes.IGNORE);

        return map;
    }

    public static Map<String, String> buildOsmAmenityToMatsimTypeMapV2(){
        Map<String, String> map = new TreeMap<String, String>();
        // "subsistence" section in osm wiki
        map.put("bar", FacilityTypes.LEISURE);
        map.put("bbq", FacilityTypes.LEISURE);
        map.put("biergarten", FacilityTypes.LEISURE);
        map.put("cafe", FacilityTypes.LEISURE);
        map.put("drinking_water", FacilityTypes.IGNORE);
        map.put("fast_food", FacilityTypes.LEISURE);
        map.put("food_court", FacilityTypes.LEISURE);
        map.put("ice_cream", FacilityTypes.LEISURE);
        map.put("pub", FacilityTypes.LEISURE);
        map.put("restaurant", FacilityTypes.LEISURE);
        // "education" section in osm wiki
        map.put("college", FacilityTypes.EDUCATION);
        map.put("kindergarten", FacilityTypes.EDUCATION);
        map.put("library", FacilityTypes.OTHER);
        map.put("archive", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("public_bookcase", FacilityTypes.IGNORE);
        map.put("music_school", FacilityTypes.EDUCATION); // Added on 2018-12-12
        map.put("driving_school", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("language_school", FacilityTypes.OTHER); // Added on 2018-12-12
        map.put("school", FacilityTypes.EDUCATION);
        map.put("university", FacilityTypes.EDUCATION);
        map.put("research_institute", FacilityTypes.EDUCATION);
        // "transportation" section in osm wiki
        map.put("bicycle_parking", FacilityTypes.IGNORE);
        map.put("bicycle_repair_station", FacilityTypes.IGNORE);
        map.put("bicycle_rental", FacilityTypes.OTHER);
        map.put("boat_rental", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("boat_sharing", FacilityTypes.IGNORE);
        map.put("bus_station", FacilityTypes.IGNORE);
        map.put("car_rental", FacilityTypes.OTHER);
        map.put("car_sharing", FacilityTypes.IGNORE);
        map.put("car_wash", FacilityTypes.OTHER);
        map.put("vehicle_inspection", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("charging_station", FacilityTypes.IGNORE);
        map.put("ferry_terminal", FacilityTypes.IGNORE);
        map.put("fuel", FacilityTypes.OTHER); // used to be "t"
        map.put("grit_bin", FacilityTypes.IGNORE);
        map.put("motorcycle_parking", FacilityTypes.IGNORE);
        map.put("parking", FacilityTypes.IGNORE);
        map.put("parking_entrance", FacilityTypes.IGNORE);
        map.put("parking_space", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("taxi", FacilityTypes.IGNORE);
        map.put("ticket_validator", FacilityTypes.IGNORE); // Added on 2018-12-12
        // "financial" section in osm wiki
        map.put("atm", FacilityTypes.OTHER);
        map.put("bank", FacilityTypes.OTHER);
        map.put("bureau_de_change", FacilityTypes.OTHER);
        // "healthcare" section in osm wiki
        map.put("baby_hatch", FacilityTypes.IGNORE);
        map.put("clinic", FacilityTypes.MEDICAL);
        map.put("dentist", FacilityTypes.MEDICAL);
        map.put("doctors", FacilityTypes.MEDICAL);
        map.put("hospital", FacilityTypes.MEDICAL);
        map.put("nursing_home", FacilityTypes.MEDICAL);
        map.put("pharmacy", FacilityTypes.MEDICAL);
        map.put("social_facility", FacilityTypes.IGNORE);
        map.put("veterinary", FacilityTypes.IGNORE);
        map.put("blood_donation", FacilityTypes.IGNORE);
        // "entertainment, arts & culture" section in osm wiki
        map.put("arts_centre", FacilityTypes.LEISURE);
        map.put("brothel", FacilityTypes.LEISURE);
        map.put("casino", FacilityTypes.LEISURE);
        map.put("cinema", FacilityTypes.LEISURE);
        map.put("community_centre", FacilityTypes.IGNORE);
        map.put("fountain", FacilityTypes.IGNORE);
        map.put("gambling", FacilityTypes.LEISURE);
        map.put("nightclub", FacilityTypes.LEISURE);
        map.put("planetarium", FacilityTypes.LEISURE);
        map.put("social_centre", FacilityTypes.OTHER);
        map.put("stripclub", FacilityTypes.LEISURE);
        map.put("studio", FacilityTypes.LEISURE);
        map.put("swingerclub", FacilityTypes.LEISURE);
        map.put("theatre", FacilityTypes.LEISURE);
        // "other" section in osm wiki
        map.put("animal_boarding", FacilityTypes.IGNORE);
        map.put("animal_shelter", FacilityTypes.IGNORE);
        map.put("baking_oven", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("bench", FacilityTypes.IGNORE);
        map.put("clock", FacilityTypes.IGNORE);
        map.put("courthouse", FacilityTypes.IGNORE);
        map.put("coworking_space", FacilityTypes.WORK);
        map.put("crematorium", FacilityTypes.IGNORE);
        map.put("crypt", FacilityTypes.IGNORE);
        map.put("dive_centre", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("dojo", FacilityTypes.IGNORE);
        map.put("embassy", FacilityTypes.OTHER);
        map.put("fire_station", FacilityTypes.WORK);
        map.put("game_feeding", FacilityTypes.LEISURE);
        map.put("grave_yard", FacilityTypes.IGNORE);
        map.put("gym", FacilityTypes.LEISURE); // Use "leisure=fitness_centre" instead
        map.put("hunting_stand", FacilityTypes.IGNORE);
        map.put("internet_cafe", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("kneipp_water_cure", FacilityTypes.IGNORE);
        map.put("marketplace", FacilityTypes.SHOPPING);
        map.put("photo_booth", FacilityTypes.LEISURE);
        map.put("place_of_worship", FacilityTypes.OTHER);
        map.put("police", FacilityTypes.POLICE);
        map.put("post_box", FacilityTypes.IGNORE);
        map.put("post_office", FacilityTypes.OTHER);
        map.put("prison", FacilityTypes.WORK);
        map.put("public_bath", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("ranger_station", FacilityTypes.IGNORE);
        // map.put("register_office", FacilityTypes.IGNORE); // Removed on 2018-12-12
        map.put("recycling", FacilityTypes.IGNORE);
        map.put("rescue_station", FacilityTypes.IGNORE);
        map.put("sanitary_dump_station", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("sauna", FacilityTypes.LEISURE); // Use "leisure=sauna" instead
        map.put("shelter", FacilityTypes.IGNORE);
        map.put("shower", FacilityTypes.IGNORE);
        map.put("table", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("telephone", FacilityTypes.IGNORE);
        map.put("toilets", FacilityTypes.IGNORE);
        map.put("townhall", FacilityTypes.OTHER);
        map.put("vending_machine", FacilityTypes.IGNORE);
        map.put("waste_basket", FacilityTypes.IGNORE);
        map.put("waste_disposal", FacilityTypes.IGNORE);
        map.put("waste_transfer_station", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("watering_place", FacilityTypes.IGNORE);
        map.put("water_point", FacilityTypes.IGNORE);

        return map;
    }

    public static Map<String, String> buildOsmLeisureToMatsimTypeMapV2(){
        Map<String, String> map = new TreeMap<String, String>();
        map.put("adult_gaming_centre", FacilityTypes.LEISURE);
        map.put("amusement_arcade", FacilityTypes.LEISURE);
        map.put("beach_resort", FacilityTypes.LEISURE);
        map.put("bandstand", FacilityTypes.IGNORE);
        map.put("bird_hide", FacilityTypes.IGNORE);
        map.put("bowling_alley", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("common", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("dance", FacilityTypes.LEISURE);
        map.put("disc_golf_course", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("dog_park", FacilityTypes.LEISURE);
        map.put("escape_game", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("firepit", FacilityTypes.LEISURE);
        map.put("fishing", FacilityTypes.IGNORE);
        map.put("fitness_centre", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("garden", FacilityTypes.LEISURE);
        map.put("golf_course", FacilityTypes.LEISURE);
        map.put("hackerspace", FacilityTypes.LEISURE);
        map.put("horse_riding", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("ice_rink", FacilityTypes.LEISURE);
        map.put("marina", FacilityTypes.IGNORE);
        map.put("miniature_golf", FacilityTypes.LEISURE);
        map.put("nature_reserve", FacilityTypes.IGNORE);
        map.put("park", FacilityTypes.IGNORE);
        map.put("picnic_table", FacilityTypes.IGNORE); // Added on 2018-12-12
        map.put("pitch", FacilityTypes.LEISURE);
        map.put("playground", FacilityTypes.LEISURE);
        map.put("recreation_ground", FacilityTypes.LEISURE); // not in Wiki, but in tags; Added on 2018-12-12
        map.put("sauna", FacilityTypes.LEISURE); // Added on 2018-12-12
        map.put("slipway", FacilityTypes.IGNORE);
        map.put("sports_centre", FacilityTypes.LEISURE);
        map.put("stadium", FacilityTypes.LEISURE);
        map.put("summer_camp", FacilityTypes.LEISURE);
        map.put("swimming_pool", FacilityTypes.LEISURE);
        map.put("swimming_area", FacilityTypes.LEISURE);
        map.put("track", FacilityTypes.LEISURE);
        map.put("water_park", FacilityTypes.LEISURE);
        map.put("wildlife_hide", FacilityTypes.IGNORE);
        return map;
    }

    public static Map<String, String> buildOsmTourismToMatsimTypeMapV2(){
        Map<String, String> map = new TreeMap<String, String>();
        map.put("alpine_hut", FacilityTypes.LEISURE);
        map.put("apartment", FacilityTypes.IGNORE);
        map.put("aquarium", FacilityTypes.LEISURE);
        map.put("attraction", FacilityTypes.IGNORE);
        map.put("artwork", FacilityTypes.IGNORE);
        map.put("camp_site", FacilityTypes.LEISURE);
        map.put("caravan_site", FacilityTypes.LEISURE);
        map.put("chalet", FacilityTypes.IGNORE);
        map.put("gallery", FacilityTypes.LEISURE);
        map.put("guest_house", FacilityTypes.IGNORE);
        map.put("hostel", FacilityTypes.IGNORE);
        map.put("hotel", FacilityTypes.IGNORE);
        map.put("information", FacilityTypes.IGNORE);
        map.put("motel", FacilityTypes.IGNORE);
        map.put("museum", FacilityTypes.LEISURE);
        map.put("picnic_site", FacilityTypes.LEISURE);
        map.put("theme_park", FacilityTypes.LEISURE);
        map.put("viewpoint", FacilityTypes.IGNORE);
        map.put("wilderness_hut", FacilityTypes.LEISURE);
        map.put("zoo", FacilityTypes.LEISURE);
        return map;
    }

}
