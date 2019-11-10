Gauss Key Card
==============

*Gauss Key Card* is a [Java Card][] applet that implements the minimal
working subset of the [Tesla Key Card Protocol][]. Supported Java Card
implementations that load this application will be able to be paired
with a compatible vehicle and subsequently unlock, start, or lock the
vehicle in the same way you would with an official key card.

If at this point you can't imagine why you might ever want to use this
applet, then it is not for you.

[Java Card]: https://en.wikipedia.org/wiki/Java_Card
[Tesla Key Card Protocol]: https://gist.github.com/darconeous/2cd2de11148e3a75685940158bddf933

## Goals and Non-Goals ##

The goal of this applet is to add the ability to pair, unlock, and
start start a supported vehicle to any Java Card meeting the minimum
requirements. This applet makes no attempt to fool the vehicle into
thinking the card is an authentic Tesla Key Card.

## Security ##

To increase the difficulty of cloning, the private value of the ECDH
key pair is generated on-card when the applet is installed and never
leaves the secure element. No mechanism for externally supplying a
private key with a known value is provided by this implementation.

Avoid buying Tesla Key Cards from anyone other than Tesla---the card
may be have weak or known private keys. Authentic Tesla Key Cards
should be safe to buy or use from anyone, but until the vehicle
actually verifies attestation certificates there is no way to be sure.
Until then you should always assume that whoever sold you a key card
might also have the ability to unlock and start any vehicle you
subsequently pair with the card.

## Smartcard Requirements ##

 * Java Card 2.2.2 (or later)
 * Contactless ISO-14443a interface (NFC)
 * 7-byte NFC UID
 * [`KeyAgreement.ALG_EC_SVDP_DH`][]
 * [`Cipher.ALG_AES_BLOCK_128_ECB_NOPAD`][]
 * [Known GlobalPlatform keys][], so you can actually load the applet

[`KeyAgreement.ALG_EC_SVDP_DH`]: https://docs.oracle.com/javacard/3.0.5/api/javacard/security/KeyAgreement.html#ALG_EC_SVDP_DH
[`Cipher.ALG_AES_BLOCK_128_ECB_NOPAD`]: https://docs.oracle.com/javacard/3.0.5/api/javacardx/crypto/Cipher.html#ALG_AES_BLOCK_128_ECB_NOPAD
[Known GlobalPlatform keys]: https://github.com/martinpaljak/GlobalPlatformPro/wiki/Keys

The Java Card **MUST** check the ECDH public key to ensure it is a
valid point on the curve before continuing to calculate the shared
secret. If it does not do this then it may be feasible to clone the
card.

## Caveats ##

There are some serious caveats that need to be well understood before
using this applet:

### Java Cards with 4-byte UIDs won't work ###

The authenticator on supported vehicles will currently refuse to pair
with any card that doesn't have a 7-byte UID. This unfortunately means
that most commonly-available dual-interface Javacards will not be
recognized by vehicles even when this applet is loaded.

### Software updates could make this applet useless ###

Official Tesla Key Cards have an attestation certificate, which means
that Tesla could easily block non-Tesla key cards from pairing with
vehicles at any point in the future via a software update. Keys
already paired with a vehicle *might* continue to work in such a
scenario, but there are no guarantees.

Since there is no publicly available comprehensive documentation for
the Tesla Key Card Protocol, what is known was determined through
observing the interactions between the vehicle and an official Tesla
Key Card. Gauss Key Card supports all commands that are currently
being used by the vehicle during pairing and authentication, but there
are several additional commands that are not supported because there
was not enough context to infer what their purpose might be. If Tesla
at some point enables additional functionality or features which
require those commands, existing Gauss Key Cards would be unable to
participate.

## Building ##

1. Install `ant`.
   * macOS/homebrew: `brew install ant`
2. `git submodules init`
3. `git submodules update --recursive`
4. `JC_HOME=ext/ant/sdks/jc222_kit ant`

## Installing/Uninstalling ##

Using [GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro):

```
$ gp -install bin/GaussKeyCard.cap
```

```
$ gp -uninstall bin/GaussKeyCard.cap
```

