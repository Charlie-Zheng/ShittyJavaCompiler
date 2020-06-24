import java.util.Stack;

/**
 * 
 */

/**
 * An abstract syntax tree. Contains nodes. Stores the root node of the tree.
 * @author charl
 */
public class Tree {

	/**
	 * @param args
	 */
	TreeNode root;

	//	TreeNode current;
	public Tree(TreeNode root) {
		this.root = root;
		//		this.current = root;
	}

	/**
	 * Outputs a String representation of this tree. The information about each node
	 * is enclosed in { }. The children of the node are enclosed in [ ]. The string
	 * is formatting into lines and appropriate tabbing is utilized. Nodes which are
	 * more tabbed having larger depth in the tree. This function recursively calls
	 * TreeNode.toString on the root, which recursively calls TreeNode.toString on
	 * each of that node's children. This may affect certain runtimes of Java due to
	 * the recursive depth limit.
	 * 
	 * @return A string representation of the tree, starting at the root
	 */
	@Override
	public String toString() {
		return root.toTabbedString(0);
	}

}
