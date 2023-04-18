#!/bin/sh
# rename and fit archive files to boringssl's naming style
find . -name "*" -type d | rename 's/ /_/g'

dir_path="${PWD}/*"
dirs=`find $dir_path -maxdepth 0 -type d`
[ ! -d "out" ] && mkdir -p "out"

for dir in $dirs;
do
  echo $dir
  cd "$dir"
  WK="${PWD##*/}";
  WK=${WK//[[:space:]]/_};
  echo $WK
  cp testvector-request.json $WK;
  bzip2 $WK;
  cp $WK.bz2 ../out/
done