# Control Basic in this module

This interpreter runs line-numbered KMC Control Basic, the form used when a program is pasted in from a KMC controller. Each line starts with a line number (`10`, `20`, …). `GOTO` and `GOSUB` target those numbers. Labels and block `IF` / `ELSE` / `ENDIF` still parse.

Keywords are not case-sensitive. A line may hold several statements separated by `:`. Comments are `REM`, `'` and `//`.

Diagnostics report the **source line in the editor** (the first line of the file is line 1), which is what the Workbench view highlights.

## Slot names

A wire-sheet slot is a KMC object address plus an optional SEBA descriptor:

```text
AI1
AI1_RmTmp
AO2_DprCmd
AV1_ZnTmpSpt
BI1_OccSts
BO1_SaFanCmd
BV1_SchdOcc
AI3_CO2_1
```

The shape is `{prefix}{number}` or `{prefix}{number}_{descriptor}`.

| Prefix | BACnet object | Slot |
| --- | --- | --- |
| `AI` | analog input | numeric input |
| `AO` | analog output | numeric output |
| `AV` | analog value | numeric variable, read and written each scan |
| `BI` | binary input | boolean input |
| `BO` | binary output | boolean output |
| `BV` | binary value | boolean variable, read and written each scan |

The descriptor follows Schneider Electric's SEBA naming convention. The abbreviation dictionary is [SEBA Naming Convention](https://bms-applications.schneider-electric.com/seba-naming-convention) (`RmTmp` room temperature, `ZnTmp` zone temperature, `SaFanCmd` supply-air fan command, `OccSts` occupancy status). The combination rules are the SEBA Point Naming Standards published with that site:

- Build the name generic-to-specific by concatenating the camel-case abbreviations. `Fcu1` + `Oa` + `Dpr` + `Pos` + `Spt` + `Min` is `Fcu1OaDprPosSptMin`.
- Put modifiers on the end: `OaDprPosMax`, not `MaxOaDprPos`.
- A number may suffix an abbreviation: `Ct1Fan`, `CtFan2`, and `Ct1Fan2`.
- An underscore inside the descriptor is only the enumeration separator used when the abbreviation itself contains digits, as in `CO2_1`.
- Not every naming level has to appear. `SaFanCmd` is a complete name.

`AI1_RoomTemp` is the same shape as `AI1_RmTmp`. The descriptor does not have to be a dictionary token, so a readable camel-case word is accepted. `AI1_Room_Temp` is not a slot: that underscore is not an enumeration, and the name stays an internal variable.

`INPUT` and `OUTPUT` declarations do not create slots. A `FOR` index does not create a slot. Built-ins (`TIME`, `FIRST`, `HOURS`, function names) do not create slots. Arrays (`DIM`) do not create slots.

Names are case-insensitive. The slot keeps the first spelling (`AI1_RmTmp` and `ai1_rmtmp` are one slot).

Niagara slot names are letters, digits, and underscore, and they start with a letter or underscore. The block passes the name through `javax.baja.naming.SlotPath.escape`, which is the `$xx` / `$uxxxx` escaping the Niagara developer guide describes for slot names and `TextUtil`. `AI1_RmTmp` is already valid. A space becomes `$20` and a hyphen becomes `$2d`.

Saving a program that compiles rebuilds dynamic slots. A name that is still present keeps its slot object, so links stay. A name that disappeared is removed. A boolean/numeric type change removes and recreates the slot. Slots this block created carry the facet `cbSlot=true`. A failed compile does not change slots, and the last program that compiled keeps scanning. **Last Error** shows the compile error followed by `(still running the last program that compiled)` until a fixed program is saved.

Each scan copies every slot into the program, runs from the top, then writes outputs and variables back. Inputs are not written back.

## Status flags

Every slot is a Niagara status value, and the program can read the status of each input. KMC Control Basic reads BACnet objects, whose Status Flags are IN ALARM, FAULT, OVERRIDDEN and OUT OF SERVICE (KMC Connect Reference Rev N, Table 35-5), with the `object.property` form (Table 28-6, pp.228-229). The same form reads them here:

