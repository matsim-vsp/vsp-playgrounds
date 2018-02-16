/**
 * 
 */
package signals.laemmer.model.util;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * ObjectAttributeClass with getter for attributes map to iterate over it.
 * @author pschade
 *
 */
public class PsObjectAttributes extends ObjectAttributes {

	/*package*/ Map<String, Map<String, Object>> attributes = new LinkedHashMap<String, Map<String, Object>>(1000);
	
	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		for ( Entry<String, Map<String,Object>> entry : attributes.entrySet() ) {
			String key = entry.getKey() ;
			stb.append("key=").append(key);
			Map<String,Object> map = entry.getValue() ;
			for ( Entry<String,Object> ee : map.entrySet() ) {
				String subkey = ee.getKey();
				stb.append("; subkey=").append(subkey);
				stb.append("; object=").append(ee.getValue().toString());
			}
			stb.append("\n") ;
		}
		return stb.toString() ;
	}
	
	public Set<Entry<String, Map<String, Object>>> getAttriutesAsEntrySet(){
		return attributes.entrySet();
	}
	
	@Override
	public Object putAttribute(final String objectId, final String attribute, final Object value) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			attMap = new IdentityHashMap<String, Object>(5);
			this.attributes.put(objectId, attMap);
		}
		return attMap.put(attribute.intern(), value);
	}

	@Override
	public Object getAttribute(final String objectId, final String attribute) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			return null;
		}
		return attMap.get(attribute.intern());
	}

	@Override
	public Object removeAttribute(final String objectId, final String attribute) {
		Map<String, Object> attMap = this.attributes.get(objectId);
		if (attMap == null) {
			return null;
		}
		return attMap.remove(attribute.intern());
	}

	@Override
	public void removeAllAttributes(final String objectId) {
		this.attributes.remove(objectId);
	}

	/**
	 * Deletes all attributes of all objects, and all objects-ids.
	 */
	public void clear() {
		this.attributes.clear();
	}
}
