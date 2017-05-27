IDEA Run Popup Plugin
=====================

A popup menu for IDEA to provide easier access torun configurations.

The popup menu shows a list of run configurations in two sections: Favorites
(first) and others. Each section can be separately configured to show the
configurations last used first or using the ordering of the run configurations
settings dialog.

For each entry, the popup remembers the last executor used (Run, Debug, etc.)

The popup menu is searchable and also handles searching for the first letters
of words (e.g. search for «riwp» to find «Run IDEA with plugin»).

It can be both placed into the toolbar or bound to a key. It can replace the
default run dropdown.


Recommended Configuration
-------------------------

Well, that's at least how I configured it.

### Toolbar

I'm using the *Navigation Toolbar* only, the main Toolbar is disabled in my
setup. I completely removed the default run actions and added the *Stop*
button and the *Run Popup* button.

Note that the default *Stop* action actually shows a popup menu of running
processes to let you choose which process to stop, if several processes are
running. This works for now, but I'll probably add something to the plugin
later.

### Keyboard

Well, that's obvious. ;) Just add a keyboard binding for the *Run Popup*
action. `Alt-X` works great for me.
