/*******************************************************************************
 * Copyright (C) 2011 Stefan Schr�der.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.basics;


import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class NodeFactory {
	public Node createNode(String id, int matrixId){
		Node node = new NodeImpl(id);
		node.setMatrixId(matrixId);
		return node;
	}
}
