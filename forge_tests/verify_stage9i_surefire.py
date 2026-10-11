#!/usr/bin/env python3
"""Stage9I gate: require three actual Forge land action tests, none skipped."""
import json, sys
from pathlib import Path
from xml.etree import ElementTree as ET

METHODS = {
 "bothIndependentActorsPlayRealForgeLandMoves",
 "secondLandSameTurnIsRejectedByActualForgeRule",
 "privateForgeZonesAndForgedCommandsFailClosed",
}
seen={}
folder=Path(sys.argv[1]) if len(sys.argv)==2 else Path("nonexistent")
if not folder.is_dir():
    raise SystemExit("missing Forge Surefire directory")
for path in sorted(folder.glob("TEST-*.xml")):
    root=ET.parse(path).getroot()
    for e in root.iter():
        if e.tag.rsplit("}",1)[-1]!="testcase" or e.get("classname") not in (
            "forge.ai.IsolatedForgeLandActionTest","IsolatedForgeLandActionTest"):
            continue
        name=e.get("name")
        if not name or name in seen:
            raise SystemExit("duplicate/anonymous test")
        seen[name]=[c.tag.rsplit("}",1)[-1] for c in e if c.tag.rsplit("}",1)[-1] in {"failure","error","skipped"}]
if set(seen)!=METHODS or any(seen.values()):
    raise SystemExit(f"wrong methods or failures: {seen}")
print(json.dumps({"gate":"PASS","executed":3,"real_forge_land_moves":True,"full_magic_match":False},sort_keys=True))
