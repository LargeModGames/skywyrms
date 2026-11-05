#!/usr/bin/env bash
# Usage: bytecode-diff.sh OLD.jar NEW.jar OUT_DIR
# Compares every class of two builds after normalizing javap output. Writes OUT_DIR/summary.md and one .diff per
# changed class. Always exits 0: the report is for reviewers, not a pass or fail check.
set -euo pipefail

old_jar=$(realpath "$1")
new_jar=$(realpath "$2")
out=$(realpath -m "$3")
javap="${JAVA_HOME:+$JAVA_HOME/bin/}javap"
work=$(mktemp -d)
mkdir -p "$out" "$work/old" "$work/new"
(cd "$work/old" && unzip -q "$old_jar")
(cd "$work/new" && unzip -q "$new_jar")

norm() {
	"$javap" -c -p -constants "$1" | sed -E 's/#[0-9]+(:#[0-9]+)?//g; s/^Compiled from.*//; s/ +\/\//  \/\//'
}

classes=$( (cd "$work/old" && find . -name '*.class'; cd "$work/new" && find . -name '*.class') | sort -u)
same=0
changed=()
added=()
removed=()

for class in $classes; do
	name=${class#./}
	name=${name%.class}
	if [ ! -f "$work/old/$class" ]; then
		added+=("$name")
	elif [ ! -f "$work/new/$class" ]; then
		removed+=("$name")
	elif diff <(norm "$work/old/$class") <(norm "$work/new/$class") > "$out/${name//\//.}.diff"; then
		rm "$out/${name//\//.}.diff"
		same=$((same + 1))
	else
		changed+=("$name ($(grep -c '^[<>]' "$out/${name//\//.}.diff") lines)")
	fi
done

{
	echo "## Bytecode compared with the base branch"
	echo
	echo "Identical classes: $same. Changed: ${#changed[@]}. Added: ${#added[@]}. Removed: ${#removed[@]}."
	echo
	echo "Changed classes can still behave the same (branch layout, local slots). The per-class diffs are in the"
	echo "\`bytecode-report\` artifact."
	for label in changed added removed; do
		declare -n list=$label
		if [ ${#list[@]} -gt 0 ]; then
			echo
			echo "**${label^}:**"
			printf -- '- `%s`\n' "${list[@]}"
		fi
	done
} > "$out/summary.md"

cat "$out/summary.md"
rm -rf "$work"
