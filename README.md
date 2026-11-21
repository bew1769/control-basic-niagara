# Control Basic for Niagara 4

A Niagara 4 module that runs KMC-style Control Basic on the wire sheet. Drop **Control Basic Block** from the palette, link numeric and boolean slots, and edit the program in Workbench. The program is recompiled on save, and the block adds or removes linkable slots to match the names the program uses.

The interpreter (lexer, parser, compiler, executor) is plain Java with no Niagara jars. Those tests run here. The station and Workbench code compiles when `niagara_home` points at Niagara 4.13 or later.

## Layout

| Path | What it is |
| --- | --- |
| `controlbasic-core` | Interpreter. JUnit tests. No Niagara types. |
| `controlBasic-rt` | `BControlBasicBlock`. Runs on the station. |
| `controlBasic-wb` | `BControlBasicEditor`, the default Workbench view. |
| `niagara-stubs` | Compile-only stand-ins for baja/bajaui. Not part of the module. |
| `docs/LANGUAGE.md` | What the interpreter accepts, and the slot rule. |
| `docs/SIGNING.md` | Keystore, trust store, production certificates. |
| `examples/damper.cb` | The program a new block starts with. |

The module targets Niagara 4.13 and later. Source and bytecode stay Java 8, which is what 4.13 stations load. The build uses the Gradle 7 plugin set shipped with those installs (`com.tridium.niagara`, `com.tridium.vendor`, `com.tridium.niagara-module`, `com.tridium.niagara-signing`). If resolution fails, set `gradlePluginVersion` in `gradle.properties` to the version directory under `niagara_home/etc/m2/repository/com/tridium/niagara-module/`. The vendor string on the jar is `bew1769`.

This was built and signed against a Niagara **4.15.6.20** Linux Supervisor (Gradle plugins **7.6.22**, settings plugin **7.6.3**). The 4.13 minimum stays: the calls that compile here (`TextParser`, `Clock.schedulePeriodically(BComponent, BRelTime, Action, BValue)`, `getDynamicPropertiesArray()`, `BFacets.make(String, boolean)`) are the 4.13-era shapes. It was not compiled against a 4.13 install. `BComponent` in 4.15 has no `configFail`; the fault text is the `lastError` slot.

## Templates

The module layout follows modules and notes that are already in the open:

