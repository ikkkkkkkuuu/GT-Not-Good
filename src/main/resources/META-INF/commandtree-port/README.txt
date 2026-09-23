Brigadier Tree GTNH source: https://github.com/EmoUsedHM01/brigadier-tree-gtnh
Pinned commit: fb8e6dc7c4c04b10139e75bc260974f38d8c4e95
License: MIT, see BRIGADIER_TREE_GTNH_LICENSE.txt.

Includes relocated Brigadier source originally by Mojang/Microsoft (MIT; see
BRIGADIER_LICENSE.txt) and relocated commodore-file parser source originally
by lucko and contributors (MIT; see COMMODORE_FILE_LICENSE.txt).

Imported source destinations:
  com/xyp/gtnotgood/commandtree
  com/xyp/gtnotgood/mixins/early/commandtree
  assets/gtnotgood/commandtree/commands

Local modifications: relocated packages and resource paths; integrated the
project network channel and mod lifecycle; adapted Mixins to GTNH mappings;
restricted custom text rendering to chat; filtered commands by permission;
matched vanilla tab-completion responses to requests; added packet size checks.
The upstream standalone mod entry point, tinylog, service loader, and separate
network registration were removed.
