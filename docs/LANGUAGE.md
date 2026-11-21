# Control Basic in this module

This interpreter follows KMC Controls' Control Basic closely enough to paste the kind of program that runs a small air handler, and it accepts both dialects KMC shipped:

- **Standard Control Basic** uses a line number on each line (`10`, `20`, …). `GOTO` and `GOSUB` target those numbers.
- **Next Generation / Generation 5** drops required line numbers and uses labels (`AHU_CODE:`) plus block `IF` / `ELSE` / `ENDIF`.

Keywords are not case-sensitive. A line may hold several statements separated by `:`. Comments are `REM`, `'` and `//`.

Diagnostics report the **source line in the editor** (the first line of the file is line 1), which is what the Workbench view highlights.

## Slot rule

Saving a program that compiles rebuilds the block's dynamic slots. A name that is still present keeps its slot object, so links on that slot stay. A name that disappeared is removed. A boolean/numeric type change removes and recreates the slot; that link has to be drawn again. Slots this block created carry the facet `cbSlot=true`. Other dynamic slots on the component are left alone.

A failed compile does **not** change slots.

Names are case-insensitive. The slot keeps the first spelling in the source (`RoomTemp` and `roomtemp` are one slot).

Resolution order:

1. Built-ins never become slots: `TIME`, `DOW`, `DOM`, `DOY`, `MONTH`, `YEAR`, `SCAN`, `SCANS`, `FIRST`, `HOURS`, `MINUTES`, `SECONDS`, `PI`, `TRUE`, `FALSE`, and the function names.
2. `FOR` loop indexes are not slots unless you declare them or give them a prefix below.
3. A declaration wins:
   - `INPUT a, b` numeric input
   - `OUTPUT a, b` numeric output
   - `BINPUT` / `BOOLINPUT` boolean input
   - `BOUTPUT` / `BOOLOUTPUT` boolean output
   - `VAR` numeric variable (read and written)
   - `BVAR` / `BOOL` boolean variable
4. Otherwise a prefix on the name wins. The prefix is the whole name, or the prefix plus digits, or the prefix plus `_` and more (`AI1`, `IN_ROOM`):

   | Prefix | Slot |
   | --- | --- |
   | `AI`, `IN` | numeric input |
   | `AO`, `OUT` | numeric output |
   | `AV`, `VAR` | numeric variable |
   | `BI`, `BIN`, `BINPUT` | boolean input |
   | `BO`, `BOUT`, `BOUTPUT` | boolean output |
   | `BV`, `BVAR` | boolean variable |

5. Otherwise usage decides. Read and never assigned: numeric input. Assigned and never read: numeric output. Both: numeric variable. `START` / `STOP` / `ENABLE` / `DISABLE` / `OPEN` / `CLOSE` count as boolean writes. If those are the only writes and the name is never used in arithmetic, the slot is boolean.

Each scan copies every slot into the program, runs from the top, then writes **outputs and variables** back. Inputs are not written back, so a link keeps driving them. Assigning to a name declared `INPUT` (or prefixed `AI`/`IN`) does not stick past the next scan.

Arrays (`DIM`) are internal. They are not slots.

These names are reserved because the block already has them as properties: `program`, `scanInterval`, `maxInstructions`, `enabled`, `lastError`, `console`, `scanCount`, `slotSummary`, `table1`…`table4`, `status`, `displayName`.

## Execution model

The block runs the program from the first statement every scan. Variables, timers, `IF+` / `IF-` memory, and PID state persist until Reset or a successful recompile. `END` ends that scan. `HALT` stops later scans until Reset. `GOTO` loops are cut off by **Max Instructions Per Scan** (default 10000). The scan runs on a daemon thread. The clock callback only queues it, and a scan still in progress is skipped rather than queued up.

`FIRST` is 1 on the first scan after start, reset, or recompile. `SCANS` counts scans. `SCAN` is the configured interval in seconds.

## Operators

Highest to lowest, matching the KMC precedence table. Unary minus binds tighter than `^`, so `-2^2` is `-4`. `NOT` binds tighter than `^`, so `NOT A = 0` is `(NOT A) = 0`. Use parentheses when you mean `NOT (A = 0)`.

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
| `AND`, `OR`, `XOR` | logic |

`00:07:00` and `8:30` are times in seconds (`HH:MM:SS` or `HH:MM`). `TIME` is seconds after midnight, so `TIME > 00:07:00` matches the KMC sample. `HOURS` is 3600, `MINUTES` is 60, `SECONDS` is 1.

## Statements that run

`LET` (optional), assignment, `A(i) =`, `DIM A(n)` (indexes are 1…n), `IF`, `IF+`, `IF-`, `THEN`, `ELSE`, `ELSEIF`, `ENDIF` / `END IF`, `GOTO`, `GOSUB`, `RETURN`, `FOR` / `TO` / `STEP` / `NEXT`, `ON n GOTO`, `ON n GOSUB`, `ONERROR GOTO`, `ONERROR OFF`, `START`, `STOP`, `ENABLE`, `DISABLE`, `OPEN`, `CLOSE`, `PRINT`, `END`, `HALT`, `REM`, and the slot declarations above.

`IF+` runs the THEN branch only on the scan the condition becomes true. `IF-` runs it only on the scan it becomes false. The first scan treats the previous value as false.

`ON n` is 1-based. An index outside the list falls through.

`THEN 100` is a goto, in the Standard dialect.

## Functions

| Function | Notes |
| --- | --- |
| `ABS` `SGN` `INT` `FIX` `FRAC` `ROUND` | `INT` is floor, so `INT(-1.2)` is -2. `FIX` truncates toward zero. `ROUND(x)` or `ROUND(x, digits)`. |
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
| `PID` | `PID(pv, sp, kp, ki, kd)` or with `outMin, outMax`. Output is not a percent unless you clamp it. Each call site keeps its own integral. Anti-windup drops the last integral step when the output saturates. |
| `DEWPOINT` `DEWPOINTF` | Magnus approximation. `DEWPOINT(tempC, rhPercent)` returns °C. `DEWPOINTF` takes and returns °F. |
| `ENTHALPYSI` `ENTHALPY` | Moist-air enthalpy at 101.325 kPa. SI is kJ/kg. `ENTHALPY` multiplies by 0.4299 to approximate Btu/lb. Not a certified psychrometric library. |
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
