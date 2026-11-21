# Control Basic for Niagara 4

A Niagara 4 module that runs KMC-style Control Basic on the wire sheet. Drop **Control Basic Block** from the palette, link numeric and boolean slots, and edit the program in Workbench. The program is recompiled on save, and the block adds or removes linkable slots to match the names the program uses.

The interpreter (lexer, parser, compiler, executor) is plain Java with no Niagara jars. Those tests run here. The station and Workbench code is a thin layer that compiles only when `niagara_home` points at a Niagara 4.10 or newer install.

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

Java source and bytecode are Java 8. Niagara 4.10 through 4.15 load Java 8 classes. The Gradle plugin that *builds* the module is the 7.6 line shipped with newer Niagara developer tools; those tools can compile against a 4.10 `niagara_home`. If your install's plugin version is different, set `gradlePluginVersion` in `gradle.properties` to the directory name under `niagara_home/etc/m2/repository/com/tridium/niagara-module/`.

## Templates

The module layout follows modules and notes that are already in the open:

- [ci-richard-mcelhinney/nhaystack](https://github.com/ci-richard-mcelhinney/nhaystack) (`n4` branch) — `settings.gradle.kts`, root `build.gradle.kts` plugins (`com.tridium.niagara`, `vendor`, `niagara-signing`, `niagara-home-repositories`), `moduleManifest` / `runtimeProfile`, `*-rt` and `*-wb` build files, `@NiagaraType` / `@NiagaraProperty` with a Slot-o-matic region, `module-include.xml` as a `<types>` list, `module.palette` as a `bajaObjectGraph`, and `BEdgePane.setTop` / `setCenter` / `setBottom` plus `Command` / `BButton` / `doLoadValue`.
- [HawkenIndustries/Niagara4-ResoluteTagUtils](https://github.com/HawkenIndustries/Niagara4-ResoluteTagUtils) — `defaultAgent = AgentOn.Preference.PREFERRED` so double-click opens this view.
- [tridium/summit16-presentations](https://github.com/tridium/summit16-presentations) — why `rt` and `wb` are separate jars, and how Niagara 4 annotations replace AX slot comments.
- [cljohnson3rd/Niagara-Developer-Series](https://github.com/cljohnson3rd/Niagara-Developer-Series) `BDriverFinderNotificationPopup` — `BTextEditor(String, boolean)` and `BTextEditorPane`.
- Dynamic slots use `add(name, value, Flags.SUMMARY, facets, null)` the way Niagara component code does (the same call shape as the [BDynamicProperties gist](https://gist.github.com/mittens13/01c433927e557839fdb926f30893b029)). Inputs and variables stay writable. Outputs are `SUMMARY | READONLY`, so links can read them and the program is what writes them.

`BControlBasicSyntax` is the same kind of hook as `javax.baja.ui.text.BJavaSyntax`: a `BSyntax` installed with `BTextEditor.setSyntax`. Coloring and errors both come from the core lexer and `ControlBasic.compile`, so the red line is the same diagnostic the station stores in `lastError`. If your `bajaui` declares a different abstract method on `BSyntax`, only `BControlBasicSyntax.colorLine` needs to match `BJavaSyntax`. The token rules stay in `Highlight`.

## Slot rule (short)

Full detail is in [docs/LANGUAGE.md](docs/LANGUAGE.md).

- `AI` / `IN` numeric input, `AO` / `OUT` numeric output, `AV` / `VAR` numeric variable.
- `BI` / `BIN` boolean input, `BO` / `BOUT` boolean output, `BV` / `BVAR` boolean variable.
- Or declare them: `INPUT RoomTemp`, `OUTPUT Damper`, `BINPUT Occupied`, `BOUTPUT Fan`.
- Anything else is an input if the program only reads it, an output if it only writes it, and a variable if it does both. `START` / `STOP` count as boolean writes.
- Recompiling keeps a slot, and its links, when the name is still there. Names that vanished are removed. A compile error leaves the previous slots in place.

## 1. Run the interpreter tests

You need a JDK that can compile `--release 8` (JDK 8 through 21). From the repo root:

```bash
./gradlew test
```

That is `controlbasic-core` only. It does not download Niagara and does not sign anything.

`./gradlew sanity` compiles the `rt` and `wb` sources against `niagara-stubs`. That catches typos. It is not a Niagara build. The stubs are not copied into the module jars.

## 2. Point the build at Niagara

Copy `local.properties.example` to `local.properties` and set:

```properties
niagara_home=C:\\Niagara\\Niagara-4.14.0.142
niagara_user_home=C:\\Users\\you\\Niagara\\tridium\\4.14.0.142
```

`NIAGARA_HOME` works instead of the property. Use a full JDK for Gradle (`jar` and `jarsigner`). The JRE inside the Niagara install is missing those tools. Java 8 bytecode is what the module contains; the JDK that runs Gradle can be newer.

If plugin resolution fails, set `gradlePluginVersion` (see `gradle.properties`) to the version your install actually ships.

## 3. Create a signing certificate

Niagara refuses an unsigned module when verification is at its default. From the repo root:

```bash
export NIAGARA_SIGNING_STORE_PASSWORD='pick-a-long-password'
./scripts/generate-dev-cert.sh
```

Windows: `scripts\generate-dev-cert.bat` with `NIAGARA_SIGNING_STORE_PASSWORD` set.

Then in `local.properties`:

```properties
certAlias=controlbasic
niagara.signing.profile=security/niagara.signing.xml
```

Import `security/dev-cert.cer` into the Workbench User Trust Store **before** you expect the station to load the jar. Steps, the `createProfile` alternative, and production CA certificates are in [docs/SIGNING.md](docs/SIGNING.md).

## 4. Build and install

```bash
./gradlew -c settings-niagara.gradle.kts jar
```

The Niagara plugin compiles `controlBasic-rt` and `controlBasic-wb`, folds the core interpreter into the runtime jar, signs both jars, and copies them to `niagara_home/modules` (`controlBasic-rt.jar` and `controlBasic-wb.jar`). If they are not there, copy them from each module's `build/libs`.

Check the signature:

```bash
jarsigner -verify -verbose "$NIAGARA_HOME/modules/controlBasic-rt.jar"
```

Restart Workbench and the station. On a JACE, install through Platform → Software Manager instead of copying the file yourself.

A new block's program is `examples/damper.cb`. After the first successful compile (station start, or Save in the editor) the wire sheet shows `AI1`, `VAR1`, `AO1`, and `BO1`.

## 5. Use the block

1. Open the palette **controlBasic** and drop **ControlBasicBlock** on a wire sheet.
2. Double-click it. The Control Basic editor is the default view: line-number gutter, keyword and comment coloring, and a status line.
3. Editing runs the same compiler the station uses. The status line shows `Line N: …` and that line is colored as an error.
4. **Compile** checks the buffer and does not write it. **Save** stores the program, recompiles, and rebuilds slots.
5. Link inputs and outputs like any other block. `scanInterval` defaults to 1 second. `maxInstructions` defaults to 10000. `lastError` is the fault text. `console` is what `PRINT` wrote. **Reset** clears variables, timers, and a `HALT`.
6. Table 1..4 are optional `x,y; x,y` lists for `TBL(x, n)`.

The scan does not run on the station engine thread. A clock ticket wakes a single daemon thread. If a scan is still going, the next tick is skipped.

## What was verified without a Niagara install

- `./gradlew :controlbasic-core:test` — arithmetic, `IF` / `ELSEIF` / `ENDIF`, `GOTO` / `GOSUB` / `FOR`, `IF+`, `TIME` / `TIMEON`, slot classification, slot diffing that keeps an existing name, instruction limit, parse and runtime line numbers, the sample program.
- `./gradlew sanity` — `BControlBasicBlock` and `BControlBasicEditor` compile against the stubs in this repo.

## What needs your Niagara install

- The Gradle plugin actually resolving from `niagara_home/etc/m2/repository`, Slot-o-matic rewriting the generated regions, and `jarsigner` accepting the profile.
- Workbench opening the editor on double-click, painting `BSyntax` colors, and linking the dynamic `BStatusNumeric` / `BStatusBoolean` slots.
- A station clock firing the scan and `configFail` showing `lastError`.

## Questions worth a follow-up

- Which Niagara build are you compiling against (4.10, 4.13, 4.14, 4.15)? The module bytecode targets 4.10+. The Gradle plugin version has to match the tools in that install.
- Do the programs you paste use line numbers, or labels and `ENDIF`? Both work. If one dialect is the one you care about, the error messages can lean that way.
- Are the names you want on the wire sheet the KMC prefixes (`AI1`, `BO1`) or declared names (`INPUT RoomTemp`)? Both are implemented. The prefix list is easy to extend.
- Should `DEWPOINT` / `ENTHALPY` match a specific KMC firmware's units, or is the approximation in `docs/LANGUAGE.md` enough?
- What vendor string should be on the jar? It is `ControlBasic` today, in `niagara.gradle.kts`.
