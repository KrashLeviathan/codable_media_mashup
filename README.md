# Codable Media Mashup

> A language. A website. An online video editor for the power user.

Codable Media Mashup (CoMM) is both a website and a language used for editing
online videos through a web server. For those of us who don't have the
time or bandwidth to download a bunch of online videos and splice them
into a video mashup, highlights reel, or compilation, CoMM simplifies things.


## Language Syntax

### `CoMM <videoName>;`
Sets the name of the output video and typically defines which cache space the
program will use when re-using video URLs or variable names. Acceptable name
characters are `[a-zA-Z0-9_]`. If the `config.cache()` option is used (see
below), then the video name given here does NOT define the cache space. Multiple
CoMM's can be defined in the same file: The definitions for each one are located
underneath it.

**Example:**
```
CoMM Game_Highlights_Reel_20160907;
...

CoMM Game_Highlights_Reel_20160914;
...
```

### `// Comments`
Everything after a double forward slash until the end of the line is a comment
and will be disregarded at runtime.

**Example:**
```
// This line is a comment.
CoMM MyCoolVideo;  // This is also a comment, from the slashes to the line's end.
```

### `var <varName> = <url | string | int | bool ...>;`
Defines a variable to be used elsewhere in the program. The `int` and `bool`
variable types don't require enclosing quotation marks, but the `url` and
`string` types do. Acceptable variable name characters are `[a-zA-Z0-9_]`.

**Example:**
```
var bananaPhone = "https://youtu.be/j5C6X9vOEkU";
var startTime   = "1:05";
var duration    = 12;
var upscale     = false;
```

### `requestVideoCredentials(url video1, url video2, ...);`
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

### `add(url video, [string startTime, string stopTime]);`
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

### `addWithCredentials(url video, [string startTime, string stopTime]);`
Same as the `add()` function, but for private videos that require credentials.
The user name and password will be requested at runtime so nothing gets
written in plain text. They are not cached or stored on the server, so if this
function is added, you will have to input a username and password 

### `config.<option>;`
Sets a configuration option for the CoMM video as a whole. Options include
the following:

- `scaleByWidth(int width)` - Scales the video to the desired width, keeping the
  height proportionate to the tallest video added. For example, if we have a
  320x240 video and a 500x500 video and we call `config.scaleByWidth(448);`,
  the CoMM's scale will be set to 448x448. The smaller video will have black
  bars added to the top and bottom, and the larger video will be scaled to 448x448.
- `scaleByHeight(int height)` - Scales the video to the desired height, keeping
  the width proportionate to the widest video added.
- `scale(int width, int height, bool keepProportions)` - Sets the size of the
  video, with the option to keep or ignore the original proportions. 
- `preventUpscaling(bool constrainScale)` - Prevents videos from being scaled
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

- `noCache()` - Tells the program NOT to used cached video from previous runs.
  Typically the program will cache videos for a given CoMM for 24 hours. If the
  program is run again after 24 hours, it will need to download the video URLs
  again. By adding `noCache()`, it will slow down same-day re-runs, but if you
  need to pull a newly-updated version of a video, use this option.
- `cache(uniqueCacheName)` - Lets you define a unique cache name that can be
  used across multiple videos.
  
**Example:**
```
// All three of these videos use the same cache space, so videos reused by the
// second two CoMM's that were used in the first one don't have to re-download.

CoMM Highlights_Reel_1;
config.cache("game footage from monday night");
...

CoMM Highlights_Reel_2;
config.cache("game footage from monday night");
...

CoMM Highlights_Reel_3;
config.cache("game footage from monday night");
...
```

## Examples

Here is a basic example of a CoMM file with one CoMM definition using two
YouTube videos.

```
TODO
```

## Restrictions and Limitations

TODO

## Authors

- Nathan Karasch
- Gregory Steenhagen