| Expression | Value |
| --- | --- |
| `AI1.FAULT` | 1 when the input's status has `fault` |
| `AI1.OVERRIDDEN` | 1 when it has `overridden` |
| `AI1.IN_ALARM` (or `.ALARM`) | 1 when it has `alarm` or `unackedAlarm` |
| `AI1.OUT_OF_SERVICE` (or `.OOS`, `.DISABLED`) | 1 when it has `disabled`, Niagara's out-of-service flag |
| `AI1.NULL`, `AI1.DOWN`, `AI1.STALE` | Niagara flags with no BACnet Status Flag |
| `AI1.STATUS` | the raw Niagara status bits: disabled 1, fault 2, down 4, alarm 8, stale 16, overridden 32, null 64, unackedAlarm 128 |
| `AI1.PV` | the value, same as `AI1` |

The properties are read-only (`AI1.FAULT = 1` is a compile error), except that `AV1.PV = 5` is the same as `AV1 = 5`. An unknown property is a compile error. Reading `AI1.FAULT` creates the `AI1` input slot like any other read.

Values:

- An input whose status is **null** reads as `NAN`, so `ISNAN(AI1)` tests for it and arithmetic on it gives `NAN`. KMC uses `NAN` for a value that is not available (`ISNAN`, Connect Rev N p.289). A null boolean input is false in `IF`.
- A **down** input keeps its last value. KMC holds the last good value while a remote device is offline (`ISNAN`, p.289). Test `AI1.DOWN` if that matters.
- Fault, overridden, alarm, stale and disabled inputs are read as their value; test the flag to act on it.

Outputs and variables (`AO`, `BO`, `AV`, `BV` slots):

| After the scan | Value | Status |
| --- | --- | --- |
| Clean scan | written | ok, or **null** when the value is `NAN` |
| Runtime error | not written (the last value stays) | **fault** |
| `HALT` | written | **fault** (the program no longer updates it) |
| Block disabled | not written | **disabled** |

The input's status is not copied to the outputs. KMC does not do that either: a program reads flags and decides. Write `IF AI1.FAULT THEN AO1 = 0 ELSE AO1 = AI1` when you want a fallback.

## Block status, Program State and the fault alarm

The block itself follows the KMC/BACnet Program object (Connect Rev N, section 35, Program object properties; Tables 35-20 and 35-23):

| Property | Values |
| --- | --- |
| **Status** | `{ok}`, `{fault}` while Reliability is not NO_FAULT_DETECTED, `{disabled}` when Enabled is false. The block implements `BIStatus`, so the wire sheet colors it. |
| **Program State** | `RUNNING`, `HALTED` (after `HALT`), `IDLE` (disabled, or nothing has compiled yet) |
| **Reliability** | `NO_FAULT_DETECTED` or `PROCESS_ERROR` |
| **Last Error** | the fault text, or empty when healthy |

The fault text is one of:

- `Compile error: Line 3: Expected an expression (still running the last program that compiled)`, after saving a program that does not compile. The last good program keeps running.
- `Runtime error: Line 5: Division by zero`, while scans fail.
- `Halted: Low pressure` (`HALT "Low pressure"`) or `Halted by HALT`. KMC puts the HALT message in Description Of Halt (p.283).

When the block enters a fault it:

1. writes the text to **Last Error**;
2. logs a WARNING on the `controlBasic` logger with the block name and the text (an INFO line when it clears);
3. raises a Niagara **fault alarm** through `javax.baja.alarm.AlarmSupport`, with the text in `msgText`, routed to the alarm class in **Alarm Source Info** (`defaultAlarmClass` unless you change it).

A new fault text (another bad edit, a different runtime error) returns the open alarm to normal and raises a new one with the new text. When the program compiles again and scans cleanly, the alarm returns to normal. One alarm is raised per fault, not one per scan. Alarms wait for the station to reach steady state, so a program that fails to compile at boot is alarmed once the alarm service is running.

## Execution model

The block runs the program from the first statement every scan. Variables, timers, `IF+` / `IF-` memory, and PID state persist until Reset or a successful recompile. `END` ends that scan. `HALT` stops later scans until Reset. `GOTO` loops are cut off by **Max Instructions Per Scan** (default 10000). The scan runs on a daemon thread. The clock callback only queues it, and a scan still in progress is skipped rather than queued up.

`FIRST` is 1 on the first scan after start, reset, or recompile. `SCANS` is the scan rate in scans per second, measured from the previous scan (1 / Scan Interval on the first scan), as KMC defines it (p.316), so KMC's `A = 1/SCANS : B = A + B` counts seconds. `SCANCOUNT` counts scans (an extension). `SCAN` is the configured interval in seconds.

