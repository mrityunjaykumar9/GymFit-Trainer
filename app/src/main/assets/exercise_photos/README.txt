HOW TO ADD EXERCISE PHOTOS
==========================

Simplest option: one photo per exercise, named exactly like the exercise, right in this
folder. No code changes needed - the app auto-detects it by name.

File name = the exercise name, lowercased, with any run of non-letter/non-digit
characters (spaces, apostrophes, slashes, etc.) collapsed into a single underscore.
Parenthesized suffixes and apostrophes are dropped first, so they don't need a filename
of their own.

Examples:
  "Barbell Bench Press"               -> exercise_photos/barbell_bench_press.jpg
  "Push Ups"                          -> exercise_photos/push_ups.jpg
  "Farmer's Carry"                    -> exercise_photos/farmers_carry.jpg
  "Pec Deck (Machine Fly)"            -> exercise_photos/pec_deck.jpg
  "Kettlebell/Barbell Clean and Press"-> exercise_photos/kettlebell_barbell_clean_and_press.jpg

.jpg, .jpeg, .png, and .webp all work.

If you'd rather show a few photos per exercise as a cycling slideshow instead of one still,
put a folder named the same way (no extension) containing 1.jpg, 2.jpg, 3.jpg, ... in
that order - the app plays those in a loop instead of a single flat file when present.

Keep photos small - resize to around 480-600px on the long edge and compress as JPEG. This
keeps the whole library's worth of photos to a few MB total, versus the tens to hundreds of
MB a 10-second video per exercise would add.

If no matching file or folder exists for an exercise, the app falls back to an animated
placeholder icon automatically - you don't have to fill in every exercise before building.
