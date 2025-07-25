class Block implements Cloneable {
	boolean block[];
	public final static boolean[] AESmod = { false, false, false, true, true, false, true, true };
	public final static Block AESmodulo = new Block(AESmod);

	public Block(int taille) {
		this.block = new boolean[taille];
	}

	public Block(int taille, int val) {
		this(taille);
		for (int i = taille - 1; i > -1; i--) {
			this.block[i] = ((val % 2) == 1);
			val /= 2;
		}
	}

	public Block(boolean[] s) {
		this(s.length);
		for (int i = 0; i < s.length; i++) {
			this.block[i] = s[i];
		}
	}

	public Block(String s) {
		this(s.length());
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '0')
				this.block[i] = false;
			else {
				if (s.charAt(i) == '1') {
					this.block[i] = true;
				} else {
					System.out.println("Block: bit " + i + " has value " + s.charAt(i) + " different from 0 or 1");
				}
			}
		}
	}

	public Block(Block[] blockList) {
		int taille = 0, cpt = 0;
		for (int i = 0; i < blockList.length; i++) {
			taille += blockList[i].block.length;
		}
		this.block = new boolean[taille];
		for (int i = 0; i < blockList.length; i++) {
			for (int j = 0; j < blockList[i].block.length; j++) {
				this.block[cpt++] = blockList[i].block[j];
			}
		}
	}

	public static Block[] stringToBlock(String chaine, int size) {
		Block[] toReturn = new Block[chaine.length() / size];
		for (int i = 0; i < toReturn.length; i++) {
			Block[] temp = new Block[size];
			for (int j = 0; j < size; j++) {
				Block octetBlock = new Block(8);
				char octet = chaine.charAt(i * size + j);
				for (int k = 0; k < 8; k++) {
					octetBlock.block[7 - k] = (octet % 2 == 1);
					octet = (char) (octet / 2);
				}
				temp[j] = octetBlock;
			}
			toReturn[i] = new Block(temp);
		}
		return toReturn;
	}

	public static String blockToString(Block[] blocks) {
		String toReturn = "";
		for (int i = 0; i < blocks.length; i++) {
			Block block = blocks[i];
			for (int j = 0; j < block.block.length / 8; j++) {
				char val = 0, pow2 = 1;
				for (int k = 0; k < 8; k++) {
					if (block.block[j * 8 + 7 - k]) {
						val += pow2;
					}
					pow2 *= 2;
				}
				toReturn += val;
			}
		}
		return toReturn;
	}

	public Block clone() {
		Block clone = new Block(this.block);
		return clone;
	}

	public String toString() {
		String result = "";
		for (int i = 0; i < this.block.length; i++) {
			result += this.block[i] ? "1" : "0";
		}
		return result;
	}

	public String toStringH() {
		String result = "";
		for (int i = 0; i < this.block.length; i += 4) {
			int val = (this.block[i] ? 8 : 0) + (this.block[i + 1] ? 4 : 0) + (this.block[i + 2] ? 2 : 0)
					+ (this.block[i + 3] ? 1 : 0);
			if (val < 10) {
				result += val;
			} else {
				result += ((char) ('A' + val - 10));
			}
		}
		return result;
	}

	public Block portion(int nbrPortion, int index) {
		boolean[] newBlock = new boolean[this.block.length / nbrPortion];
		for (int i = 0; i < newBlock.length; i++) {
			newBlock[i] = this.block[i + index * newBlock.length];
		}
		return new Block(newBlock);
	}

	public Block xOr(Block secondMember) {
		if (this.block.length != secondMember.block.length) {
			return null;
		}
		boolean[] result = new boolean[this.block.length];
		for (int i = 0; i < this.block.length; i++) {
			result[i] = this.block[i] ^ secondMember.block[i];
		}
		return new Block(result);
	}

	public Block leftShift() {
		boolean[] result = new boolean[this.block.length];
		for (int i = 0; i < this.block.length - 1; i++) {
			result[i] = this.block[i + 1];
		}
		result[this.block.length - 1] = false;
		return new Block(result);
	}

	public int rowValue() {
		int row = 0;
		for (int i = 0; i < 4; i++) {
			if (this.block[i]) {
				row += (1 << (3 - i));
			}
		}
		return row;
	}

	public int columnValue() {
		int col = 0;
		for (int i = 4; i < 8; i++) {
			if (this.block[i]) {
				col += (1 << (7 - i));
			}
		}
		return col;
	}

	public Block modularMultByX() {
		Block result = this.leftShift();
		if (this.block[0]) { // MSB check (correct)
			result = result.xOr(AESmodulo);
		}
		return result;
	}

	public Block modularMult(Block prod) {
		Block result = new Block(8);
		Block temp = this.clone();
		for (int i = 7; i >= 0; i--) { // Process LSB to MSB
			if (prod.block[i]) {
				result = result.xOr(temp);
			}
			temp = temp.modularMultByX(); // multiply by x
		}
		return result;
	}

	public Block g(SBox sbox, Block rc) {
		Block rotWord = new Block(32);

		// ✅ Byte-wise left rotation: [A][B][C][D] -> [B][C][D][A]
		for (int i = 0; i < 4; i++) {
			Block byteBlock = this.portion(4, (i + 1) % 4); // rotate by 1 byte
			for (int j = 0; j < 8; j++) {
				rotWord.block[i * 8 + j] = byteBlock.block[j];
			}
		}

		// ✅ SubBytes using SBox
		Block subBytes = new Block(32);
		for (int i = 0; i < 4; i++) {
			Block tempByte = new Block(8);
			for (int j = 0; j < 8; j++) {
				tempByte.block[j] = rotWord.block[i * 8 + j];
			}
			Block subbed = sbox.cypher(tempByte);
			for (int j = 0; j < 8; j++) {
				subBytes.block[i * 8 + j] = subbed.block[j];
			}
		}

		Block result = subBytes.xOr(rc);

		return result;
	}

	public Block next() {
		Block toReturn = new Block(this.block);
		int index = this.block.length;
		boolean stop = false;
		while (index > 0 && !stop) {
			index--;
			if (toReturn.block[index]) {
				toReturn.block[index] = false;
			} else {
				toReturn.block[index] = true;
				stop = true;
			}
		}
		return toReturn;
	}
}

