# demoasync

Demo for devcon2013.
demoasync is a clojurescript game built to demo CSP in node.js and the browse.
The game is built of sequential processes that communicate using channels.
The main process listens to a websocket channel with twitter searchs and splits tweets into words.
For each word a new process is created that renders a block into the browser that falls at a random speed.
Each word listens to the keyboard events and will dissapear if the whole word is written in the keyboard.
If the word reachs the floor it will also dissapear.
When a word dissapears it send a scoring to another channel, positive if the word is completed, negative if it reaches the floor.


## Usage

To run use leiningen and node.
To build the game

  leiningen cljsbuild once

To run it
  node server/main.js

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
