#!/bin/bash

#
# Example: ./play.sh 20 "Match" games/tictactoe.kif 120 30 1 plays 20 matches of tictactoe with clock 120/30 and role 1 => xplayer.
#

CMD="java -jar gamecontroller-cli-r360.jar"
ITER=$1
MATCH_ID=$2
GAMEFILE=$3
STARTCLOCK=$4
PLAYCLOCK=$5
ROLEINDEX=$6
NOW=$(date +"%Y%m%d_%T")

touch "$NOW-${GAMEFILE:6}-$ROLEINDEX.txt"

ct=0;
while [ $ct -ne $ITER ]
do
	$CMD $MATCH_ID$ITER $GAMEFILE $STARTCLOCK $PLAYCLOCK -remote $ROLEINDEX MyPlayer localhost 4001 >> "$NOW-${GAMEFILE:6}-$ROLEINDEX.txt"
	ct=$(( $ct + 1 ))
done