class Key {
	private Block[] bytes;

	public Key() {
		this.bytes = new Block[4];
		for (int i = 0; i < 4; i++) {
			this.bytes[i] = new Block(32);
		}
	}

	public Key(Block block) {
		this.bytes = new Block[4];
		for (int i = 0; i < 4; i++) {
			this.bytes[i] = block.portion(4, i);
		}
	}

	public Key(Block[] blocks) {
		this.bytes = new Block[4];
		for (int i = 0; i < 4; i++) {
			this.bytes[i] = blocks[i].clone();
		}
	}

	public Key(Key toCopy) {
		this.bytes = new Block[4];
		for (int i = 0; i < 4; i++) {
			this.bytes[i] = toCopy.bytes[i].clone();
		}
	}

	public Key[] genSubKeys(SBox sbox) {
		Key[] subKeys = new Key[11];
		subKeys[0] = new Key(this);

		// Round constants for AES key expansion
		Block[] rcon = new Block[10];
		long[] rconValues = {
				0x01000000L, 0x02000000L, 0x04000000L, 0x08000000L,
				0x10000000L, 0x20000000L, 0x40000000L, 0x80000000L,
				0x1B000000L, 0x36000000L
		};

		for (int i = 0; i < 10; i++) {
			rcon[i] = new Block(32, rconValues[i]);
		}

		// Generate the remaining 10 round keys
		for (int i = 1; i < 11; i++) {
			subKeys[i] = new Key();

			Block temp = subKeys[i - 1].bytes[3].g(sbox, rcon[i - 1]);

			subKeys[i].bytes[0] = subKeys[i - 1].bytes[0].xOr(temp);

			for (int j = 1; j < 4; j++) {
				subKeys[i].bytes[j] = subKeys[i - 1].bytes[j].xOr(subKeys[i].bytes[j - 1]);
			}
		}

		return subKeys;
	}

	// difference : inversion entre i et j
	public Block elmnt(int i, int j) {
		return this.bytes[i].portion(4, j);
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < this.bytes.length; i++) {
			s += this.bytes[i].toString() + " ";
		}
		return s;
	}

	public String toStringH() {
		String s = "";
		for (int i = 0; i < this.bytes.length; i++) {
			s += this.bytes[i].toStringH() + " ";
		}
		return s;
	}
}

class State {
	private Block[][] bytes;

	public State() {
		this.bytes = new Block[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				this.bytes[i][j] = new Block(8);
			}
		}
	}

	public State(Block block) {
		this.bytes = new Block[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				this.bytes[i][j] = block.portion(16, i + j * 4);
			}
		}
	}

	public State(State toCopy) {
		this.bytes = new Block[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				this.bytes[i][j] = toCopy.bytes[i][j].clone();
			}
		}
	}

	public State(int[][] val) {
		this.bytes = new Block[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				this.bytes[i][j] = new Block(8, val[i][j]);
			}
		}
	}

	public State substitute(SBox sbox) {
		State result = new State();
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				result.bytes[i][j] = sbox.cypher(this.bytes[i][j]);
			}
		}
		return result;
	}

	public State shift() {
		State result = new State();
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				result.bytes[i][j] = this.bytes[i][(j + i) % 4];
			}
		}
		return result;
	}

	public State shiftInv() {
		State result = new State();
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				result.bytes[i][j] = this.bytes[i][(j - i + 4) % 4];
			}
		}
		return result;
	}

	public State mult(State mixMatrix) {
		State result = new State();
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				Block sum = new Block(8);
				for (int k = 0; k < 4; k++) {
					Block a = mixMatrix.bytes[row][k];
					Block b = this.bytes[k][col];
					Block multResult = a.modularMult(b);

					sum = sum.xOr(multResult);
				}
				result.bytes[row][col] = sum;

			}
		}
		return result;
	}

	public State xOr(Key key) {
		State result = new State();
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				result.bytes[i][j] = this.bytes[i][j].xOr(key.elmnt(j, i));
			}
		}
		return result;
	}

	public Block block() {
		Block[] blocks = new Block[16];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				blocks[4 * j + i] = this.bytes[i][j];
			}
		}
		return new Block(blocks);
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				s += this.bytes[i][j] + " ";
			}
			s += "\n";
		}
		return s;
	}
}

class SBox {
	private int[][] matrix;

