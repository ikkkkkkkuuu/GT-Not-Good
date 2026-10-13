# Wireless Frequency Card port

The Wireless Frequency Card implementation is adapted from GT-Not-Leisure PR 627,
commit `651396c75d108e6ff5399a44c56c12214ad27d3d`, at
https://github.com/ABKQPO/GT-Not-Leisure. The original Java code is licensed
under LGPL-3.0. Adapted GTNG Java in `common/items/wirelessfrequency`,
`common/wirelessfrequency`, `common/network/wirelessfrequency` and
`client/wirelessfrequency`, plus associated mixins, retains LGPL-3.0 and is
outside the repository's general MIT grant. The LGPL license text is bundled as
`META-INF/licenses/GT-Not-Leisure-LGPL-3.0.txt`.

The item texture is copied unchanged from that GTNL commit. GTNL identifies
AE2 Lightning Tech Reborn commit `2d8369e1e8b952300ec7ee73837e75cb61c7fc8d`
as the original source of `assets/ae2lt/textures/item/overloaded_frequency_card.png`:
https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn.
The visual asset retains CC BY-NC-SA 3.0; its attribution, noncommercial and
ShareAlike requirements are separate from the Java license. License:
https://creativecommons.org/licenses/by-nc-sa/3.0/legalcode.
The intermediate project's original attribution is bundled unchanged as
`GTNL_ASSET_NOTICE.txt`.

`ASSET_MANIFEST.json` records the source, destination and SHA-256 hash. The
source checkouts stay outside compilation and resource packaging. This port
adapts package names, item registration, localization and networking to GTNH
Minecraft 1.7.10; no upstream endorsement is implied.

The GT 5.09.54.183 adapter records registered hatches after structure checks.
Unlike the upstream formed-machine filter, grouping retains these associations
for incomplete machines; structure revisions invalidate removed associations.
Type folding groups controller entries; each controller opens only its own ME devices.
Machine children retain their ME device identities; single-node children do
not open a redundant entrance-detail panel.
GTNG adds saved source selection and per-source automatic-placement preferences,
with migration from the legacy single binding and server-validated switching.
Saved sources support per-card names, displayed with their original coordinates.
