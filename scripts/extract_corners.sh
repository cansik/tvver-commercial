#!/bin/sh
width=80
height=50
ffmpeg -i $1 -filter_complex "[0:0]crop=$width:$height:0:0,pad=$width*2:2*$height:0:0[tl]; [0:0]crop=$width:$height:iw-$width:0[tr]; [0:0]crop=$width:$height:0:ih-$height,pad=$width*2:$height:0:0[bl]; [0:0]crop=$width:$height:iw-$width:ih-$height[br]; [tl][tr]overlay=W/2[top]; [bl][br]overlay=W/2[bot]; [top][bot]overlay=0:H/2" -codec:a copy $2