	public SBox(int[][] matrix) {
		this.matrix = new int[matrix.length][];
		for (int i = 0; i < matrix.length; i++) {
			this.matrix[i] = new int[matrix[i].length];
			for (int j = 0; j < matrix[i].length; j++) {
				this.matrix[i][j] = matrix[i][j];
			}
		}
	}

	public Block cypher(Block toCypher) {
		int row = toCypher.rowValue();
		int col = toCypher.columnValue();
		int val = this.matrix[row][col];

		Block result = new Block(8);
		for (int i = 7; i >= 0; i--) {
			result.block[i] = ((val & 1) == 1);
			val >>= 1;
		}
		return result;
	}
}

abstract class Mode {
	protected AES cypherAlgo;

	public Mode(AES cypherAlgo) {
		this.cypherAlgo = cypherAlgo;
	}

	public abstract Block[] enCypher(Block[] toEncypher);

	public abstract Block[] deCypher(Block[] toDecypher);
}

class CBC extends Mode {
	private Block IV;

	public CBC(AES cypherAlgo, Block IV) {
		super(cypherAlgo);
		this.IV = IV;
	}

	public Block[] enCypher(Block[] toEncypher) {
		Block[] result = new Block[toEncypher.length];
		Block previous = this.IV;

		for (int i = 0; i < toEncypher.length; i++) {

			Block input = toEncypher[i].xOr(previous);

			result[i] = this.cypherAlgo.cypher(input);

			previous = result[i];
		}

		return result;
	}

	public Block[] deCypher(Block[] toDecypher) {
		Block[] result = new Block[toDecypher.length];
		Block previous = this.IV;

		for (int i = 0; i < toDecypher.length; i++) {
			Block decrypted = this.cypherAlgo.deCypher(toDecypher[i]);
			result[i] = decrypted.xOr(previous);
			previous = toDecypher[i];
		}

		return result;
	}
}

class OFB extends Mode {
	private Block Nonce;

	public OFB(AES cypherAlgo, Block Nonce) {
		super(cypherAlgo);
		this.Nonce = Nonce;
	}

	public Block[] enCypher(Block[] toEncypher) {
		Block[] result = new Block[toEncypher.length];
		Block output = this.Nonce;

		for (int i = 0; i < toEncypher.length; i++) {
			output = this.cypherAlgo.cypher(output);
			result[i] = toEncypher[i].xOr(output);
		}

		return result;
	}

	public Block[] deCypher(Block[] toDecypher) {
		// OFB decryption is identical to encryption
		return this.enCypher(toDecypher);
	}
}

class CTR extends Mode {
	private Block counter;

	public CTR(AES cypherAlgo, Block counter) {
		super(cypherAlgo);
		this.counter = counter;
	}

	public Block[] enCypher(Block[] toEncypher) {
		Block[] result = new Block[toEncypher.length];
		Block counterCopy = this.counter.clone();

		for (int i = 0; i < toEncypher.length; i++) {
			Block keystream = this.cypherAlgo.cypher(counterCopy);
			result[i] = toEncypher[i].xOr(keystream);
			counterCopy = counterCopy.next();
		}

		return result;
	}

	public Block[] deCypher(Block[] toDecypher) {
		// CTR decryption is identical to encryption
		return this.enCypher(toDecypher);
	}
}

