# Codable Media Mashup

> A language. A command line tool. A quick and dirty video editor for the power user.

![Logo](resources/logo.svg.png "Codable Media Mashup")

Codable Media Mashup (CoMM) is both a language and a command line tool used for slicing
and splicing online videos together. The purpose is to allow people to quickly and
efficiently make "highlights reel" type of videos using a collection of online videos.

Eventually I would like to make it a server-side tool that gets launched when you
upload a CoMM file on a website. For now, though, it just gets run on the user's
own machine through the command line.

Potential use cases:

- Game footage highlights from the season
- Top ten funniest Seinfeld moments
- Cat fail compilations
- Caruso one-liners
- Compilation of every time Neo says "No", "Why", or "I don't understand". Wake up Neo...


## Dependencies

CoMM is designed to run in a bash terminal on a Unix machine with the following packages installed:

- [Java SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [youtube-dl](https://rg3.github.io/youtube-dl/)
- [ffmpeg](https://ffmpeg.org/)
- [moreutils](https://joeyh.name/code/moreutils/)

**Mac**

On Mac, [homebrew](http://brew.sh/) is a good option for installing these
dependencies. Once brew in installed, use `brew install <package>` to install.

**Linux**

Use your package manager of choice to install. On most Ubuntu/Debian systems,
this looks like `sudo apt-get install <package>`.


## Using the Command Line Tool

1) **Clone it**

```
git clone https://github.com/KrashLeviathan/codable_media_mashup.git
```

2) **Build it**

```
tools/build.sh
```

3) **Run it**

```
java -jar Comm.jar <input_file.comm>
```

If no input file is provided, it will look for input from stdin.


## Language Syntax

### `CoMM <videoName> [cache(uniqueCacheName)] ;`
Sets the name of the output video and defines which cache namespace the
program will use when re-using video URLs or variable names. If the `cache()`
option is used, then the `uniqueCacheName` provided will define the cache.
If the `cache()` option is left out, the video name will define the cache.
The cache option is useful if you want to define multiple CoMM's
in one file using a shared set of videos. By using the same cache, shared
videos won't be re-downloaded. Acceptable name and cache namespace
characters are `[a-zA-Z0-9_]`. 

**Example:**
```
// The first three videos use the same cache, so shared
// videos between them will only download once.

CoMM Highlights_Reel_1 cache(monday_night_game_footage);
...

CoMM Highlights_Reel_2 cache(monday_night_game_footage);
...

CoMM Highlights_Reel_3 cache(monday_night_game_footage);
...

CoMM And_Now_For_Something_Completely_Different;
// Doesn't use the same cache as the other three videos
```


### `// Comments`
Everything after a double forward slash until the end of the line is a comment
and will be disregarded at runtime.

**Example:**
```
// This line is a comment.
CoMM MyCoolVideo;  // This is also a comment, from the slashes to the line's end.
```

### `var <varName> = <varName | string | int | bool ...>;`
Defines a variable to be used elsewhere in the program. String literals
require enclosing double quotation marks, but the rest don't. Acceptable
variable name characters are `[a-zA-Z0-9_]`.

**Example:**
```
var bananaPhone = "https://youtu.be/j5C6X9vOEkU";
var startTime   = "1:05";
var duration    = 12;
var upscale     = false;
```

### `add(string url, [string startTime, string stopTime]);`
Adds a video or video clip to the stream. If no start/stop times are given, the
entire video will be added. Start and stop times can be provided to use only a
certain clip from the video. The start and stop times must be either both present
or both absent. The times are given in `"minute:second"` format.

**Example:**
```
// Add the whole video?
add(graduationVid);

// Or maybe we'll just add my glorious moments on stage...
add(graduationVid, "59:12", "63:07");
```

### `config.<option>;`
Sets a configuration option for the CoMM video as a whole. Options include
the following:

- `config.noCache()` - Tells the program NOT to used cached video from previous runs.
  Typically the program will cache videos for a given CoMM for 24 hours. If the
  program is run again after 24 hours, it will need to download the video URLs
  again. By adding `config.noCache()`, it will slow down same-day re-runs, but if you
  need to pull a newly-updated version of a video, use this option.
  *NOTE: This will disregard any `cache()` option given in the `CoMM` definition.*
- More to come! (see below)


## Examples

Here is a basic example of a CoMM file with several video definitions:

```
CoMM banana_phone_clips cache(BananaPhone);

// This part is just showing the different types of variable assignments
var bananaPhone = "https://youtu.be/j5C6X9vOEkU";
var startTime1 = "1:20";
var stopTime1  = "1:30";
var someInt    = 20;
var someBool   = true;
var sameUrl    = bananaPhone;

// We can add the video with variables or literals
add("https://youtu.be/j5C6X9vOEkU", startTime1, stopTime1);
add(bananaPhone, "2:00", "2:05");



// This will be a second, separate video with a different cache
CoMM duckSong;

// Adding the entire "Duck Song"
add("https://www.youtube.com/watch?v=MtN1YnoL46Q");

// We can still use the variables from earlier definitions, but since
// this video is in a different cache, it will re-download the banana phone
// video.
add(sameUrl, "0:00", "0:05");



// This third video will use the same cache as the first video
CoMM dance_moves cache(BananaPhone);

// King Louie's moves
add("https://www.youtube.com/watch?v=9JDzlhW3XTM", "0:40", "1:28");

// Even though the variables `startTime` and `stopTime` were defined earlier
// in the file, we still have to use the `var` keyword when we assign them
// again.
var startTime = "1:35"; var stopTime = "2:17";
add(bananaPhone, startTime, stopTime);

```


## Upcoming Features

The following features are on their way! Just... not here yet. *NOTE: The language
parser won't recognize these commands as syntax errors, but the functionality hasn't been
implemented yet.*

- `config.scaleByWidth(int width)` - Scales the video to the desired width, keeping the
  height proportionate to the tallest video added. For example, if we have a
  320x240 video and a 500x500 video and we call `config.scaleByWidth(448);`,
  the CoMM's scale will be set to 448x448. The smaller video will have black
  bars added to the top and bottom, and the larger video will be scaled to 448x448.
- `config.scaleByHeight(int height)` - Scales the video to the desired height, keeping
  the width proportionate to the widest video added.
- `config.scale(int width, int height, bool keepProportions)` - Sets the size of the
  video, with the option to keep or ignore the original proportions. 
- `config.preventUpscaling(bool constrainScale)` - Prevents videos from being scaled
  larger than their original size. If `constrainScale` is set to `true`, the
  entire CoMM's scale dimensions will be reduced to the largest permissible
  video scale. Otherwise, it will add a black border around videos that cannot
  be upscaled.

  **Example:**
  ```
  // In each of these examples, there are two videos: One is 640x480 and
  // the other is 320x240
  
  CoMM video1;
  config.scale(448, 336, true);   // Sets the scale of the CoMM to 448x336.
  config.preventUpscaling(true);  // Since the smaller of the two videos is 320x240,
                                  // the CoMM scale is reduced to 320x240 because
                                  // we have set `constrainScale` to true.
  ...
  
  CoMM video2;
  config.scale(448, 336, true);   // Sets the scale of the CoMM to 448x336.
  config.preventUpscaling(false); // Since we're not constraining the scale here,
                                  // the CoMM scale remains at 448x336 and black
                                  // bars are added around the smaller video
                                  // instead of upscaling it.
  ...
  ```

- `requestVideoCredentials(string url1, string url2, ...);` - 
  This allows the program to download and manipulate private videos that require
  credentials. The user name and password will be requested at runtime so nothing
  gets written in plain text in the code. They are not cached or stored on the
  server, so you will have to input a username and password every time the 
  program is run, once for each `requestVideoCredentials()` function. All the 
  videos listed in a single function will receive the same user name and password.

  **Example:** You have three videos, with variable names `funny1`, `funny2`, and
  `funny3`, on your YouTube account that you want to use, but they're private.
  You have another private video from a different website assigned to the variable
  name `yoloFail`. You should make 2 separate calls to this method, like so:

  ```
  requestVideoCredentials(funny1, funny2, funny3);
  requestVideoCredentials(yoloFail);
  ```

  At runtime, the program will ask for a user name and password for the first video
  set, and then it will ask for a user name and password for the second video set.


## Future Development Plans

The original plan was to host this tool on a web server that gets accessed through
the browser. The exerience would look like so:

1. The user uploads a CoMM file or enters a ComM definition in a textarea.
2. The file is sent to the server, which runs the command line application.
3. Once complete, the video file is sent back to the user for download.

The cached files would only stick around for 24 hours before being automagically
cleaned up by the server. Restrictions would need to be in place to prevent abuse,
but overall I think it would be pretty doable. I like the idea of letting a really
fast server do the downloading/processing instead of using my own computer, thus
freeing up my memory for more important things!

Other potential features include:

- Audio integration (e.g. adding background music to a video compilation)
- Overlaid text (a quick video meme machine!)
- Using local video files
- Finer precision on time start/stop (nanoseconds?)
- More output format options (probably prioritizing mp4)

## Contributing

If you like using CoMM (or if you don't like it) and want to make it better,
feel free to contribute! See the
[CONTRIBUTING](https://github.com/KrashLeviathan/codable_media_mashup/blob/master/CONTRIBUTING.md)
file for more information.

Feedback can be sent to [nate@krashdev.com](mailto:nate@krashdev.com).
