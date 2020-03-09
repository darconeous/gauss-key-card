package io.github.darconeous.gausskeycard;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyAgreement;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

public class GaussKeyCard extends Applet
{
	/* Card commands we support. */
	private static final byte INS_GET_PUBLIC_KEY = (byte)0x04;
	private static final byte INS_AUTHENTICATE = (byte)0x11;
	private static final byte INS_GET_CARD_INFO = (byte)0x14;

	private static final short OFFSET_CHALLENGE = (short)(ISO7816.OFFSET_CDATA + 65);

	// Constants from JavaCard 3.x. This way we can still install on
	// JC 2.2.2 cards and fall back to the traditional behavior.
	private static final byte TYPE_AES_TRANSIENT_DESELECT = 14;
	private static final byte TYPE_AES_TRANSIENT_RESET = 13;

	private final KeyPair key1;
	private final KeyAgreement ecdh;
	private final Cipher aes_ecb;
	private final AESKey aes_key;
	private final RandomData rng;

	public static void
	install(byte[] info, short off, byte len)
	{
		final GaussKeyCard applet = new GaussKeyCard();
		applet.register();
	}

	protected
	GaussKeyCard()
	{
		key1 = ECP256.newKeyPair(false);

		key1.genKeyPair();

		ecdh = KeyAgreement.getInstance(KeyAgreement.ALG_EC_SVDP_DH, false);

		ecdh.init(key1.getPrivate());

		aes_ecb = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);

		AESKey key = null;

		try {
			// Put the AES key in RAM if we can.
			key = (AESKey)KeyBuilder.buildKey(TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_128, false);
		} catch (CryptoException e) {
			try {
				// This will use a bit more RAM, but
				// at least it isn't using flash.
				key = (AESKey)KeyBuilder.buildKey(TYPE_AES_TRANSIENT_RESET, KeyBuilder.LENGTH_AES_128, false);
			} catch (CryptoException x) {
				// Uggh. This will wear out the flash
				// eventually, but we don't have a better option.
				key = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
			}
		}

		aes_key = key;

		// We shouldn't require high-strength random numbers
		// for calculating the challenge salt.
		rng = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
	}

	public void
	process(APDU apdu)
	{
		final byte[] buffer = apdu.getBuffer();

		if (selectingApplet()) {
			return;
		}

		// We only support the proprietary class.
		if ((buffer[ISO7816.OFFSET_CLA] & (byte)0x80) != (byte)0x80) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
			return;
		}

		switch (buffer[ISO7816.OFFSET_INS]) {
		case INS_GET_PUBLIC_KEY:
			processGetPublicKey(apdu);
			break;

		case INS_AUTHENTICATE:
			processAuthenticate(apdu);
			break;

		case INS_GET_CARD_INFO:
			processGetCardInfo(apdu);
			break;

		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void
	processGetCardInfo(APDU apdu)
	{
		final byte[] buffer = apdu.getBuffer();
		final short le = apdu.setOutgoing();

		short len = 0;
		buffer[len++] = 0x00;
		buffer[len++] = 0x01;

		len = le > 0 ? (le > len ? len : le) : len;
		apdu.setOutgoingLength(len);
		apdu.sendBytes((short)0, len);
	}

	private void
	processGetPublicKey(APDU apdu)
	{
		final byte[] buffer = apdu.getBuffer();

		final short le = apdu.setOutgoing();

		final ECPublicKey epubk = (ECPublicKey)key1.getPublic();

		short len = epubk.getW(buffer, (short)0);

		len = le > 0 ? (le > len ? len : le) : len;
		apdu.setOutgoingLength(len);
		apdu.sendBytes((short)0, len);
	}

	private void
	processAuthenticate(APDU apdu)
	{
		final byte[] buffer = apdu.getBuffer();
		final short incomingLength = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);

		if (incomingLength < (short)0x51) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		ecdh.generateSecret(buffer, ISO7816.OFFSET_CDATA, (short)65, buffer, (short)16);

		aes_key.setKey(buffer, (short)16);
		aes_ecb.init(aes_key, Cipher.MODE_ENCRYPT);

		// Generate the random salt.
		rng.generateData(buffer, OFFSET_CHALLENGE, (short)4);

		short len = aes_ecb.doFinal(buffer, OFFSET_CHALLENGE, (short)16, buffer, (short)0);
		final short le = apdu.setOutgoing();

		len = le > 0 ? (le > len ? len : le) : len;
		apdu.setOutgoingLength(len);
		apdu.sendBytes((short)0, len);
	}
}
