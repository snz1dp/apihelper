package gateway.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * RSA工具类
 * @author neeker
 *
 */
public abstract class RSAUtils {

	public static final String RSA_DEFAULT_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	
	private static final String PEM_PUBLIC_KEY = "PUBLIC KEY";
	
	private static final String PEM_PRIVATE_KEY = "RSA PRIVATE KEY";
	
	private static final String RSA_ALGORITHM = "RSA";

	//添加BouncyCastle的RSA扩展提供器
	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/**
	 * 产生密钥对
	 * 
	 * @param keySize
	 *          密钥长度
	 * @return {@link KeyPair}
	 */
	public static KeyPair generateKeyPair(int keySize) {

		KeyPairGenerator keypair_gen = null;

		try {
			keypair_gen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		keypair_gen.initialize(keySize * 8);
		return keypair_gen.generateKeyPair();
	}

	/**
	 * 从PEM格式中获得RSA公钥
	 * 
	 * @param pem
	 *          传入的PEM格式
	 * @return {@link PublicKey}
	 */
	public static PublicKey parsePublicKeyFromPEM(String pem) {
		PemReader pem_reader = new PemReader(new StringReader(pem));
		try {
			KeyFactory keyfactory = null;
			try {
				keyfactory = KeyFactory.getInstance(RSA_ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			} 
			X509EncodedKeySpec keyspec = null;
			try {
				keyspec = new X509EncodedKeySpec(pem_reader.readPemObject().getContent());
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			try {
				return keyfactory.generatePublic(keyspec);
			} catch (InvalidKeySpecException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} finally {
			IOUtils.closeQuietly(pem_reader);
		}
	}
	
	public static PublicKey parsePublicKey(String key) {
		KeyFactory keyfactory = null;
		try {
			keyfactory = KeyFactory.getInstance(RSA_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} 
		X509EncodedKeySpec keyspec = null;
		try {
			keyspec = new X509EncodedKeySpec(Base64.decodeBase64(key));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		try {
			return keyfactory.generatePublic(keyspec);
		} catch (InvalidKeySpecException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public static PrivateKey parsePrivateKey(String key) {
		KeyFactory keyfactory = null;
		try {
			keyfactory = KeyFactory.getInstance(RSA_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} 
		PKCS8EncodedKeySpec keyspec = null;
		try {
			keyspec = new PKCS8EncodedKeySpec(Base64.decodeBase64(key));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		try {
			return keyfactory.generatePrivate(keyspec);
		} catch (InvalidKeySpecException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * @brief 从PEM格式中获得RSA私钥
	 * @param pem
	 *          PEM格式
	 * @return {@link PrivateKey}
	 */
	public static PrivateKey parsePrivateKeyFromPEM(String pem) {
		pem = StringUtils.replace(pem, "\\n", "\n");
		PemReader pem_reader = new PemReader(new StringReader(pem));
		try {
			KeyFactory keyfactory = null;
			try {
				keyfactory = KeyFactory.getInstance(RSA_ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			PKCS8EncodedKeySpec keyspec = null;
			try {
				keyspec = new PKCS8EncodedKeySpec(pem_reader.readPemObject().getContent());
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			try {
				return keyfactory.generatePrivate(keyspec);
			} catch (InvalidKeySpecException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} finally {
			IOUtils.closeQuietly(pem_reader);
		}
	}

	/**
	 * @brief 校验数字签名
	 * @param digest
	 *          原始数据
	 * @param sign
	 *          签名数据
	 * @param pubkey
	 *          公钥
	 * @param algorithm
	 *          签名算法
	 * @return 返回true表示签名正确
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public static boolean verifySign(byte digest[], byte sign[], PublicKey pubkey, String algorithm)
			throws InvalidKeyException, SignatureException {
		Signature signature = null;
		try {
			signature = Signature.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		signature.initVerify(pubkey);
		signature.update(digest);
		return signature.verify(sign);
	}

	/**
	 * @brief 数字签名
	 * @param digest
	 *          待签名数据
	 * @param privkey
	 *          RSA私钥
	 * @param algorithm
	 *          签名数据
	 * @return 已签名数据
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public static byte[] signData(byte digest[], PrivateKey privkey, String algorithm) throws InvalidKeyException,
			SignatureException {
		return signData(digest, 0, digest.length, privkey, algorithm);
	}

	public static byte[] signData(byte digest[], int offset, int len, PrivateKey privkey, String algorithm)
			throws InvalidKeyException, SignatureException {
		Signature signature = null;
		try {
			signature = Signature.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		signature.initSign(privkey);
		signature.update(digest, offset, len);
		return signature.sign();
	}

	/**
	 * @brief 解密数据
	 * 
	 * @param cipher_data
	 *          密文
	 * @param key
	 *          密钥
	 * @return 明文
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] decryptData(byte cipher_data[], Key key, String transformation) throws InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		return decryptData(cipher_data, 0, cipher_data.length, key, transformation);
	}

	/**
	 * @brief 解密数据
	 * @param cipher_data
	 *          密文块
	 * @param data_offset
	 *          开始索引
	 * @param data_len
	 *          数据长度
	 * @param key
	 *          密钥
	 * @return 明文
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] decryptData(byte cipher_data[], int data_offset, int data_len, Key key, String transformation)
			throws InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = null;

		try {
			cipher = Cipher.getInstance(transformation);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		cipher.init(Cipher.ENCRYPT_MODE, key);
		int keysiz = cipher.doFinal(new byte[1]).length;

		ByteBuffer byte_buffer = ByteBuffer.wrap(new byte[(data_len / keysiz) * keysiz]);
		for (int i = 0; i < (data_len / keysiz); i++) {
			try {
				cipher = Cipher.getInstance(transformation);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte_buffer.put(cipher.doFinal(cipher_data, data_offset + (i * keysiz), keysiz));
		}
		return Arrays.copyOfRange(byte_buffer.array(), 0, byte_buffer.position());
	}

	/**
	 * @brief 加密数据
	 * @param plaint_data
	 *          明文
	 * @param key
	 *          密钥
	 * @return 密文
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] encryptData(byte plaint_data[], Key key, String transformation) throws InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		return encryptData(plaint_data, 0, plaint_data.length, key, transformation);
	}

	/**
	 * @brief 加密数据
	 * @param plaint_data
	 *          明文块
	 * @param data_offset
	 *          开始索引
	 * @param data_len
	 *          数据长度
	 * @param key
	 *          密钥
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] encryptData(byte plaint_data[], int data_offset, int data_len, Key key, String transformation)
			throws InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(transformation);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		cipher.init(Cipher.ENCRYPT_MODE, key);
		int keysiz = cipher.doFinal(new byte[1]).length;

		int pkcs1_keysiz = keysiz - 11;
		ByteBuffer byte_buffer = ByteBuffer.wrap(new byte[((data_len / pkcs1_keysiz) + ((data_len % pkcs1_keysiz) == 0 ? 0
				: 1)) * keysiz]);
		for (int i = 0; i < (data_len / pkcs1_keysiz); i++) {
			try {
				cipher = Cipher.getInstance(transformation);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte_buffer.put(cipher.doFinal(plaint_data, data_offset + (i * pkcs1_keysiz), pkcs1_keysiz));
		}

		if ((data_len % pkcs1_keysiz) > 0) {
			try {
				cipher = Cipher.getInstance(transformation);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte_buffer.put(cipher.doFinal(plaint_data, data_offset + ((data_len / pkcs1_keysiz) * pkcs1_keysiz), data_len
					% pkcs1_keysiz));
		}

		return Arrays.copyOfRange(byte_buffer.array(), 0, byte_buffer.position());
	}

	public static String toPem(Key key) {
		ByteArrayOutputStream bstm = new ByteArrayOutputStream();
		PemWriter pem_writer = new PemWriter(new OutputStreamWriter(bstm));
		try {
			if (key instanceof PrivateKey) {
				try {
					pem_writer.writeObject(new PemObject(PEM_PRIVATE_KEY, key.getEncoded()));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			} else if (key instanceof PublicKey) {
				try {
					pem_writer.writeObject(new PemObject(PEM_PUBLIC_KEY, key.getEncoded()));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			} else {
				throw new IllegalArgumentException("参数错误!");
			}
	
			try {
				pem_writer.flush();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			String result_pem = bstm.toString();
			return result_pem;
		} finally {
			IOUtils.closeQuietly(pem_writer);
		}
	}


}
