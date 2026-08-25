HOW TO ADD EXERCISE PHOTOS
==========================

Drop a numbered sequence of photos for an exercise into a folder here named after that
exercise. The app auto-detects the folder by name - no code changes needed.

Folder name = the exercise name, lowercased, with any run of non-letter/non-digit
characters (spaces, apostrophes, slashes, etc.) collapsed into a single underscore.

Examples:
  "Barbell Bench Press"              -> exercise_photos/barbell_bench_press/
  "Push Ups"                         -> exercise_photos/push_ups/
  "Farmer's Carry"                   -> exercise_photos/farmer_s_carry/
  "Kettlebell/Barbell Clean and Press"-> exercise_photos/kettlebell_barbell_clean_and_press/

Inside that folder, name the files in the order they should play: 1.jpg, 2.jpg, 3.jpg, ...
(.jpg, .jpeg, .png, and .webp all work). 3-5 photos showing the key positions of the
movement (e.g. start / midpoint / end) is enough - the app cycles through them
automatically like a slideshow while that exercise's page is open.

Keep each photo small - resize to around 480-600px on the long edge and compress as a
JPEG. A handful of small photos per exercise (a few hundred KB total) is far lighter than
bundling a 10-second video for every exercise, which would add tens to hundreds of MB to
the app depending on resolution.

If no matching folder exists for an exercise, the app falls back to an animated
placeholder icon automatically - you don't have to fill in every exercise before building.