public class AES {
	private Key[] keys;
	private SBox sbox, sboxInv;
	private State mixState, mixStateInv;
	private static int[][] s = {
			{ 0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5, 0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76 },
			{ 0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0, 0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0 },
			{ 0xB7, 0xFD, 0x93, 0x26, 0x36, 0x3F, 0xF7, 0xCC, 0x34, 0xA5, 0xE5, 0xF1, 0x71, 0xD8, 0x31, 0x15 },
			{ 0x04, 0xC7, 0x23, 0xC3, 0x18, 0x96, 0x05, 0x9A, 0x07, 0x12, 0x80, 0xE2, 0xEB, 0x27, 0xB2, 0x75 },
			{ 0x09, 0x83, 0x2C, 0x1A, 0x1B, 0x6E, 0x5A, 0xA0, 0x52, 0x3B, 0xD6, 0xB3, 0x29, 0xE3, 0x2F, 0x84 },
			{ 0x53, 0xD1, 0x00, 0xED, 0x20, 0xFC, 0xB1, 0x5B, 0x6A, 0xCB, 0xBE, 0x39, 0x4A, 0x4C, 0x58, 0xCF },
			{ 0xD0, 0xEF, 0xAA, 0xFB, 0x43, 0x4D, 0x33, 0x85, 0x45, 0xF9, 0x02, 0x7F, 0x50, 0x3C, 0x9F, 0xA8 },
			{ 0x51, 0xA3, 0x40, 0x8F, 0x92, 0x9D, 0x38, 0xF5, 0xBC, 0xB6, 0xDA, 0x21, 0x10, 0xFF, 0xF3, 0xD2 },
			{ 0xCD, 0x0C, 0x13, 0xEC, 0x5F, 0x97, 0x44, 0x17, 0xC4, 0xA7, 0x7E, 0x3D, 0x64, 0x5D, 0x19, 0x73 },
			{ 0x60, 0x81, 0x4F, 0xDC, 0x22, 0x2A, 0x90, 0x88, 0x46, 0xEE, 0xB8, 0x14, 0xDE, 0x5E, 0x0B, 0xDB },
			{ 0xE0, 0x32, 0x3A, 0x0A, 0x49, 0x06, 0x24, 0x5C, 0xC2, 0xD3, 0xAC, 0x62, 0x91, 0x95, 0xE4, 0x79 },
			{ 0xE7, 0xC8, 0x37, 0x6D, 0x8D, 0xD5, 0x4E, 0xA9, 0x6C, 0x56, 0xF4, 0xEA, 0x65, 0x7A, 0xAE, 0x08 },
			{ 0xBA, 0x78, 0x25, 0x2E, 0x1C, 0xA6, 0xB4, 0xC6, 0xE8, 0xDD, 0x74, 0x1F, 0x4B, 0xBD, 0x8B, 0x8A },
			{ 0x70, 0x3E, 0xB5, 0x66, 0x48, 0x03, 0xF6, 0x0E, 0x61, 0x35, 0x57, 0xB9, 0x86, 0xC1, 0x1D, 0x9E },
			{ 0xE1, 0xF8, 0x98, 0x11, 0x69, 0xD9, 0x8E, 0x94, 0x9B, 0x1E, 0x87, 0xE9, 0xCE, 0x55, 0x28, 0xDF },
			{ 0x8C, 0xA1, 0x89, 0x0D, 0xBF, 0xE6, 0x42, 0x68, 0x41, 0x99, 0x2D, 0x0F, 0xB0, 0x54, 0xBB, 0x16 } };
	private static int[][] sInv = {
			{ 0x52, 0x09, 0x6A, 0xD5, 0x30, 0x36, 0xA5, 0x38, 0xBF, 0x40, 0xA3, 0x9E, 0x81, 0xF3, 0xD7, 0xFB },
			{ 0x7C, 0xE3, 0x39, 0x82, 0x9B, 0x2F, 0xFF, 0x87, 0x34, 0x8E, 0x43, 0x44, 0xC4, 0xDE, 0xE9, 0xCB },
			{ 0x54, 0x7B, 0x94, 0x32, 0xA6, 0xC2, 0x23, 0x3D, 0xEE, 0x4C, 0x95, 0x0B, 0x42, 0xFA, 0xC3, 0x4E },
			{ 0x08, 0x2E, 0xA1, 0x66, 0x28, 0xD9, 0x24, 0xB2, 0x76, 0x5B, 0xA2, 0x49, 0x6D, 0x8B, 0xD1, 0x25 },
			{ 0x72, 0xF8, 0xF6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xD4, 0xA4, 0x5C, 0xCC, 0x5D, 0x65, 0xB6, 0x92 },
			{ 0x6C, 0x70, 0x48, 0x50, 0xFD, 0xED, 0xB9, 0xDA, 0x5E, 0x15, 0x46, 0x57, 0xA7, 0x8D, 0x9D, 0x84 },
			{ 0x90, 0xD8, 0xAB, 0x00, 0x8C, 0xBC, 0xD3, 0x0A, 0xF7, 0xE4, 0x58, 0x05, 0xB8, 0xB3, 0x45, 0x06 },
			{ 0xD0, 0x2C, 0x1E, 0x8F, 0xCA, 0x3F, 0x0F, 0x02, 0xC1, 0xAF, 0xBD, 0x03, 0x01, 0x13, 0x8A, 0x6B },
			{ 0x3A, 0x91, 0x11, 0x41, 0x4F, 0x67, 0xDC, 0xEA, 0x97, 0xF2, 0xCF, 0xCE, 0xF0, 0xB4, 0xE6, 0x73 },
			{ 0x96, 0xAC, 0x74, 0x22, 0xE7, 0xAD, 0x35, 0x85, 0xE2, 0xF9, 0x37, 0xE8, 0x1C, 0x75, 0xDF, 0x6E },
			{ 0x47, 0xF1, 0x1A, 0x71, 0x1D, 0x29, 0xC5, 0x89, 0x6F, 0xB7, 0x62, 0x0E, 0xAA, 0x18, 0xBE, 0x1B },
			{ 0xFC, 0x56, 0x3E, 0x4B, 0xC6, 0xD2, 0x79, 0x20, 0x9A, 0xDB, 0xC0, 0xFE, 0x78, 0xCD, 0x5A, 0xF4 },
			{ 0x1F, 0xDD, 0xA8, 0x33, 0x88, 0x07, 0xC7, 0x31, 0xB1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xEC, 0x5F },
			{ 0x60, 0x51, 0x7F, 0xA9, 0x19, 0xB5, 0x4A, 0x0D, 0x2D, 0xE5, 0x7A, 0x9F, 0x93, 0xC9, 0x9C, 0xEF },
			{ 0xA0, 0xE0, 0x3B, 0x4D, 0xAE, 0x2A, 0xF5, 0xB0, 0xC8, 0xEB, 0xBB, 0x3C, 0x83, 0x53, 0x99, 0x61 },
			{ 0x17, 0x2B, 0x04, 0x7E, 0xBA, 0x77, 0xD6, 0x26, 0xE1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0C, 0x7D } };
	private static int[][] mix = { { 2, 3, 1, 1 }, { 1, 2, 3, 1 }, { 1, 1, 2, 3 }, { 3, 1, 1, 2 } },
			mixInv = { { 14, 11, 13, 9 }, { 9, 14, 11, 13 }, { 13, 9, 14, 11 }, { 11, 13, 9, 14 } };

