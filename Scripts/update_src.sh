rm -rf $PLATFORM/generators/boom/src/main
rm -rf $PLATFORM/generators/rocket-chip/src/scala
rm -rf $PLATFORM/generators/chipyard/src/main

cp $FIREGUARD/HARDWARE/big $PLATFORM/generators/boom/src/main
cp $FIREGUARD/HARDWARE/little $PLATFORM/generators/rocket-chip/src/scala
cp $FIREGUARD/HARDWARE/top $PLATFORM/generators/rocket-chip/src/scala