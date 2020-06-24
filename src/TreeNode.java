import java.util.ArrayList;
import java.util.Collections;

import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * 
 */

/**
 * A single node inside a Abstract Syntax Tree. Stores the location, type,
 * attribute, and children of a node
 * 
 * @author charl
 */
public class TreeNode {
	protected ArrayList<TreeNode> children = new ArrayList<TreeNode>();
	protected TreeNodeType type;
	protected String attr;
	protected Location loc;
	protected String sig;
	/**
	 * @return the scope of the node
	 */
	public Scope getScope() {
		return scope;
	}

	protected Scope scope;

	/**
	 * Adds the specified signature and scope to this node
	 * 
	 * @param sig
	 * @param scope
	 */
	public void annotate(String sig, Scope scope) {
		this.sig = sig;
		this.scope = scope;
	}

	/**
	 * Adds the specified signature to this node
	 * 
	 * @param sig
	 */
	public void annotate(String sig) {
		this.sig = sig;
	}

	/**
	 * Adds the specified scope to this node
	 * 
	 * @param scope
	 */
	public void annotate(Scope scope) {
		this.scope = scope;
	}

	/**
	 * Sets the type and location of this node to the user specified values
	 * 
	 * @param type
	 *            The type of the node
	 * @param loc
	 *            The location of the node. Used to output information to the user
	 */
	public TreeNode(TreeNodeType type, Location loc) {
		this.type = type;
		this.loc = loc;
	}

	/**
	 * Sets the type of this node to the user specified value. The location will be
	 * null
	 * 
	 * @param type
	 *            The type of the node
	 */
	public TreeNode(TreeNodeType type) {
		this.type = type;
	}

	/**
	 * @return the children
	 */
	public ArrayList<TreeNode> getChildren() {
		return children;
	}

	/**
	 * @return the type
	 */
	public TreeNodeType getType() {
		return type;
	}

	/**
	 * @return the loc
	 */
	public Location getLoc() {
		return loc;
	}

	/**
	 * Appends the input TreeNode to the ArrayList of children of this node
	 * 
	 * @param child
	 *            The child to be appended
	 * @return This TreeNode
	 */
	public TreeNode addChild(TreeNode child) {
		if (child != null)
			children.add(child);
		return this;
	}

	/**
	 * Appends a null child to the ArrayList of children of this node
	 * 
	 * @return This TreeNode
	 */
	public TreeNode addChild() {
		children.add(null);
		return this;
	}

	/**
	 * Reverses the order of all children of this node
	 * 
	 * @return This TreeNode
	 */
	public TreeNode reverseChildren() {
		Collections.reverse(children);
		return this;
	}

	/**
	 * Appends all the children of the input TreeNode to the ArrayList of children
	 * of this node
	 * 
	 * @param node
	 *            The input TreeNode
	 * @return This TreeNode
	 */
	public TreeNode addChildren(TreeNode node) {
		if (node != null) {
			for (TreeNode child : node.children) {
				addChild(child);
			}
		}
		return this;
	}

	/**
	 * Getter for a child. The index-th child of this node will be returned
	 * 
	 * @param index
	 *            The index
	 * @return The index-th child
	 */
	public TreeNode getChild(int index) {
		return children.get(index);
	}

	/**
	 * Sets the index-th child of this node to be the input TreeNode
	 * 
	 * @param index
	 *            The index
	 * @param child
	 *            The node that will become the index-th child of this node
	 * @return This TreeNode
	 */
	public TreeNode setChild(int index, TreeNode child) {
		children.set(index, child);
		return this;
	}

	/**
	 * Sets the attribute of this node to the input String
	 * 
	 * @param attr
	 *            The input string which will become the attribute of this node
	 * @return This TreeNode
	 */
	public TreeNode setAttr(String attr) {
		this.attr = attr;
		return this;
	}

	/**
	 * @return The attribute of this node
	 */
	public String getAttr() {
		return attr;
	}

	@Override
	public String toString() {
		//Output the information of this node, if the value is null, don't append it
		StringBuffer out = new StringBuffer();
		out.append(type + ": {");
		boolean addedInfo = false;
		if (loc != null) {
			out.append("line = ");
			out.append(loc.getLine());
			out.append(", ");
			addedInfo = true;
		}
		if (attr != null) {
			out.append("attr = ");
			out.append(attr);
			out.append(", ");
			addedInfo = true;
		}
		if (sig != null) {
			out.append("sig = ");
			out.append(sig);
			out.append(", ");
			addedInfo = true;
		}
		//If info was appended, replace the last character (which is a ',') with }
		if (addedInfo) {
			out.replace(out.length() - 2, out.length() - 1, "}");
		} else
			out.append('}');
		//Add the string representation of each of the children of this node between '[' ']' 
		return out.toString();

	}

	/**
	 * Returns a String representation of this node and all it's children. The
	 * information about this node is enclosed in { }. The children of this node are
	 * enclosed in [ ]. The string is unformatted. This function recursively calls
	 * child.toString on each of this node's children. This may affect certain
	 * runtimes of Java due to the recursive depth limit.
	 * 
	 * @return A string representation of this node and all it's children, with the
	 *         given tabbing
	 */
	protected String toTabbedString(int tabs) {
		//Output the information of this node, if the value is null, don't append it
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < tabs; i++) {
			out.append('\t');
		}
		out.append(type.name() + ": {");
		boolean addedInfo = false;
		if (loc != null) {
			out.append("line = ");
			out.append(loc.getLine());
			out.append(", ");
			addedInfo = true;
		}
		if (attr != null) {
			out.append("attr = ");
			out.append(attr);
			out.append(", ");
			addedInfo = true;
		}
		if (sig != null) {
			out.append("sig = ");
			out.append(sig);
			out.append(", ");
			addedInfo = true;
		}
		if (scope != null) {
			out.append("scope = ");
			out.append(scope.toString());
			out.append(", ");
			addedInfo = true;
		}
		//If info was appended, replace the last character (which is a ',') with }
		if (addedInfo) {
			out.replace(out.length() - 2, out.length() - 1, "}");
		} else
			out.append('}');
		//Add the string representation of each of the children of this node between '[' ']' 

		if (children.size() != 0) {
			out.append('[');
			for (TreeNode child : children) {
				if (child != null) {
					out.append('\n');
					out.append(child.toTabbedString(tabs + 1));
				} else {
					out.append("null");
				}
			}
			out.append('\n');
			for (int i = 0; i < tabs; i++) {
				out.append('\t');
			}
			out.append(']');
		}

		return out.toString();
	}
}