	public AES(Block key) {
		// Initialize S-boxes
		this.sbox = new SBox(s);
		this.sboxInv = new SBox(sInv);

		// Initialize mixing matrices
		this.mixState = new State(mix);
		this.mixStateInv = new State(mixInv);

		// Generate round keys
		Key masterKey = new Key(key);
		this.keys = masterKey.genSubKeys(this.sbox);
	}

	public Block cypher(Block plaintext) {
		State state = new State(plaintext);

		// Initial round key addition
		state = state.xOr(this.keys[0]);

		// Main rounds
		for (int i = 1; i < 10; i++) {
			state = state.substitute(this.sbox);
			state = state.shift();
			state = state.mult(this.mixState);
			state = state.xOr(this.keys[i]);
		}

		// Final round (no MixColumns)
		state = state.substitute(this.sbox);
		state = state.shift();
		state = state.xOr(this.keys[10]);

		return state.block();
	}

	public Block deCypher(Block cyphertext) {
		State state = new State(cyphertext);

		// Initial round key addition
		state = state.xOr(this.keys[10]);
		state = state.shiftInv();
		state = state.substitute(this.sboxInv);

		// Main rounds
		for (int i = 9; i > 0; i--) {
			state = state.xOr(this.keys[i]);
			state = state.mult(this.mixStateInv);
			state = state.shiftInv();
			state = state.substitute(this.sboxInv);
		}

		// Final round key addition
		state = state.xOr(this.keys[0]);

		return state.block();
	}

