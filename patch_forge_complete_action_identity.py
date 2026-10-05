from __future__ import annotations

import argparse
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match; found {count}")
    return text.replace(old, new, 1)


def patch(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    old = """        @Override
        public String toString() {
            return toString(false);
        }
    }
}
"""
    new = """        /**
         * Stable identity for one executable root action. Stage 6 information-set
         * search must aggregate scores by this identity, never by a per-world
         * optimum. It intentionally includes every decision component already
         * captured by Forge's Plan: ability, announced X, modes, targets and
         * explicit card choices. The initial score is deliberately excluded.
         */
        public String completeActionIdentity() {
            StringBuilder sb = new StringBuilder();
            sb.append("recipe=v2|ability=").append(saRef == null ? "<none>" : saRef.toString(false));
            // The candidate index distinguishes two sources with identical rules text.
            sb.append("|candidate=").append(saRef == null ? "<none>" : saRef.saIndex + "/" + saRef.saCount);
            sb.append("|x=").append(xMana == null ? "<none>" : xMana);
            sb.append("|modes=");
            if (modes == null) {
                sb.append("<none>");
            } else {
                sb.append(java.util.Arrays.toString(modes));
            }
            sb.append("|targets=").append(targets == null ? "<none>" : targets.toString());
            sb.append("|choices=").append(choices == null ? "<none>" : choices.toString());
            return sb.toString();
        }

        @Override
        public String toString() {
            return toString(false);
        }
    }
}
"""
    text = replace_once(text, old, new, "Plan.Decision complete-action identity insertion")
    path.write_text(text, encoding="utf-8")
    print(f"Patched complete-action identity into {path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    patch(args.path)
