package signals.laemmer.model.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.spark_project.guava.reflect.TypeToken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.util.Types;

public class ConflictsConverter implements AttributeConverter<signals.laemmer.model.util.Conflicts> {

	private Gson converter;
	
	public ConflictsConverter() {
		GsonBuilder gsb = new GsonBuilder();
		gsb.enableComplexMapKeySerialization();
		gsb.serializeNulls();
		gsb.registerTypeAdapter(Types.newParameterizedType(Id.class, Lane.class), new LaneIdTypeAdapter());
		gsb.registerTypeAdapter(Types.newParameterizedType(Id.class, Link.class), new LinkIdTypeAdapter());
		converter = gsb.create();
	}

	@Override
	public Conflicts convert(String value) {
		Map<String, TreeMap<Id<Link>, List<Id<Lane>>>> serialisationMap = converter.fromJson(value, new TypeToken<HashMap<String, TreeMap<Id<Link>, LinkedList<Id<Lane>>>>>(){private static final long serialVersionUID = 1L;}.getType());
		Conflicts conflicts;
		if (serialisationMap.get("ids").firstEntry().getValue() == null)
			conflicts = new Conflicts(serialisationMap.get("ids").firstEntry().getKey());
		else
			conflicts = new Conflicts(serialisationMap.get("ids").firstEntry().getKey(), serialisationMap.get("ids").firstEntry().getValue().get(0));

		if (serialisationMap.containsKey("conflicts")) {
			for (Map.Entry<Id<Link>, List<Id<Lane>>> e : serialisationMap.get("conflicts").entrySet()) {
				if (e.getValue() == null) {
					conflicts.addConflict(e.getKey());
				} else {
					for (Id<Lane> laneId : e.getValue()) {
						conflicts.addConflict(e.getKey(), laneId);
					}
				}
			}
		}
		
		if (serialisationMap.containsKey("allowedConflictsPriority")) {
			for (Map.Entry<Id<Link>, List<Id<Lane>>> e : serialisationMap.get("allowedConflictsPriority").entrySet()) {
				if (e.getValue() == null) {
					conflicts.addAllowedConflictPriority(e.getKey());
				} else {
					for (Id<Lane> laneId : e.getValue()) {
						conflicts.addAllowedConflictPriority(e.getKey(), laneId);
					}
				}
			}
		}
		
		if (serialisationMap.containsKey("allowedConflictsNonPriority")) {
			for (Map.Entry<Id<Link>, List<Id<Lane>>> e : serialisationMap.get("allowedConflictsNonPriority").entrySet()) {
				if (e.getValue() == null) {
					conflicts.addAllowedConflictNonPriority(e.getKey());
				} else {
					for (Id<Lane> laneId : e.getValue()) {
						conflicts.addAllowedConflictNonPriority(e.getKey(), laneId);
					}
				}
			}
		}
		return conflicts;
	}
	
	@Override
	public String convertToString(Object o) {
		Conflicts conflicts = (Conflicts) o;
//		Gson gson = new Gson(); // json = new JSONObject(serialisationMap);
//		List<Id<Lane>> list = new LinkedList();
//		Map<String, List<Id<Lane>>> tmap = new TreeMap<>();
//		Id<Lane> lid = Id.create("i", Lane.class);
//		Map<String, Map<String, List<Id<Lane>>>> hmap = new HashMap<>();
//		list.add(lid);
//		tmap.put("firstList", list);
//		hmap.put("test", tmap);
//		String gsonString = gson.toJson(hmap, new TypeToken<Map<String, Map<String, List<Id<Lane>>>>>(){}.getType());
//		System.out.println("lid: "+gsonString);
//		
//		System.out.println(hmap);
//		
//		hmap = null;
//		
//		hmap = gson.fromJson(gsonString, new TypeToken<Map<String, Map<String, List<Id<Lane>>>>>(){}.getType());
//		
//		System.out.println(hmap);
		
		HashMap<String, TreeMap<Id<Link>, List<Id<Lane>>>> serialisationMap = new HashMap<String, TreeMap<Id<Link>, List<Id<Lane>>>>();
		serialisationMap.put("ids", conflicts.getIdsForSerialisation());
		serialisationMap.put("conflicts", conflicts.getConflicts());
		serialisationMap.put("allowedConflictsPriority", conflicts.getAllowedConflictsPriority());
		serialisationMap.put("allowedConflictsNonPriority", conflicts.getAllowedConflictsNonPriority());
		return converter.toJson(serialisationMap, new TypeToken<HashMap<String, TreeMap<Id<Link>, LinkedList<Id<Lane>>>>>(){private static final long serialVersionUID = 1L;}.getType()).toString();
	}
	
	private class LaneIdTypeAdapter extends TypeAdapter<Id<Lane>>{

		@Override
		public void write(JsonWriter writer, Id<Lane> id) throws IOException {
			writer.value(id.toString());
		}

		@Override
		public Id<Lane> read(JsonReader reader) throws IOException {
			return Id.create(reader.nextString(), Lane.class);
		}
	}
	
	private class LinkIdTypeAdapter extends TypeAdapter<Id<Link>>{

		@Override
		public void write(JsonWriter writer, Id<Link> id) throws IOException {
			writer.value(id.toString());
		}

		@Override
		public Id<Link> read(JsonReader reader) throws IOException {
			return Id.createLinkId(reader.nextString());
		}
	}

}