- [ci-richard-mcelhinney/nhaystack](https://github.com/ci-richard-mcelhinney/nhaystack) (`n4` branch) — `settings.gradle.kts`, root `build.gradle.kts` plugins (`com.tridium.niagara`, `vendor`, `niagara-signing`, `niagara-home-repositories`), `moduleManifest` / `runtimeProfile`, `*-rt` and `*-wb` build files, `@NiagaraType` / `@NiagaraProperty` with a Slot-o-matic region, `module-include.xml` as a `<types>` list, `module.palette` as a `bajaObjectGraph`, and `BEdgePane.setTop` / `setCenter` / `setBottom` plus `Command` / `BButton` / `doLoadValue`.
- [HawkenIndustries/Niagara4-ResoluteTagUtils](https://github.com/HawkenIndustries/Niagara4-ResoluteTagUtils) — `defaultAgent = AgentOn.Preference.PREFERRED` so double-click opens this view.
- [tridium/summit16-presentations](https://github.com/tridium/summit16-presentations) — why `rt` and `wb` are separate jars, and how Niagara 4 annotations replace AX slot comments.
- [cljohnson3rd/Niagara-Developer-Series](https://github.com/cljohnson3rd/Niagara-Developer-Series) `BDriverFinderNotificationPopup` — `BTextEditor(String, boolean)` and `BTextEditorPane`.
- Dynamic slots use `add(name, value, Flags.SUMMARY, facets, null)` the way Niagara component code does (the same call shape as the [BDynamicProperties gist](https://gist.github.com/mittens13/01c433927e557839fdb926f30893b029)). Inputs and variables stay writable. Outputs are `SUMMARY | READONLY`, so links can read them and the program is what writes them.

`BControlBasicSyntax` extends `javax.baja.ui.text.TextParser` and is installed with `BTextEditor.setParser`. Niagara 4.15 has no `javax.baja.ui.text.BSyntax`. Coloring comes from the core lexer. An error line uses `Segment.PREPROCESSOR` (there is no error segment type) and the status line under the editor. The token rules stay in `Highlight`.

## Slot names (short)

Full detail is in [docs/LANGUAGE.md](docs/LANGUAGE.md).

A slot is a KMC object plus an optional [SEBA](https://bms-applications.schneider-electric.com/seba-naming-convention) descriptor: `AI1`, `AI1_RmTmp`, `AO1_DprCmd`, `BI1_OccSts`, `BO1_SaFanCmd`, `AV1_ZnTmpSpt`, `BV1_SchdOcc`. The prefix sets the type. The descriptor is camel-case SEBA text; an underscore inside it is only the `CO2_1` enumeration form. Other names stay internal. Recompiling keeps a slot, and its links, when the name is still there.

## 1. Run the interpreter tests

You need a JDK that can compile `--release 8` (JDK 8 through 21). From the repo root:

```bash
./gradlew test
```

That is `controlbasic-core` only. It does not download Niagara and does not sign anything.

`./gradlew sanity` compiles the `rt` and `wb` sources against `niagara-stubs`. That catches typos. It is not a Niagara build. The stubs are not copied into the module jars.

## 2. Point the build at Niagara

`settings-niagara.gradle.kts` reads `niagara_home` from `local.properties`, then `NIAGARA_HOME`. That path is only the Gradle plugin repository (`$NIAGARA_HOME/etc/m2/repository`).

The module plugin resolves `baja.jar` and the other runtime jars from a directory named `niagara/niagara_home` under the repo, not from the environment variable. Symlink the install there (both that directory and `.niagara/` are gitignored; do not commit Tridium files):

```bash
mkdir -p niagara
ln -sfn /path/to/Niagara-4.15.6.20 niagara/niagara_home
export NIAGARA_HOME=/path/to/Niagara-4.15.6.20
```

`compileJava` asks for a Java 8 toolchain. The 4.15.6.20 `jre` is Zulu 8.0.502 and includes `javac`. Tell Gradle about it (this machine path stays out of git):

```properties
org.gradle.java.installations.paths=/path/to/Niagara-4.15.6.20/jre
```

Gradle itself can run on a newer JDK. On this machine that was OpenJDK 21.0.13. The Niagara `jre/bin` does not include `jarsigner`; verification used the system JDK's `jarsigner`.

## 3. Create a signing certificate

Niagara 4.15.6 plugin 7.6.22 stores the profile as a Java `properties` XML document (`Properties.loadFromXML`). A hand-written `<signingProfile>` file fails the jar task with `InvalidPropertiesFormatException`. Create the profile with the signing plugin. `-Psigning.skip=true` is only for these two tasks, before a profile exists:

```bash
./gradlew -c settings-niagara.gradle.kts createProfile \
  --profile-path=security/niagara.signing.xml --store-type=PKCS12 -Psigning.skip=true
./gradlew -c settings-niagara.gradle.kts generateCertificate \
  --profile-path=security/niagara.signing.xml --alias=controlbasic \
  --pem-file=security/dev-cert.pem -Psigning.skip=true
```

That writes `security/niagara.signing.xml` (passwords included), a sibling keystore `security/niagara.signing.pkcs12`, and the PEM. None of those are committed. The certificate is self-signed, RSA 3072, SHA256withRSA, validity 365 days, alias `controlbasic`. Import the PEM into the Workbench User Trust Store before you expect a station to load the jar. Details are in [docs/SIGNING.md](docs/SIGNING.md).

`scripts/generate-dev-cert.sh` still builds a keytool PKCS12 if you want a certificate you control the password of. Plugin 7.6.22 will not sign with the XML that script writes.

## 4. Build and install

```bash
export NIAGARA_SIGNING_PROFILE="$PWD/security/niagara.signing.xml"
export NIAGARA_SIGNING_ALIAS=controlbasic
./gradlew -c settings-niagara.gradle.kts jar
```

The plugin compiles `controlBasic-rt` and `controlBasic-wb`, folds the core interpreter into the runtime jar, signs both jars, and copies them to `niagara_home/modules`. The workbench jar also needs `gx-rt` on the compile classpath: `BButton` and `BTextEditor` signatures reference `javax.baja.gx.BImage`, which is not inside `gx-wb`.

`module-include.xml` must not start with an XML declaration. Slot-o-matic wraps the file in `<dummyroot>`, and a declaration after that is a parse error.

Check the signature:

```bash
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-rt.jar"
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-wb.jar"
```

`jar verified` is the result that matters. A self-signed jar also warns that the certificate chain is not from a public CA, and this profile does not add a timestamp.

Restart Workbench and the station. On a JACE, install through Platform → Software Manager instead of copying the file yourself.

A new block's program is `examples/damper.cb`. After the first successful compile the wire sheet shows `AI1_RmTmp`, `AV1_ZnTmpSpt`, `AO1_DprCmd`, and `BO1_FanCmd`. The scan is a hidden `scanTick` action. `Clock` has no `Runnable` overload in 4.15; `Clock.schedulePeriodically(this, interval, scanTick, null)` queues that action, and the action hands the scan to a daemon thread.

## 5. Use the block

1. Open the palette **controlBasic** and drop **ControlBasicBlock** on a wire sheet.
2. Double-click it. The Control Basic editor is the default view: line-number gutter, keyword and comment coloring, and a status line.
3. Editing runs the same compiler the station uses. The status line shows `Line N: …` and that line is colored as an error.
4. **Compile** checks the buffer and does not write it. **Save** stores the program, recompiles, and rebuilds slots.
5. Link inputs and outputs like any other block. `scanInterval` defaults to 1 second. `maxInstructions` defaults to 10000. `lastError` is the fault text. `console` is what `PRINT` wrote. **Reset** clears variables, timers, and a `HALT`.
6. Table 1..4 are optional `x,y; x,y` lists for `TBL(x, n)`.

The scan does not run on the station engine thread. A clock ticket wakes a single daemon thread. If a scan is still going, the next tick is skipped.

## What was verified on Niagara 4.15.6.20

The Supervisor zip (`Tridium_EMEA_N4_Supervisor_for_Linux_x64-4.15.6.20.zip`) downloaded over HTTP without a login. A silent install accepted the license and created the `niagara` user. The install tree is gitignored.

- `./gradlew -c settings-niagara.gradle.kts jar` compiled both module parts against that install's jars and plugin 7.6.22, signed them, and copied them to `modules/`.
- Signed files: `controlBasic-rt.jar` and `controlBasic-wb.jar`. `jarsigner -verify` reported `jar verified` for both. Signer `CN=ubuntu@cursor(controlbasic), OU=For Development Purposes Only Do Not Distribute, O=Tridium, L=Richmond, ST=Virginia, C=US`. Digest SHA-256, signature SHA256withRSA, 3072-bit key. Self-signed, no timestamp, certificate expires 2027-10-31.
- `nre -modules:controlBasic` listed `controlBasic-rt bew1769 1.0.0` and `controlBasic-wb bew1769 1.0.0`. The user-home registry contains `ControlBasicBlock` and `ControlBasicEditor`. The module signature registry loaded without rejecting the jars.
- `./gradlew test` and `./gradlew sanity` still cover the interpreter and the stub compile.

## What the license blocked

`nre -licenses` reported `Licenses (perpetual) none` and `Features none` for host `LNX-EB1D-0A44-FAB8-A2F3`. Starting a station (`station` with no arguments, which goes through `nre`) stopped before a station existed:

```
javax.baja.license.FeatureNotLicensedException: tridium:nre
    at com.tridium.sys.license.NLicenseManager.checkFeature(NLicenseManager.java:95)
    at com.tridium.sys.Nre.runClass(Nre.java:669)
```

The block was not instantiated, a program was not scanned, and dynamic slots were not observed on a running station. Workbench was not started. Those checks need a license that includes the `tridium:nre` feature, plus the dev certificate in that host's user trust store.

Line-numbered programs are the form this module is written for. Labels and `ENDIF` still parse. `DEWPOINT` and `ENTHALPY` use the formulas in `docs/LANGUAGE.md`, not a KMC firmware.
