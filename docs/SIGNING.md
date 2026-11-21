# Signing the module

Niagara 4 will not load a module jar unless the jar is signed and the station or Workbench trusts the signing certificate. On current 4.x builds the default check is medium: the signature must verify and the certificate must be in that host's user trust store. A self-signed certificate is enough for a lab station. It is not a substitute for a CA-issued certificate on a job you ship.

Passwords never go in git. `security/*.p12`, `security/*.cer`, `security/niagara.signing.xml`, and `security/niagara.signing.properties` are gitignored. Only the `*.example` files are tracked.

## Development certificate with keytool

From the repo root, with a JDK on `PATH` (`keytool` ships with it; the JRE inside Niagara often omits it):

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

The script writes:

- `security/dev-keystore.p12` — PKCS12 keystore, RSA 2048, SHA256withRSA, code-signing extended key usage, 10 year life
- `security/dev-cert.cer` — public certificate to import into Niagara
- `security/niagara.signing.xml` and `security/niagara.signing.properties` — alias, keystore path, and passwords for the Gradle signing plugin

`local.properties` (also gitignored; copy from `local.properties.example`):

```properties
certAlias=controlbasic
niagara.signing.profile=security/niagara.signing.xml
```

You can point `niagara.signing.profile` at the `.properties` file instead, or set `NIAGARA_SIGNING_PROFILE` / `CERT_ALIAS` in the environment. The Gradle build reads those and passes them to `com.tridium.niagara-signing`. It does not fall back to Tridium's throwaway dev certificate. If the profile file is missing, the module build stops.

The XML the script writes names `com.tridium.gradle.plugins.signing.profile.RestrictedSigningProfile` and the keystore fields Tridium's signing talks describe. If your exact plugin build rejects that file, generate one the plugin itself understands:

```bash
./gradlew -c settings-niagara.gradle.kts :createProfile --profile-path security/niagara.signing.xml -Psigning.skip=true
./gradlew -c settings-niagara.gradle.kts :generateCertificate --profile-path security/niagara.signing.xml --alias controlbasic -Psigning.skip=true
./gradlew -c settings-niagara.gradle.kts :exportCertificate --profile-path security/niagara.signing.xml --alias controlbasic --pem -Psigning.skip=true
```

`-Psigning.skip=true` is only for those one-time tasks, before a profile exists. Do not use it to build a jar you plan to install. After `createProfile`, point `niagara.signing.profile` at the file it wrote and build again without the skip flag. You can still import the keytool `.cer` if you would rather keep the keytool key; the profile just has to reference that keystore.

Confirm the jar after a successful module build:

```bash
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-rt.jar"
```

A self-signed jar verifies with a warning that the certificate chain is not from a public CA. That warning is expected. Niagara's own check is whether the host trusts the certificate, which is the next step.

## Trust the certificate

Do this on every Workbench and every station host that will load the module. Trust is per host. A jar that loads on your laptop is refused on a JACE until that JACE trusts the same certificate.

1. Open Workbench.
2. Tools → Certificate Management.
3. Open the User Trust Store.
4. Import `security/dev-cert.cer` (or the PEM from `exportCertificate`).
5. Restart Workbench, and restart the station.

For a remote station host (supervisor or JACE), open the platform connection and use Platform Administration → Certificate Management → User Trust Store → Import. Then install the module with Software Manager rather than copying a jar onto the JACE by hand. Software Manager is what registers the module.

The store file under `niagara_user_home/security` has changed names across 4.10–4.15. Prefer the Certificate Management dialog. If you must import from a shell, point `keytool -importcert` at the user trust store shown in that dialog, not at `niagara_home/jre/lib/security/cacerts`.

## What the station checks

`niagara.moduleVerificationMode` in the host's system properties is `low`, `medium`, or `high`.

- **low** — missing signatures warn. Do not rely on this for anything you leave running.
- **medium** — default on recent 4.x. The jar must be signed by a certificate in the user trust store.
- **high** — the certificate also has to satisfy the host's module permission policy.

Lowering the mode does not fix a bad certificate, and it is the wrong way to make a lab station accept a module you intend to keep.

## Production

Use a certificate issued for code signing, with the chain that signed it. Import that public certificate (or the issuing CA, if the host does not already trust it) into the same User Trust Store. Commercial CAs stopped issuing publicly trusted code-signing certificates whose private keys live only in a file, after the June 2023 CA/Browser Forum change; new certificates are meant to stay on a hardware token. Tridium's file-based signing profile does not take a hardware token directly. For a module you will ship, plan on either a code-signing certificate that the Niagara signing tool can use from a keystore, or Tridium's current guidance for HSM-backed certificates, and timestamp the signature (`timestampUrl` in the profile, default `http://timestamp.digicert.com`) so the signature still verifies after the certificate expires.

Keep the production keystore out of this repo. CI should receive the path and passwords as secrets, the same variables the local script uses.
