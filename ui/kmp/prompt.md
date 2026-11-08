/plan

ON COMPACT SCREEN WIDTHS:
The tiles do not show any date information anymore. Let's update them as follows:

* Integrate a space for the company logo
* Tiles will be expandable
* Details will be launched by dedicated trigger within expanded form

COLLAPSED STATE:
1. The logo will at the "START" position, in a simple 90x90 square
2. The START of the Job Title text will align with the TOP-END of the logo (titleMedium/titleLarge)
3. The START of the Company name text will align with the BOTTOM-END of the logo (bodyMedium/bodyLarge)
4. The Chevron indicating expanded/collapsed state will be 90x90 space at the "END" position of the tile
   1. A chevron pointing RIGHT should be used for when collapsed
   2. A rotation to the downward position should align with the expansion of the tile
   3. A chevron pointing DOWN should be used for when expanded
5. The END of the "Created on YYYY-MM-DD" (always YYYY-MM-DD) text will be aligned with the TOP-START of the Chevron
6. The END of the `StatusTag` ("Applied", "Drafting", etc...) widget will be aligned with the BOTTOM-START of the Chevron

EXPANDED STATE:
1. The top of the card stays the same as collapsed, but Chevron is pointing downward
2. The body of the expanded state will be a 3x3 grid
   1. ROW1,COL1 - "Fit Score"
   2. ROW1,COL2 - "Applied on"
   3. ROW1,COL3 - "Updated"
   4. ROW2,COL1 - The numerical FIT score
      1. DO NOT USE THE `FitTag` widget, use `Text`
   5. ROW2,COL2 - The actual date the application was submitted (always YYYY-MM-DD)
   6. ROW2,COL3 - The most recent date the record changed (using the language conversion pattern already defined)
   7. ROW3 - Primary Button to launch `features/job-application-details`

ON WIDER SCREEN WIDTHS:
* As width permits, the collapsing state can be removed and the hidden data can surface on the card.  Once collapsing is removed, tapping the card should launch `features/job-application-details`.
* Data should appear in the following order
  * The START of the "Applied on YYYY-MM-DD" will align with the END of the "Created on" text
  * The START of the `FitTag` will align with the END of the `StatusTag`
  * The START of the "Updated ...." will align with the END of the "Applied on YYYY-MM-DD" text
* Space the content out evenly as space permits