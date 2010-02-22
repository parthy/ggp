package player;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash {
	private String hash;

	public MD5Hash(String text) {
		MessageDigest md = null;
		byte[] encryptMsg = null;
		try {
			md = MessageDigest.getInstance("MD5");

			encryptMsg = md.digest(text.getBytes());

		} catch (NoSuchAlgorithmException e) {
			System.out.println("No Such Algorithm Exception!");
		}
		String swap = "";

		String byteStr = "";

		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i <= encryptMsg.length - 1; i++) {
			byteStr = Integer.toHexString(encryptMsg[i]);

			switch (byteStr.length()) {
			case 1:
				swap = "0" + Integer.toHexString(encryptMsg[i]);
				break;
			case 2:

				swap = Integer.toHexString(encryptMsg[i]);
				break;
			case 8:

				swap = (Integer.toHexString(encryptMsg[i])).substring(6, 8);
				break;
			}
			strBuf.append(swap);

		}
		this.hash = strBuf.toString();
	}

	public String toString() {
		return this.hash;
	}
}
