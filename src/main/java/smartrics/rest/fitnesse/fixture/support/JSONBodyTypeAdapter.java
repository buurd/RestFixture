/*  Copyright 2008 Fabrizio Cannizzo
 *
 *  This file is part of RestFixture.
 *
 *  RestFixture (http://code.google.com/p/rest-fixture/) is free software:
 *  you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  RestFixture is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with RestFixture.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  If you want to contact the author please leave a comment here
 *  http://smartrics.blogspot.com/2008/08/get-fitnesse-with-some-rest.html
 */
package smartrics.rest.fitnesse.fixture.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JSONBodyTypeAdapter extends XPathBodyTypeAdapter {

	private static Log LOG = LogFactory.getLog(JSONBodyTypeAdapter.class);

	@Override
	protected boolean eval(String expr, String json) {
		String xml = Tools.fromJSONtoXML(json);
		return super.eval(expr, xml);
	}

	@Override
	public String toString(Object obj) {
		if (obj == null || obj.toString().trim().equals(""))
			return "no-body";
		// the actual value is passed as an xml string
		// todo: pretty print?
		return Tools.toJSON(obj.toString());
	}

	@Override
	public String toXmlString(String content) {
		return Tools.fromJSONtoXML(content);
	}

}
