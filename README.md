Gauss Key Card
==============

*Gauss Key Card* is a [Java Card][] applet that implements the minimal
working subset of the [Tesla Key Card Protocol][]. Supported Java Card
implementations that load this application will be able to be paired
with a compatible vehicle and subsequently unlock, start, or lock the
vehicle in the same way you would with an official key card. ([video][])

If at this point you can't imagine why you might ever want to use this
applet, then it is not for you.

[Java Card]: https://en.wikipedia.org/wiki/Java_Card
[Tesla Key Card Protocol]: https://gist.github.com/darconeous/2cd2de11148e3a75685940158bddf933
[video]: https://www.youtube.com/watch?v=QBP_Hjlpwjs

## Caveats ##

THIS APPLET IS INTENDED FOR EXPERIMENTAL USE ONLY. IT HAS NOT BEEN
REVIEWED, CHECKED, APPROVED, OR ENDORSED BY TESLA. IT MAY BE BLOCKED
BY TESLA AT ANY TIME.

BY USING THIS APPLET (OR INSTALLING IT TO A CARD FOR SOMEONE ELSE
TO USE) YOU ASSUME ALL RESPONSIBILITY, RISK, AND LIABILITY FOR THE
USE OR MISUSE OF THIS SOFTWARE. SEE [LICENSE AGREEMENT](./LICENSE)
FOR MORE INFORMATION.

There are some serious caveats that need to be well understood before
using this applet:

### Type-A vs. Type-B ###

IEEE 14443 defines two types of contactless devices: Type-A and Type-B.
There are javacards available in both varieties, and this applet will
blissfully allow itself to be installed onto either. However, *Tesla
vehicles currently ignore type-B cards*, so make sure that your card is
indeed a 14443 type-A card.

### Maximum Frame Size ###

The maximum frame size (FSC) for the card must be 96 bytes or greater.
This corresponds to a FSCI value of 6 or greater. **Any card with an FSCI
of 5 or less will not work**.

Tesla vehicles will ignore the FSCI field of the ATS, which means that it
will not attempt to break up larger frames if the indicated FSCI is small (<6).
Specifically, **the card MUST be able to properly handle receiving the
authenticate APDU (86 bytes) in a single frame**. If a card advertises
an FSCI smaller than 6 then it is unlikely to be able to satisfy this
requirement.

For example, smart cards with DESFire EV1 emulation support have an
FSCI of 5, and will unfortunately choke if they receive a frame larger
than 64 bytes. Such cards are not able to be used as Tesla Key Cards.

> NOTE: In earlier versions of this document, this behavior was confused with
> the NFC UID length. It just happened to be the case that most of the
> 4-byte UID cards the author tested also had an FCI of 5. There is no
> limitation of the length of the UID on the card imposed by the vehicle.

A list of tested cards can be found [here](https://github.com/darconeous/gauss-key-card/wiki/Recommended-Cards).

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

### Updating GaussKeyCard Applet Will Break Pairing

If you decide to upgrade the version of GaussKeyCard on your card
to a new version, the pairing will be broken because a new internal
private key will be generated for the card.

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
 * **FSCI must be 6 or larger.** (Must support frame sizes of at least 96 bytes)
 * Must support [`KeyAgreement.ALG_EC_SVDP_DH`][]
 * Must support [`Cipher.ALG_AES_BLOCK_128_ECB_NOPAD`][]
 * [Known card management keys][], so you can actually load the applet

[`KeyAgreement.ALG_EC_SVDP_DH`]: https://docs.oracle.com/javacard/3.0.5/api/javacard/security/KeyAgreement.html#ALG_EC_SVDP_DH
[`Cipher.ALG_AES_BLOCK_128_ECB_NOPAD`]: https://docs.oracle.com/javacard/3.0.5/api/javacardx/crypto/Cipher.html#ALG_AES_BLOCK_128_ECB_NOPAD
[Known card management keys]: https://github.com/martinpaljak/GlobalPlatformPro/wiki/Keys

A list of tested cards can be found [here](https://github.com/darconeous/gauss-key-card/wiki/Recommended-Cards).

## Installing/Uninstalling ##

You can download a pre-built CAP file on the [project release page][];

[project release page]: https://github.com/darconeous/gauss-key-card/releases

To install the applet to a supported Java Card *with default card
management keys*, use [GlobalPlatformPro](https://github.com/martinpaljak/GlobalPlatformPro):

```
$ gp -install bin/GaussKeyCard.cap
```

Uninstalling is similar:

```
$ gp -uninstall bin/GaussKeyCard.cap
```

## Building ##

1. Install `ant`.
   * macOS/homebrew: `brew install ant`
2. `git submodules init`
3. `git submodules update --recursive`
4. `JC_HOME=ext/ant/sdks/jc222_kit ant`
