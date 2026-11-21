# Chart smoothing

The dashboard selector applies a persisted 3-, 7-, or 10-calendar-day binomial
window to demands, tension, energy, room to act, and the optional stress index.
The default is seven days. Daily points remain visible with faint connecting
lines; a thicker line shows the weighted mean and a translucent band shows
plus/minus one weighted population standard deviation (not a confidence interval).

Weights are row `width - 1` of Pascal's triangle, normalized by their sum.
For values 2, 8, 2, the three-day weights 1:2:1 give mean 5 and deviation 3.
A ten-day window is centered at a half-day position. The band is clipped to the
chart's fixed 0–10 range without changing its calculated values.

Only complete windows inside the selected dashboard period are used. Missing
dates, missing metric values, or non-finite values invalidate the affected
windows; zeros are valid. Neither line nor band bridges invalid windows. At
period boundaries the centered curve stops where a full window is unavailable.
An explanatory message appears if no complete window is available. The stress
index uses only days with all four measurements, as before.

## Device checks

1. Change 3 → 7 → 10 days and check all four charts and the enabled stress chart.
2. Restart the app and verify that the selected width persists.
3. Check constant values (flat line, zero-width band) and fluctuating values.
4. Check missing days and missing individual metrics: no interpolation across gaps.
5. Change 4 weeks / 3 months / all and check the visible period boundaries.
6. Check German/English, light/dark themes, and a period without complete windows.

Pure JVM tests cover kernel values, spread, all widths, half-day positions, zeros,
missing/null/non-finite data, unordered input, and stress-index completeness.
