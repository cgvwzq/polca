#!/usr/bin/env bash

if [[ $# -lt 1 ]]; then
	echo "[!] use: $0 <file> [assoc] [template]"
	exit 1
fi

file=$1
assoc=${2:-4}
template=${3:-simple}

numStates=$(cat $file | grep -E 's[0-9]+ \[shape=' | wc -l)

# input: s0 -> s1 [label="m() / 0"]; # output: missState(s0) == s1
missState() {
	echo -n "missState("
	echo $1 | awk '{print $1") == "$3}'
}

# input: s0 -> s1 [label="m() / 0"];
# output: missIndx(s0) == 0
missIdx () {
	echo -n "missIdx("
	echo -n $1 | awk '{printf "%s", $1}'
	echo $1 | cut -d\" -f 2 | awk '{print ") == " $3}'
}

# input: s0 -> s1 [label="h(0) / _"];
# output: missState(s0, 0) == s1
hitState() {
	num=$(echo $line | cut -d\( -f2 | cut -d\) -f1)
	echo -n "hitState("
	echo $1 | awk '{print $1", '$num') == "$3}'
}

# modify params
echo "pragma options \"--be:nosim --beopt:simplifycex NOSIM --bnd-inbits 2 --bnd-cbits 2 --bnd-angelicbits 2 --bnd-unroll-amnt 1 --bnd-inline-amnt 2 --slv-seed 1337\";";
# eom

echo "int ASSOC = $assoc;";
echo "include \"templates/${template}.sk\";";

echo "harness void main() {"

echo -e "\t// define states"
for i in `seq 0 1 $((numStates-1))`; do
	echo -ne "\tint[ASSOC] s${i} = {"
	echo -n $(for i in `seq 0 1 $((assoc-2))`; do printf '??, '; done; printf '??')
	echo "};";
done

echo
echo -e "\t// miss transitions"
while read line; do
	out=$(missState "$line")
	echo -e "\tassert $out;"
done <<< $(grep 'label="m()' $file)

echo
echo -e "\t// miss index"
while read line; do
	out=$(missIdx "$line")
	echo -e "\tassert $out;"
done <<< $(grep 'label="m()' $file)

echo
echo -e "\t// hit state"
while read line; do
	out=$(hitState "$line")
	echo -e "\tassert $out;"
done <<< $(grep 'label="h(' $file)

echo "}";