	public static void main(String[] args) {
		String plaintext = "00000001001000110100010101100111100010011010101111001101111011111111111011011100101110101001100001110110010101000011001000010000";
		String key = "00001111000101010111000111001001010001111101100111101000010110010000110010110111101011011101011010101111011111110110011110011000";
		String[] keys = {
				"00001111000101010111000111001001 01000111110110011110100001011001 00001100101101111010110111010110 10101111011111110110011110011000 ",
				"11011100100100000011011110110000 10011011010010011101111111101001 10010111111111100111001000111111 00111000100000010001010110100111 ",
				"11010010110010010110101110110111 01001001100000001011010001011110 11011110011111101100011001100001 11100110111111111101001111000110 ",
				"11000000101011111101111100111001 10001001001011110110101101100111 01010111010100011010110100000110 10110001101011100111111011000000 ",
				"00101100010111000110010111110001 10100101011100110000111010010110 11110010001000101010001110010000 01000011100011001101110101010000 ",
				"01011000100111010011011011101011 11111101111011100011100001111101 00001111110011001001101111101101 01001100010000000100011010111101 ",
				"01110001110001110100110011000010 10001100001010010111010010111111 10000011111001011110111101010010 11001111101001011010100111101111 ",
				"00110111000101001001001101001000 10111011001111011110011111110111 00111000110110000000100010100101 11110111011111011010000101001010 ",
				"01001000001001100100010100100000 11110011000110111010001011010111 11001011110000111010101001110010 00111100101111100000101100111000 ",
				"11111101000011010100001011001011 00001110000101101110000000011100 11000101110101010100101001101110 11111001011010110100000101010110 ",
				"10110100100011101111001101010010 10111010100110000001001101001110 01111111010011010101100100100000 10000110001001100001100001110110 " };
		Block plaintextBlock = new Block(plaintext), keyBlock = new Block(key);
		AES aes = new AES(keyBlock);
		for (int i = 0; i < aes.keys.length; i++) {
			if (keys[i].compareTo(aes.keys[i].toString()) == 0)
				System.out.println("Key " + i + " OK");
			else
				System.out.println("Key " + i + " KO");
		}
		Block cypherBlock = aes.cypher(plaintextBlock);
		System.out.println(cypherBlock);
		if (cypherBlock.toString().compareTo(
				"11111111000010111000010001001010000010000101001110111111011111000110100100110100101010110100001101100100000101001000111110111001") == 0)
			System.out.println("cypher OK");
		else
			System.out.println("cypher KO");
		Block deCypherBlock = aes.deCypher(cypherBlock);
		System.out.println(deCypherBlock);
		// System.out.println(deCypherBlock.toString().compareTo(plaintext));
		if (deCypherBlock.toString().compareTo(plaintext) == 0)
			System.out.println("decypher OK");
		else
			System.out.println("decypher KO");
		System.out.println("==========");
		Block deCypherBlock2 = aes.deCypher(cypherBlock);
		System.out.println(deCypherBlock2);
		System.out.println(deCypherBlock2.toString().compareTo(plaintext));
		if (deCypherBlock2.toString().compareTo(plaintext) == 0) {
			System.out.println("deCypherBlock 2 AES OK");
		} else {
			System.out.println("deCypherBlock 2 AES KO");
		}
		System.out.println("==========");
		String poeme = "Le temps a laissé son manteau\r\n"
				+ "De vent, de froidure et de pluie,\r\n"
				+ "Et s’est vêtu de broderie,\r\n"
				+ "De soleil luisant, clair et beau.\r\n"
				+ "\r\n"
				+ "Il n’y a bête ni oiseau\r\n"
				+ "Qu’en son jargon ne chante ou crie :\r\n"
				+ "Le temps a laissé son manteau\r\n"
				+ "De vent, de froidure et de pluie.\r\n"
				+ "\r\n"
				+ "Rivière, fontaine et ruisseau\r\n"
				+ "Portent en livrée jolie,\r\n"
				+ "Gouttes d’argent d’orfèvrerie ;\r\n"
				+ "Chacun s’habille de nouveau :\r\n"
				+ "Le temps a laissé son manteau.";
		Block[] blocks = Block.stringToBlock(poeme, 16);
		CBC cbc = new CBC(aes, cypherBlock);
		Block[] blocksCypher = cbc.enCypher(blocks);
		System.out.println(Block.blockToString(blocks));
		// System.out.println(Block.blockToString(cbc.deCypher(blocksCypher)));
		String[] cypherPoeme = {
				"11001010000111010101001110010110100110101111100001111000010100000010100110110101110010111011001000101101000001110010110101000101",
				"10001011110110011010101101100000100111111010100000000001001000100101010111100100001110000100001101011101011000111111000101010010",
				"01100110011111100011110101111001011011011111010100110100110110011011100010000000110000110000011001100011000011101111000111010100",
				"11101011011111110001010111010001100111010100110110000101110100000100110011000010010111111111010100111110111000000100011110000010",
				"00111001111101110001010010100001111001010011001110111100111101011101001100010100101110100010100001110011001101011010101010011110",
				"11100010111101010001100110100110101100100001001000101110010011100101010010000010011000010111101001111001000110100101100010010000",
				"11000111011100000010110011111110111001101000111011110000010000011010111110000111110111000001011100111010000011100100010001111111",
				"10010111001100010111110011001100110001001010101110000110100000011110100001111000100111010101000011011011111101011111111111100011",
				"11101110011000001010000001011101011001010100100101000010111100110010101110100111011101001100010110101101011101110111110000101000",
				"00100010010001010001011001001101111001100000101101010100001011001011110001011101101111001010100011100000010001100110010000100010",
				"01000100110001011111101110010101011101111111001000011111010111000110001011101101111101001010001110001011010101110010001011011101",
				"01010001100111000100001110111000100100100010011101111011011101101011000000001110100110111101001100111110011100000001010011100100",
				"01010000100001100000111110111001011111010000100011000010111010100011011101100000000110011110011001111101000100011010000100001001",
				"11101010001011011001001111110001101100010011010110011010100010011000100011101011000111100011111101100111100000100110000110110011",
				"10110011110000111111111010110000101111011111100100100011101000000001011111111111100110100111000011010001000101100110001010011000",
				"10011000010100000001111100010011011110011000111010011010101011000011101101101011101111110001100110111100101000100101011011010010",
				"10111110011101100000001100100010010110011011000100010111101111110101110110011100001000111100011001111100001000110110110010011001",
				"00101111110010110000001100100100101110010101001011010010000001010010111111011110100111100010001001101101010111111101111010000001",
				"11011011001101001010101111000001100001000010010001010011100010001111100011111100010101011010011111111111100001010111000000011010",
				"01101110100110101011111101101001000111101000101111111111101000010110110110110100000001010011010101111000000000000011101110110101",
				"11101000000101001110000110111001011100011110011100001101011111101100010000011001010110101101111000010011011110100010100001101101",
				"01100100101100011101110001101111010100000001011010110111001110001001000110110101101000101001111011101001101011110101001110000000",
				"10010101010010101100110111010100001111000111001001010100011110100011000001001010101011010000110000010110000010100011001010101010",
				"11110000110110111011100110000001000111010010100001110010010100000011100010100010001111010110100111000100000011100101011111001011",
				"10100110110011111101110000101110010111001001110111000011110111001011101010101001101111100111111000101000011100110100000011101011" };
		for (int i = 0; i < cypherPoeme.length; i++) {
			if (cypherPoeme[i].compareTo(blocksCypher[i].toString()) != 0) {
				System.out.println("Bloc CBC " + i + " KO");
			} else {
				System.out.println("Bloc CBC " + i + " OK");
			}
		}
		System.out.println("==========");
		String poeme2 = "Mignonne, allons voir si la rose\r\n"
				+ "Qui ce matin avait déclôsé\r\n"
				+ "Sa robe de pourpre au Soleil,\r\n"
				+ "A point perdu cette vesprée\r\n"
				+ "Les plis de sa robe pourprée,\r\n"
				+ "Et son teint au vôtre pareil.\r\n"
				+ "\r\n"
				+ "Las ! voyez comme en peu d'espace,\r\n"
				+ "Mignonne, elle a dessus la place,\r\n"
				+ "Las ! las ! ses beautés laissé choir !\r\n"
				+ "Ô vraiment marâtre Nature,\r\n"
				+ "Puisqu'une telle fleur ne dure\r\n"
				+ "Que du matin jusques au soir !\r\n"
				+ "\r\n"
				+ "Donc, si vous me croyez, mignonne,\r\n"
				+ "Tandis que votre âge fleuronne\r\n"
				+ "En sa plus verte nouveauté,\r\n"
				+ "Cueillez, cueillez votre jeunesse :\r\n"
				+ "Comme à cette fleur, la vieillesse\r\n"
				+ "Fera ternir votre beauté.";
		Block[] blocks2 = Block.stringToBlock(poeme2, 16);
		OFB ofb = new OFB(aes, cypherBlock);
		blocksCypher = ofb.enCypher(blocks2);
		System.out.println(Block.blockToString(ofb.deCypher(blocksCypher)));
		String[] cypherPoeme2 = {
				"10111011001011010011011000100100000001011000101010111100111000110000101100101000011010011111110001100101001001111110000000101111",
				"10111101001101101011101111110110010010001010010100000110000000001111110010101011011100101000111001111101111110110100111001010011",
				"00001010010101001000100111100110001101000111011000000001011110010000100101001010001111011000111010000100110001101100100110000001",
				"01101110101100100001001011110100111010000000011100101001101100110010101011001000100100101011001101000001010111011001011011010010",
				"11101110100101101101111010100100010001101111110011000100111101100110100111110010010101100011010100100010100000101011110001101000",
				"10110001011001000010001100000110101111000000011000101101001010011100011110101110110001010111011001011111000111000100101011111011",
				"11000011011010000010100100110110100110001001111000110000110100011110111101101000000111001001000110110101000001100010001101110010",
				"10001001010100001010011001100011110110001111100100010011000100011110010010101011101111110011110100100011101000110010011110000111",
				"01001000010000110100010001010000011100010100110111101011100000001101010111111100010001001010111000111111000110010010001100001111",
				"00011110111001110010000000000111100101000110111000001010101011011001101000011001100010001100010001011000000011001001000011110110",
				"00001011011110011010111010101000110111010011011101011110111101100011011101101000011100111011111001101001110111001100110111101110",
				"01100010011000111000111011101111110011001001000000011111011000110001100010010110110111011111100101110101001001111010000011000110",
				"01001101111000011110100111100001001101011000000100101010010101110010011001101001001000000100011101011010011011001001111101011001",
				"11001110000100011100010000010001001100010100110001111000100001000000011000000100100010001001100000111011100011100110011110110011",
				"11100000000010101011000111010110001011111110001010111111101100000101010001100010100000010000100111010110110110100100011011000010",
				"00011011001000100101001100000010000000010010011010101001110110010000010010010010001110110100001111000001111011100111011000111010",
				"01010011011000101011101101000111000100110110010000010100111001100001011110111001111000011100010110111110000000111100100001010011",
				"11101001011000001111010000110100110110010111011100000100000111100101100001000110111001100111010000011011010001111010100011010001",
				"11000001111100001101110101101101011101000111110010010101010101111111000010110010111011000101001110001100001001110100101101110010",
				"01011001010101000111111011100110011010111001100011001110001110011010101100100011000010011110000100001010110101101111011101011110",
				"11100011001000001011100011010111010010010111010100001100110101111111100001110001001010101110101001000101100111110110101111000111",
				"01101010001100000011101001100001100010101100000001000101010101100110101000000100000111101100001100001101011110001110110101000000",
				"01001010101100111000111110010010010011011110001000100101101100011111010101111010111010001001011010111111101010111111011111111011",
				"01011101100110000010101011000110110000101001001110101110010110000001001000001100000010010101100010110010011111100100011101010101",
				"00110100000010010111101111101010010011110000111111000110110001100011001111011001011100010101100111011110100101100011011100101101",
				"01101000001001111010101111110010001010010001101110001000001100111011111000001100100000001001010101100000100111100110010011011110",
				"10000101111011110000010001111010001000001110011111111010100000011111100001010011111101000111101100000101000000101111000101100111",
				"10111010010010100111111001001100011011101110111111001010101010011101000000110011111101100101111011110011010110011011100000100001",
				"01000100110100011110011001101010101100111111100010101011000001000110000101000000001111111010010001000110110001010100101000100101",
				"00010101100000100001010011101000011101001010111100000001011101100011111001001011010000101101010101001100111110010101110001101010",
				"11111110000010111100100000001110101001111101000110111111100011110101100100011001100011111000011111001000101011110101101000010001",
				"10001100000101000000101011100010001001100000110101100110101100110111011100001111111000111110100000110010000011000000001010010001",
				"11001011100110110001010111011110110111011011000011100000010101100101010011001010111101110110111011010111010000000010010011010111",
				"01011011011110101011010001111010110011000101100100100011110100001001011000010001001110100000011100001000110110011010010000011011",
				"11101111100110101001111000111000101000111111101111100010000001101011100000110110101110010010011011011101111100111101011111000100",
				"11010011000011111010100000100111000000010010110000011001100111000001010010010110100101101010000000010011100000110100001100111010" };
		for (int i = 0; i < cypherPoeme2.length; i++) {
			// System.out.println("\"" + blocksCypher[i].toString() + "\",");
			if (cypherPoeme2[i].compareTo(blocksCypher[i].toString()) != 0) {
				System.out.println("Bloc OFB " + i + " KO");
			} else {
				System.out.println("Bloc OFB " + i + " OK");
			}
		}
		System.out.println("==========");
		String poeme3 = "Demain, dès l'aube, à l'heure où blanchit la campagne,\r\n"
				+ "Je partirai. Vois-tu, je sais que tu m'attends.\r\n"
				+ "J'irai par la forêt, j'irai par la montagne.\r\n"
				+ "Je ne puis demeurer loin de toi plus longtemps.\r\n"
				+ "\r\n"
				+ "Je marcherai les yeux fixés sur mes pensées,\r\n"
				+ "Sans rien voir au dehors, sans entendre aucun bruit,\r\n"
				+ "Seul, inconnu, le dos courbé, les mains croisées,\r\n"
				+ "Triste, et le jour pour moi sera comme la nuit.\r\n"
				+ "\r\n"
				+ "Je ne regarderai ni l'or du soir qui tombe,\r\n"
				+ "Ni les voiles au loin descendant vers Harfleur,\r\n"
				+ "Et quand j'arriverai, je mettrai sur ta tombe\r\n"
				+ "Un bouquet de houx vert et de bruyère en fleur.";
		Block[] blocks3 = Block.stringToBlock(poeme3, 16);
		CTR ctr = new CTR(aes, cypherBlock);
		blocksCypher = ctr.enCypher(blocks3);
		System.out.println(Block.blockToString(ctr.deCypher(blocksCypher)));
		String[] cypherPoeme3 = {
				"10110010001000010011110000101011000000111000101011111110101001100100001111100000011110111011000001100101011011111110111100101001",
				"10000011100000001111011011010000110000000000101111111110001101100110111000100111000000010110001100011110110101101011110010101001",
				"10100101000010100111111111111110011000100001110101011001111000001100011111010110000110000010110100100010001000010111110101100101",
				"00101101111101100011010110000110001111011101011100001000110011110011000000110110101101111011101111010101001001010111110010010101",
				"11110001000000110101011111000111011011010110101011100010000001100011010001101100001000000010110110011100001010001100111010001101",
				"10011110010001100101000101001111010000111001101110000110100011100000010101001101011101001001101111001110010001100101011100001001",
				"10010001001110111101000101011110000100101001110011000110110011110110001101000001011010110000011100111011010100010110101010100100",
				"00010001000010010110011001011101001010011010101110101001101100011110011101100011000101101001110000110111101001010110010100100110",
				"01001010110100000101001110001100000111011000111100011100110100010010010001001010101110110101011101011000100110111001110011010000",
				"10000011101110000110111011011111001100010011110000111111110110010111010000001001000001101000000010011101100100100010001001101001",
				"10001000000000001101110101010111000001000000011001011111011001011111101100010101110001111000011011110101011101000101100000000000",
				"10110111101111000011001110001011100001110010011011000101011000101100011111010111110100111100001101110010110111011011000100111101",
				"00011011101010011001010110100110001001010010100101000010001000000110100000110001111010100111001011110000110010000111101111011001",
				"11010000111010000100100001100100000001110011100011000100111110011011011100111111010110111100000011111101110000010100101111001010",
				"11000000011011110010001010000001100111010111101110011111101011110110111010100100000000100011100010111011110100101001001011001100",
				"10010110011101010011111100000111100101111101011010011101101011000101110010111111001000100001101101000010100101110001001101011011",
				"01110011101010011011001001001111111000101111100000111111100110101011110010110100101001110101100001010011100011101011001001010111",
				"01111110000011110000011100100111100110000100101001110100010110011100011111100010101101001100000000010011111110100010010000000001",
				"11001111101110111101101110000110100000111110111000100100010101010001000111101110011000100110101001011011101101111001101001001110",
				"00010000101001000011000011001110000010110010001100110001110010110000111100000100000010010001110000000101111010010001000001011110",
				"11010100110001001010000101111101001101111110110000011111111110001100011010100011111100101001000101000011011111110010001011001010",
				"10011010001001110110110110011001110101010001011101010011010010010000000111000111010100000010000101011010110101111011011011110001",
				"10110010001100001010110001010000010001000000110011000101110001111110110001110110010111111010110011101000011011000011110101001111",
				"00011001000010111011010100010011011011000001100111100010110110010011011000001110010000011100000001111001110011010101000010001010",
				"11000101101010111100110000001000101110111100010011110001001100010101101100101000001101110001110110011111011100010011111111001110",
				"11001000100011011010111011000011110111111111111000100100001011001111101001010001100001110100010110100010101001000110001010100100",
				"01011101100111110011000011100001010001011100101101000000111110011001110000100010110100011010110100100000101011010110011100010110",
				"00101100001011111100110101010000000100010101001101111110010011111101101010001000010001011011011110001101100110000100110111101000",
				"11000011111010001110101110111111011000001010111001000111110111101010010010000110100100111101000100001001001010001111111000101001",
				"01110000101101100110011010101011100010111011100101000000110111111010100000100001010101111101111001101000001001101111000100111110",
				"00000111111001000001100001101011010010001000110110011001000011000001111010010100001111111011010010111010000001001110011110101100",
				"00010000101111100111110111111000101101110100010101000100101101010010001010101001110111000110110001110100100101001000001100000011",
				"10011100100010101110000000101001110111010011110010101000101110010100001011000111001111011110110101010011001111100000011011101000",
				"10110001000101110000001101011110010111101001110110100101001010000101101110000011010110100110101000100001001111000000111110000100",
				"01110001001100001000010011001110011011010100111110100011110101111100011010110100011011000001010001010110010110000100011011001100",
				"11011011001010101000101100010110100011001010110011101110101100111011101001100011010101110001101001100100010101101001011101001100",
				"11000011110010010010100001010010100010010101111010110110011011001110101111111011110101000111000110111110100010111111111100010100" };
		for (int i = 0; i < cypherPoeme3.length; i++) {
			// System.out.println("\"" + blocksCypher[i].toString() + "\",");
			if (cypherPoeme3[i].compareTo(blocksCypher[i].toString()) != 0) {
				System.out.println("Bloc CTR " + i + " KO");
			} else {
				System.out.println("Bloc CTR " + i + " OK");
			}
		}
	}
}