## Operators

Highest to lowest, as in KMC Connect Rev N, Illustration 28-2 "Order of operator precedence" (p.223): `()`, `NOT`, `^`, `*` `/`, `\`, `MOD`, `+` `-`, `<` `>` `<=` `>=`, `=` `<>`, `AND`, `OR`, `XOR`. "Operations at the same level of precedence are evaluated from left to right", so `2 ^ 3 ^ 2` is 64 and `8 / 4 / 2` is 1. `NOT` binds tighter than `^`, so `NOT 2 ^ 2` is `(NOT 2) ^ 2` = 0 and `NOT A = 0` is `(NOT A) = 0`. Use parentheses when you mean `NOT (A = 0)`. The manual does not list unary minus; here `^` binds tighter than a leading minus, so `-2^2` is `-4`, and an exponent may carry its own sign, so `2 ^ -1` is 0.5.

`AND` and `OR` stop early, so `IF FAN AND 1/FAN > 0` is safe when `FAN` is 0. Comparisons and logic yield 1 or 0. Zero is false.

| Operator | Meaning |
| --- | --- |
| `^` | power |
| `*`, `/` | multiply, real divide |
| `\` | integer divide, toward zero |
| `MOD` | remainder, toward zero |
| `+`, `-` | add, subtract |
| `<`, `>`, `<=`, `>=` | relational |
| `=`, `<>` | equal, not equal |
| `AND` | logic and |
| `OR` | logic or |
| `XOR` | logic exclusive or |

`AND` binds tighter than `OR`, and `OR` tighter than `XOR`, so `1 OR 1 AND 0` is 1 and `1 XOR 1 OR 1` is 0. Operators on one row are evaluated left to right. Use parentheses whenever you mix logic operators.

`00:07:00` and `8:30` are times in seconds (`HH:MM:SS` or `HH:MM`). `TIME` is seconds after midnight, so `TIME > 00:07:00` matches the KMC sample. `HOURS` is 3600, `MINUTES` is 60, `SECONDS` is 1.

## NAN and infinity

`NAN`, `POS_INFINITY` and `NEG_INFINITY` (also `NEG-INFINITY`) are values (Connect Rev N pp.296, 310). `ISNAN(x)` is 1 for NAN (p.289). `NAN` may be stored in a variable or written to an output, which then goes null. KMC's own example tests `IF A <> NAN` (p.296), so here `NAN = NAN` is true and `NAN <> NAN` is false, unlike IEEE. Arithmetic with a NAN operand gives NAN.

## Errors

A compile error (syntax, `GOTO` to a missing line, `NEXT` without `FOR`, an unsupported statement, or `NAME(x)` where `NAME` is neither a function nor an array that is `DIM`med or assigned somewhere) sets **Last Error** with the line and column. The previous program and its slots stay in place, and the previous program keeps running.

Every compile and runtime error also faults the block, logs, and raises the fault alarm described above.

A runtime error stops that scan. **Last Error** shows the line and message, outputs are not written for that scan, and they get the fault status. The next scan runs again from the top. Runtime errors are division by zero (`/`, `\`, `MOD`), a negative `SQR`, `LN` or `LOG` of 0 or less, `ARCSIN` or `ARCCOS` outside -1..1, arithmetic on finite numbers whose result is NaN or infinite (for example `0 ^ -1`, `(-8) ^ 0.5`, or an overflow), an array index out of range, `RETURN` without `GOSUB`, and going over **Max Instructions Per Scan**. The manual does not say what a controller does on these; it only says a bad object mnemonic halts the program (p.227 caution).

KMC handles an error with the statement *after* the one that can fail (Connect Rev N):

- `ONERROR location` "redirects the program to the line specified by location when an error is detected in the previous line" (p.305). Put it right after the risky line: `10 A = AI1 / AI2` then `20 ONERROR 100`. `ONERROR GOTO 100` is accepted too. `ONERROR OFF` is accepted and does nothing.
- `ERRORLEVEL` "returns a non zero value when an error is detected in the previous statement" (p.278): `10 A = SQR(AI1)` then `20 IF ERRORLEVEL THEN A = 0`.

When the next statement is one of these, the error does not stop the scan and does not fault the block. The previous statement of a one-line `IF` is the whole `IF` line, so KMC's example (`IF ... THEN AV24 = 4410.AI4` followed by `ONERROR`) works. An error two statements above `ONERROR` still stops the scan.

## Statements that run

`LET` (optional), assignment, `A(i) =`, `DIM A(n)` (indexes are 1…n), `IF`, `IF+`, `IF-`, `THEN`, `ELSE`, `ELSEIF`, `ENDIF` / `END IF`, `GOTO`, `GOSUB`, `RETURN`, `FOR` / `TO` / `STEP` / `NEXT`, `ON n GOTO`, `ON n GOSUB`, `ONERROR location`, `ONERROR OFF`, `INC(point[, step])`, `DEC(point[, step])`, `START`, `STOP`, `ENABLE`, `DISABLE`, `OPEN`, `CLOSE`, `PRINT`, `END`, `HALT ["message"]`, `REM`.

`INC` and `DEC` add or subtract the step (default 1), as on Connect Rev N p.286 and p.270. `HALT "message"` stops the program until Reset, a recompile, or a station restart (KMC: a warm or cold start or Program Change = Run, p.283); the block shows Program State HALTED and `Halted: message`. `INPUT` / `OUTPUT` / `VAR` declarations are accepted and do not create wire-sheet slots.

`IF+` runs the THEN branch only on the scan the condition becomes true. `IF-` runs it only on the scan it becomes false. The first scan treats the previous value as false.

`ON n` is 1-based. An index outside the list falls through.

`THEN 100` is a goto, in the Standard dialect.

## Functions

| Function | Notes |
| --- | --- |
| `ABS` `SGN` `INT` `FIX` `FRAC` `ROUND` | `INT` is floor, so `INT(-1.2)` is -2. `FIX` truncates toward zero. `ROUND(x)` or `ROUND(x, digits)`. Halves round away from zero: `ROUND(2.5)` is 3, `ROUND(-2.5)` is -3. |
| `SQR` / `SQRT` `EXP` `LN` `LOG` / `LOG10` | `LOG` is base 10. `INVLN(x)` is `EXP(x)`. |
| `SIN` `COS` `TAN` `ATN` / `ARCTAN` `ARCSIN` `ARCCOS` | Radians. `DEG` and `RAD` convert. |
| `MIN` `MAX` | Two or more arguments. |
| `AVG` | `AVG(a, b, ...)`, one or more arguments (p.258). |
| `ISNAN` | 1 when the argument is NAN (p.289). |
| `LIMIT` / `CLAMP` | `LIMIT(value, low, high)`. |
| `SCALE` | `SCALE(x, inLo, inHi, outLo, outHi)`. Does not clamp; wrap with `LIMIT`. |
| `TABLE` | `TABLE(x, x1, y1, x2, y2, …)` linear interpolation, flat outside the ends. |
| `TBL` | `TBL(x, n)` uses the block's Table 1..4 text: `0,0; 10,100; 20,50`. |
| `TIME` `DOW` `DOM` `DOY` `MONTH` `YEAR` | `DOW` is Monday 1 through Sunday 7, and `MON` … `SUN` are the constants 1 … 7, so `IF DOW = MON` works (KMC BACnet Module Rev E p.49; Connect Rev N p.272). `DOY` counts February as 29 days every year: March 1 is always day 61 and December 31 is day 366; day 60 is skipped in other years (p.273). |
| `TIMEON` `TIMEOFF` | Seconds the variable has been continuously non-zero, or zero. Argument is a variable name. Wall clock, not scan count. |
| `SENSORON` `SENSOROFF` | 1 on the scan the variable rises or falls. Same idea as KMC `SENSOR-ON` / `IF+`. |
| `INTERVAL` | `INTERVAL(seconds)` is 1 on the first call and again after that many seconds. Each call site has its own timer. |
| `PID` | `PID(pv, sp, kp, ki, kd)` or with `outMin, outMax`. Output is not a percent unless you clamp it. Each call site keeps its own integral. Anti-windup drops the last integral step when the output saturates. The derivative term is 0 on the first call after start or reset, so there is no derivative kick. |
| `DEWPOINT` `DEWPOINTSI` | KMC argument order, humidity first (pp.270-271): `DEWPOINT(OAH, OAT)` takes % RH and °F and returns °F; `DEWPOINTSI(OAH, OAT)` takes % RH and °C and returns °C. Magnus formula; KMC does not publish its formula. |
| `ENTHALPY` `ENTHALPYSI` | KMC argument order (p.277): `ENTHALPY(OAH, OAT)` takes % RH and °F and returns Btu/lb of dry air (0.240·T + W·(1061 + 0.444·T)); `ENTHALPYSI(OAH, OAT)` takes % RH and °C and returns kJ/kg. Both at 101.325 kPa. |
| `RND` | `RND(n)` is a whole number from 0 to n-1, "a random number between 0 and expression-1" (p.316). `RND()` with no argument is a real 0..1 (an extension). |

Hyphenated KMC spellings are accepted: `TIME-ON`, `TIME-OFF`, `SIN-1`, `COS-1`, `TAN-1`, `DEW-POINT`, `DEW-POINT-SI`, `ENTHALPY-SI`, `LN-1`, `SENSOR-ON`, `SENSOR-OFF`, `ON-ERROR`, `NEG-INFINITY`, `POS-INFINITY`.

Extensions that are not in the KMC manuals (kept because existing programs use them): `ROUND`, `FIX`, `SGN`, `FRAC`, `LIMIT` / `CLAMP`, `SCALE`, `TABLE`, `PID`, `DIM`, `SCANCOUNT`, `RND()` with no argument, and the Niagara status properties `.NULL`, `.DOWN`, `.STALE`, `.STATUS`.

## KMC sources

- KMC Connect Installation and Reference Guide, Rev N: https://www.kmccontrols.com/wp-content/uploads/kmc_documents/SOFTWARE_MANUAL_KMC_Connect_InstallationAndReference_RevN.pdf (section 28 About Control Basic programs, section 29 keywords, section 35 object properties)
- KMC BACnet Module for NiagaraAX, Rev E: https://www.kmccontrols.com/wp-content/uploads/kmc_documents/KMC%20BACnet%20Module%20Rev%20E.pdf (Control Basic chapter, operator hierarchy p.35, DOW and DOY p.49)
- TB150604A Versions of Control Basic: https://www.kmccontrols.com/wp-content/uploads/kmc_documents/TB150604A_VersionsOfControlBasic.pdf

## Where the manual does not settle it

- Relational operators: Illustration 28-2 lists `=` `<>` on a row below `<` `>` `<=` `>=`, but p.224 says "All relational operators have the same level of precedence". This interpreter keeps `=` `<>` one level lower; the two readings differ only for an expression like `A = B < C` without parentheses.
- Unary minus is not in the precedence table; `-2 ^ 2` stays `-4`.
- What a controller does on division by zero and similar runtime errors is not documented. The scan stops, the block faults, and the next scan retries.
- `SENSORON` / `SENSOROFF` in KMC detect a shorted or open analog input in hardware (pp.319-321). Niagara has no input voltage, so here they report a rising or falling edge of the variable.
- Gen5 integer typing (`AV1 = 1/2` giving an integer, pp.248-249) is not implemented; every value is a real, as in KMC Standard.
- KMC Gen5 arrays are 0-based with `[ ]`; this module uses `DIM A(n)` with indexes 1…n.
- The `DEC 25` date literal for `DOY` is not supported.
- Copying an input's status to outputs is not described by KMC; it is not done.
- KMC does not describe alarms for program errors; the Niagara fault alarm is this module's choice.

## Not supported

These parse as a clear error, or are ignored, rather than half-implemented:

- Off-panel BACnet paths, `NETPOINT`, `NETPOINTCACHE`, `COV`
- `FUNCTION` / `ENDFUNC` / `FRETURN`, `PROCEDURE` / `ENDPROC` / `PRETURN`, `CALL`
- `STRING` variables (string literals work in `PRINT` only)
- `WAIT` (it would block the scan thread)
- `ALARM` / `DALARM`
- `INTEGER` and `REAL` are accepted as numeric `VAR` declarations. Every value is still a double.
- Controller identity (`PANELADDRESS`, `MODELNUMBER`, `NETSENSORSTATUS`)
- Schedules (`SCHEDON` / `SCHEDOFF`) and `OUTPUTOVERRIDE`
- KMC `LOOP` objects. Use `PID(...)` if you need a loop in the program itself.
- User-defined lookup tables beyond Table 1..4 and inline `TABLE`

`OPEN` / `CLOSE` are accepted as aliases of `START` / `STOP`.
