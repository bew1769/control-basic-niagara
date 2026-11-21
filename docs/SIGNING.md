# Signing the module

Niagara 4 will not load a module jar unless the jar is signed and the station or Workbench trusts the signing certificate. On current 4.x builds the default check is medium: the signature must verify and the certificate must be in that host's user trust store. A self-signed certificate is enough for a lab station. It is not a substitute for a CA-issued certificate on a job you ship.

Passwords never go in git. `security/*.p12`, `security/*.pkcs12`, `security/*.jceks`, `security/*.cer`, `security/*.pem`, `security/niagara.signing.xml`, and `security/niagara.signing.properties` are gitignored. Only the `*.example` files are tracked.

## Development certificate (Niagara 4.15.6, plugin 7.6.22)

This is the path that produced the signed jars. The profile file is Java properties XML (`<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">`). The plugin reads it with `Properties.loadFromXML`. A `<signingProfile>` document is rejected with `InvalidPropertiesFormatException`.

From the repo root, with `NIAGARA_HOME` set and `niagara/niagara_home` symlinked at the install (see the README):

```bash
./gradlew -c settings-niagara.gradle.kts createProfile \
  --profile-path=security/niagara.signing.xml --store-type=PKCS12 -Psigning.skip=true
./gradlew -c settings-niagara.gradle.kts generateCertificate \
  --profile-path=security/niagara.signing.xml --alias=controlbasic \
  --pem-file=security/dev-cert.pem -Psigning.skip=true
```

`createProfile` refuses to overwrite an existing profile. `generateCertificate` writes a random store password and key password into the profile and creates `security/niagara.signing.pkcs12`. The certificate verified on the jars is self-signed RSA 3072, SHA256withRSA, alias `controlbasic`, valid for 365 days. The signature has no timestamp.

Then build without `-Psigning.skip`:

```bash
export NIAGARA_SIGNING_PROFILE="$PWD/security/niagara.signing.xml"
export NIAGARA_SIGNING_ALIAS=controlbasic
./gradlew -c settings-niagara.gradle.kts jar
```

Confirm:

```bash
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-rt.jar"
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-wb.jar"
```

Both commands printed `jar verified` against the 4.15.6.20 install. The self-signed chain warning is expected. Import `security/dev-cert.pem` into the User Trust Store (steps below) before a station will accept the module at the default verification mode.

## Development certificate with keytool

`scripts/generate-dev-cert.sh` still creates a PKCS12 you choose the password for. Plugin 7.6.22 does not accept the XML that script writes, so do not point `niagara.signing.profile` at it on 4.15.6. Use the Gradle tasks above. The keytool steps remain if you only need a `.cer` to import by hand.

From the repo root, with a JDK on `PATH` (`keytool` ships with it; the 4.15.6.20 `jre/bin` has `keytool` and `javac`, and does not have `jarsigner`):

```bash
export NIAGARA_SIGNING_STORE_PASSWORD='pick-a-long-password'
export NIAGARA_SIGNING_KEY_PASSWORD='pick-a-long-password'   # optional; defaults to the store password
export NIAGARA_SIGNING_ALIAS=controlbasic                     # optional
./scripts/generate-dev-cert.sh
```

On Windows:

```bat
set NIAGARA_SIGNING_STORE_PASSWORD=pick-a-long-password
scripts\generate-dev-cert.bat
```

The script writes `security/dev-keystore.p12` (RSA 2048, SHA256withRSA, code-signing extended key usage, 10 year life) and `security/dev-cert.cer`. It also writes `security/niagara.signing.xml` in a layout plugin 7.6.22 rejects. Leave signing to the Gradle tasks in the section above. The module build reads `NIAGARA_SIGNING_PROFILE` or `niagara.signing.profile` and stops if that file is missing. It does not fall back to Tridium's throwaway dev certificate.

## Trust the certificate

Do this on every Workbench and every station host that will load the module. Trust is per host. A jar that loads on your laptop is refused on a JACE until that JACE trusts the same certificate.

1. Open Workbench.
2. Tools → Certificate Management.
3. Open the User Trust Store.
4. Import `security/dev-cert.cer` (or the PEM from `exportCertificate`).
5. Restart Workbench, and restart the station.

For a remote station host (supervisor or JACE), open the platform connection and use Platform Administration → Certificate Management → User Trust Store → Import. Then install the module with Software Manager rather than copying a jar onto the JACE by hand. Software Manager is what registers the module.

The store file under `niagara_user_home/security` is not the same name on every 4.13+ build. Prefer the Certificate Management dialog. If you must import from a shell, point `keytool -importcert` at the user trust store shown in that dialog, not at `niagara_home/jre/lib/security/cacerts`.

## What the station checks

`niagara.moduleVerificationMode` in the host's system properties is `low`, `medium`, or `high`.

- **low** — missing signatures warn. Do not rely on this for anything you leave running.
- **medium** — default on recent 4.x. The jar must be signed by a certificate in the user trust store.
- **high** — the certificate also has to satisfy the host's module permission policy.

Lowering the mode does not fix a bad certificate, and it is the wrong way to make a lab station accept a module you intend to keep.

## Production

Use a certificate issued for code signing, with the chain that signed it. Import that public certificate (or the issuing CA, if the host does not already trust it) into the same User Trust Store. Commercial CAs stopped issuing publicly trusted code-signing certificates whose private keys live only in a file, after the June 2023 CA/Browser Forum change; new certificates are meant to stay on a hardware token. Tridium's file-based signing profile does not take a hardware token directly. For a module you will ship, plan on either a code-signing certificate that the Niagara signing tool can use from a keystore, or Tridium's current guidance for HSM-backed certificates, and timestamp the signature (`timestampUrl` in the profile, default `http://timestamp.digicert.com`) so the signature still verifies after the certificate expires.

Keep the production keystore out of this repo. CI should receive the path and passwords as secrets, the same variables the local script uses.
