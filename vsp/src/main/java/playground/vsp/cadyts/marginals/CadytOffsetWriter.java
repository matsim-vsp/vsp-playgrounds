package playground.vsp.cadyts.marginals;

import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.DynamicDataXMLFileIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.units.qual.K;
import org.matsim.api.core.v01.Id;
import playground.vsp.cadyts.marginals.prep.DistanceBin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static cadyts.utilities.misc.XMLHelpers.writeAttr;

public class CadytOffsetWriter extends DynamicDataXMLFileIO<Id<DistanceBin>> {


	@Override
	protected String key2attrValue(Id<DistanceBin> key) {
		return key.toString();
	}

	@Override
	protected Id<DistanceBin> attrValue2key(String string) {
		return Id.create(string, DistanceBin.class);
	}
}
