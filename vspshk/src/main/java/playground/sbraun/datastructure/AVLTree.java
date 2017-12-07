package playground.sbraun.datastructure;

/**
 * @author sbraun
 *
 */


public class AVLTree<T> {

    private Node root;

    // innere abstrakte Klasse Node
    private abstract class Node {

        public int key;
        public int height;

        public Node(int key) {
            this.key = key;
            this.height = 0;
        }

        public abstract Node insert(int key, T element);

        public abstract Node delete(int key);

        public abstract String toString(String tree, String ast);

        public abstract int getBalanceNumber();

    }

    // innere Klasse Fork
    private class Fork extends Node {

        private Node left;
        private Node right;

        // Konstruktor
        public Fork(int key) {
            super(key);
        }

        // erweiterter Konstruktor
        public Fork(int key, Node left, Node right) {
            super(key);
            this.left = left;
            this.right = right;
            updateHeight();
        }

        public void setLeft(Node left){
            this.left=left;
            updateHeight();
        }

        public void setRight(Node right){
            this.right=right;
            updateHeight();
        }

        private void updateHeight(){
            if (left==null)         { this.height = right.height; }
            else if (right == null) { this.height = left.height; }
            else this.height = Math.max(this.left.height, this.right.height)+1;
        }

        public int getBalanceNumber(){
            return right.height-left.height;
        }

        // Linksrotation am Knoten a.
        private Fork singleRotateLeft() {
            // Voraussetzung: a und a.right sind Verzweigungen (Fork).
            Fork b = (Fork) this.right;
            this.setRight(b.left);
            b.setLeft(this);
            return b;
        }

        private Fork singleRotateRight() {
            Fork b = (Fork) this.left;
			this.setLeft(b.right);
			b.setRight(this);
            return b;
        }

        // Doppelrotation links bzgl. Knoten a.
        private Fork doubleRotateRightLeft() {
            // Voraussetzung: a, a.right, a.right.left sind
            this.setRight( ((Fork) this.right).singleRotateRight());
            // Verzweigungen
            return this.singleRotateLeft();

        }

        private Fork doubleRotateLeftRight() {
            this.setLeft( ((Fork) this.left).singleRotateLeft());
			return this.singleRotateRight();
        }

        public Node insert(int key, T element) {
            if (key <= this.key) {
                setLeft(this.left.insert(key, element));
            } else {
                setRight(this.right.insert(key, element));
            }
            return repairAVL();
        }

        public Node delete(int key) {
            if (key<=this.key){
				setLeft(this.left.delete(key));
				if (this.left == null) return right;
			} else {
				setRight(this.right.delete(key));
				if (this.right == null) return left;
			}
			return repairAVL();
        }

        private Node repairAVL(){
            if (getBalanceNumber()>1){
				if (right.getBalanceNumber()>=0){
					return singleRotateLeft();
				} else return doubleRotateRightLeft();
			}
			if (getBalanceNumber()<-1){
				if (left.getBalanceNumber()<=0){
					return singleRotateRight();
				} else return doubleRotateLeftRight();
			}
			
			return this;
        }

        public String toString(String tree, String ast) {
            if (right != null)
                tree = right.toString(tree, ast + "| ");
            tree += ast + "|-" + key + "\n";
            if (left != null)
                tree = left.toString(tree, ast + "| ");
            return tree;
        }

    }

    // innere Klasse Leaf
    private class Leaf extends Node {

        private T value;

        public T getVal() {
            return value;
        }

        public Leaf(int key, T value) {
            super(key);
            this.value = value;
        }

        public int getBalanceNumber(){
            return 0;
        }

        public Node insert(int key, T element) {
            Node newLeaf = new Leaf(key, element);
            if (key < this.key) {
                return new Fork(key, newLeaf, this);
            } else if (key > this.key) {
                return new Fork(this.key, this, newLeaf);
            }
            return newLeaf;
        }

        public Node delete(int key) {
            if (key == this.key) return null;
            else return this;
        }

        public String toString(String tree, String ast) {
            tree += ast + "|-" + key + " " + getVal().toString() + "\n";
            return tree;
        }

    }

    // Konstruktor
    public AVLTree() {
        this.root = null;
    }

    // parametrisierter Konstruktor
    public AVLTree(int key, T data) {
        this.root = new Leaf(key, data);
    }


    // Einfuegen eines neuen Elements mit gegebenem Schluessel in den Baum.
    public void insert(int key, T element) {
        if (root == null) {
            root = new Leaf(key, element);
        } else {
            root = root.insert(key, element);
        }
    }

    // Loeschen eines Elements mit gegebenem Schluessel aus dem Baum.
    public void delete(int key) {
        if (root == null) return;
        root = root.delete(key);
    }

    // Gibt eine String-Repraesentation des Baumes zurueck.
    public String toString() {
        String tree = root.toString("", "");
        return tree;
    }

    // Gibt die String-Repraesentation des Baumes auf der Konsole aus.
    public void print() {
        System.out.println(this.toString());
    }
    
    public int getRootbalance() {
    	return root.getBalanceNumber();
    }
}
