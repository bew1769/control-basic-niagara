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

Status flags are not used. An input that is null, fault, down, stale, or overridden is read as its value, so check the source point if that matters. Output and variable slots are written with their value only and keep an ok status.

## Execution model

The block runs the program from the first statement every scan. Variables, timers, `IF+` / `IF-` memory, and PID state persist until Reset or a successful recompile. `END` ends that scan. `HALT` stops later scans until Reset. `GOTO` loops are cut off by **Max Instructions Per Scan** (default 10000). The scan runs on a daemon thread. The clock callback only queues it, and a scan still in progress is skipped rather than queued up.

`FIRST` is 1 on the first scan after start, reset, or recompile. `SCANS` counts scans. `SCAN` is the configured interval in seconds.

## Operators

Highest to lowest, matching the KMC precedence table. `^` binds tighter than a leading minus, so `-2^2` is `-4`. The exponent may carry its own sign, so `2 ^ -1` is 0.5, and `^` is right-associative (`2 ^ 3 ^ 2` is 512). `NOT` binds tighter than `^`, so `NOT A = 0` is `(NOT A) = 0`. Use parentheses when you mean `NOT (A = 0)`.

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

## Errors

A compile error (syntax, `GOTO` to a missing line, `NEXT` without `FOR`, an unsupported statement, or `NAME(x)` where `NAME` is neither a function nor an array that is `DIM`med or assigned somewhere) sets **Last Error** with the line and column. The previous program and its slots stay in place, and the previous program keeps running.

A runtime error stops that scan. **Last Error** shows the line and message, and outputs are not written for that scan. The next scan runs again from the top. Runtime errors are division by zero (`/`, `\`, `MOD`), a negative `SQR`, `LN` or `LOG` of 0 or less, `ARCSIN` or `ARCCOS` outside -1..1, any other result that is NaN or infinite (for example `0 ^ -1` or `(-8) ^ 0.5`), an array index out of range, `RETURN` without `GOSUB`, and going over **Max Instructions Per Scan**. `ONERROR GOTO line` jumps to a handler instead.

## Statements that run

`LET` (optional), assignment, `A(i) =`, `DIM A(n)` (indexes are 1…n), `IF`, `IF+`, `IF-`, `THEN`, `ELSE`, `ELSEIF`, `ENDIF` / `END IF`, `GOTO`, `GOSUB`, `RETURN`, `FOR` / `TO` / `STEP` / `NEXT`, `ON n GOTO`, `ON n GOSUB`, `ONERROR GOTO`, `ONERROR OFF`, `START`, `STOP`, `ENABLE`, `DISABLE`, `OPEN`, `CLOSE`, `PRINT`, `END`, `HALT`, `REM`. `INPUT` / `OUTPUT` / `VAR` declarations are accepted and do not create wire-sheet slots.

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
| `LIMIT` / `CLAMP` | `LIMIT(value, low, high)`. |
| `SCALE` | `SCALE(x, inLo, inHi, outLo, outHi)`. Does not clamp; wrap with `LIMIT`. |
| `TABLE` | `TABLE(x, x1, y1, x2, y2, …)` linear interpolation, flat outside the ends. |
| `TBL` | `TBL(x, n)` uses the block's Table 1..4 text: `0,0; 10,100; 20,50`. |
| `TIME` `DOW` `DOM` `DOY` `MONTH` `YEAR` | `DOW` is Sunday 1 through Saturday 7. |
| `TIMEON` `TIMEOFF` | Seconds the variable has been continuously non-zero, or zero. Argument is a variable name. Wall clock, not scan count. |
| `SENSORON` `SENSOROFF` | 1 on the scan the variable rises or falls. Same idea as KMC `SENSOR-ON` / `IF+`. |
| `INTERVAL` | `INTERVAL(seconds)` is 1 on the first call and again after that many seconds. Each call site has its own timer. |
| `PID` | `PID(pv, sp, kp, ki, kd)` or with `outMin, outMax`. Output is not a percent unless you clamp it. Each call site keeps its own integral. Anti-windup drops the last integral step when the output saturates. The derivative term is 0 on the first call after start or reset, so there is no derivative kick. |
| `DEWPOINT` `DEWPOINTF` | Magnus formula, not a KMC firmware match. `DEWPOINT(tempC, rhPercent)` returns °C. `DEWPOINTF` takes and returns °F. |
| `ENTHALPYSI` `ENTHALPY` | ASHRAE-style moist-air enthalpy at 101.325 kPa. SI is kJ/kg. `ENTHALPY` multiplies by 0.4299 to approximate Btu/lb. |
| `RND` | `RND()` is 0..1. `RND(n)` is 0..n. |

Hyphenated KMC spellings are accepted: `TIME-ON`, `TIME-OFF`, `SIN-1`, `COS-1`, `TAN-1`, `DEW-POINT`, `LN-1`, `SENSOR-ON`, `SENSOR-OFF`, `ON-ERROR`.

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
