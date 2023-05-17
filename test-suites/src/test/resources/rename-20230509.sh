#!/bin/sh
# rename and fit archive files to boringssl's naming style
#find ./work -name "*" -type f | rename 's/ /_/g'

dir_path="${PWD}/work"
pwd_path="${PWD}"
echo $dir_path
e_files=`find $dir_path -name "*expected.json" -type f`
r_files=`find $dir_path -name "*request.json" -type f`

rm -rf expected
rm -rf vectors

[ ! -d "expected" ] && mkdir -p "expected"
[ ! -d "vectors" ] && mkdir -p "vectors"

cd $dir_path || exit
for f in $e_files;
do
  FF="${f##*/}";
  echo $FF
  cp $f $pwd_path/expected/$FF
done
cd $pwd_path/expected/ || exit
rename s/\-expected.json//g *
files=`find $PWD -name "*" -type f`
for f in $files;
do
  echo ">" $f
  bzip2 $f
done

cd $dir_path || exit
for f in $r_files;
do
  FF="${f##*/}";
  echo $FF
  cp $f $pwd_path/vectors/$FF
done
cd $pwd_path/vectors/ || exit
rename s/\-request.json//g *
files=`find $PWD -name "*" -type f`
for f in $files;
do
  echo ">" $f
  bzip2 $f
done

